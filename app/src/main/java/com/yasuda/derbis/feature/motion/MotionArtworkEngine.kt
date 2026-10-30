package com.yasuda.derbis.feature.motion

import android.content.Context
import com.yasuda.derbis.data.ChartOfficialArtworkResolver
import com.yasuda.derbis.data.local.DerbisDatabase
import com.yasuda.derbis.domain.DerbisCanvasSource
import com.yasuda.derbis.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.LinkedHashMap
import java.util.Locale

class MotionArtworkEngine(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MotionArtworkRepository(DerbisDatabase.get(appContext).motionArtworkDao())
    private val networkPolicy = MotionArtworkNetworkPolicy(appContext)
    private val urlVerifier = MotionArtworkUrlVerifier(appContext)
    private val metadataResolver = ChartOfficialArtworkResolver(appContext)
    private val providerFactories: Map<String, (MotionArtworkConfig) -> MotionArtworkProvider> = mapOf(
        AppleMusicAnimatedArtworkProvider.PROVIDER_ID to { _: MotionArtworkConfig ->
            AppleMusicAnimatedArtworkProvider(appContext)
        },
        SpotifyCanvasProvider.PROVIDER_ID to { _: MotionArtworkConfig ->
            SpotifyCanvasProvider(appContext)
        },
        DerbisCanvasCatalogProvider.PROVIDER_ID to { _: MotionArtworkConfig ->
            DerbisCanvasCatalogProvider(appContext)
        }
    )
    private val runtimeLock = Any()
    private var activeEpoch = -1L
    private var activeProviders: List<MotionArtworkProvider> = emptyList()
    private val lookupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestCoordinator = MotionArtworkRequestCoordinator(lookupScope)
    private val metadataWarmCache = MotionMetadataWarmCache()
    fun close() {
        lookupScope.cancel()
    }

    suspend fun resolve(
        track: Track,
        source: DerbisCanvasSource = DerbisCanvasSource.Auto
    ): MotionArtwork? = resolveProgressive(track, source).lastOrNull()

    fun resolveProgressive(
        track: Track,
        source: DerbisCanvasSource = DerbisCanvasSource.Auto
    ): Flow<MotionArtwork> = flow {
        if (!networkPolicy.canResolveCurrent()) return@flow
        val runtime = MotionArtworkRuntime.snapshot()
        val lookupTrack = prepareLookupTrackWithinBudget(track)
        if (!networkPolicy.canResolveCurrent()) return@flow
        val identityKey = motionArtworkRequestKey(lookupTrack, source)
        val requestKey = "${runtime.epoch}:$identityKey"
        emitAll(
            sharedProgressive(requestKey) { emit ->
                resolveFreshProgressive(lookupTrack, identityKey, runtime.epoch, runtime.value, source).collect { artwork ->
                    emit(artwork)
                }
            }
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun prepareLookupTrackWithinBudget(track: Track): Track {
        val remembered = metadataWarmCache.get(track) ?: track
        val budgetMs = motionMetadataForegroundBudgetMs(remembered)
        if (budgetMs <= 0L) return remembered
        val prepared = lookupScope.async {
            prepareLookupTrack(remembered).also { resolved ->
                metadataWarmCache.put(track, resolved)
            }
        }
        return withTimeoutOrNull(budgetMs) { prepared.await() } ?: remembered
    }

    private suspend fun prepareLookupTrack(track: Track): Track {
        if (track.isrc.isNotBlank() && !isUnusableMotionAlbum(track.album)) return track
        val country = Locale.getDefault().country
            .trim()
            .uppercase(Locale.ROOT)
            .takeIf { it.length == 2 }
            ?: DEFAULT_MOTION_METADATA_COUNTRY
        return try {
            metadataResolver.enrich(listOf(track), country).firstOrNull() ?: track
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Motion metadata enrichment failed for %s / %s", track.title, track.artist)
            track
        }
    }

    private fun sharedProgressive(
        requestKey: String,
        block: suspend (emit: suspend (MotionArtwork) -> Unit) -> Unit
    ): Flow<MotionArtwork> = requestCoordinator.share(requestKey, block)

    suspend fun prefetchNext(
        track: Track?,
        source: DerbisCanvasSource = DerbisCanvasSource.Auto
    ) {
        if (track == null || !networkPolicy.canPrefetchNext()) return
        resolve(track, source)
    }

    suspend fun invalidate(
        track: Track,
        source: DerbisCanvasSource = DerbisCanvasSource.Auto,
        cachedIdentityKey: String? = null
    ) {
        val runtime = MotionArtworkRuntime.snapshot()
        val prepared = metadataWarmCache.get(track)
        val cacheKeys = linkedSetOf(motionArtworkRequestKey(track, source))
        prepared?.let { cacheKeys += motionArtworkRequestKey(it, source) }
        cachedIdentityKey
            ?.takeIf { it.isNotBlank() }
            ?.let(cacheKeys::add)

        cacheKeys.forEach { cacheKey ->
            requestCoordinator.cancel("${runtime.epoch}:$cacheKey")
            repository.invalidate(cacheKey)
        }

        val identity = MotionTrackIdentity.from(prepared ?: track)
        providersFor(runtime.epoch, runtime.value)
            .filterIsInstance<MotionArtworkRefreshableProvider>()
            .forEach { provider ->
                try {
                    provider.invalidate(identity)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    Timber.d(error, "Motion provider %s refresh invalidation failed", provider.id)
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun resolveFreshProgressive(
        track: Track,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig,
        source: DerbisCanvasSource
    ): Flow<MotionArtwork> = flow {
        when (val cached = repository.get(identityKey, configEpoch)) {
            is MotionArtworkCacheResult.Hit -> {
                emit(cached.artwork)
                return@flow
            }
            MotionArtworkCacheResult.Negative -> return@flow
            MotionArtworkCacheResult.Miss -> Unit
        }
        val identity = MotionTrackIdentity.from(track)
        val providerOrder = motionArtworkProviderOrder(config.providerOrder, source)
        val providers = providersFor(configEpoch, config).filter { it.id in providerOrder }
        val providerRanks = providerOrder.withIndex().associate { it.value to it.index }
        val forcedSource = source != DerbisCanvasSource.Auto

        var providerFailed = providers.isEmpty()
        var verifierFailed = false
        var verificationExhaustive = true
        var publishedArtwork: MotionArtwork? = null
        var publishedCandidate: MotionArtworkRankedCandidate? = null
        var upgradesUsed = 0

        supervisorScope {
            val lookupOutcomes = Channel<IndexedMotionProviderOutcome>(Channel.UNLIMITED)
            val lookups = providers.mapIndexed { index, provider ->
                launch {
                    lookupOutcomes.send(
                        IndexedMotionProviderOutcome(index, runProviderLookup(provider, identity, config))
                    )
                }
            }
            launch {
                lookups.joinAll()
                lookupOutcomes.close()
            }
            val pendingProviderIndexes = providers.indices.toMutableSet()
            fun pendingProviderRanks(): List<Int> = pendingProviderIndexes.map { index ->
                providerRanks[providers[index].id] ?: Int.MAX_VALUE
            }
            var heldCandidate: MotionArtworkRankedCandidate? = null
            var holdDeadlineNanos = 0L

            suspend fun publish(accepted: MotionArtworkRankedCandidate): Boolean {
                if (!shouldPublishMotionArtwork(networkPolicy.canResolveCurrent())) {
                    cancelMotionArtworkLookups(lookups)
                    return false
                }
                if (publishedCandidate != null) upgradesUsed++
                publishedCandidate = accepted
                val artwork = motionArtworkFrom(accepted, identityKey, configEpoch, config)
                publishedArtwork = artwork
                repository.save(artwork)
                emit(artwork)
                return true
            }

            while (true) {
                val held = heldCandidate
                val received = if (held == null) {
                    lookupOutcomes.receiveCatching()
                } else {
                    val remainingMs = (holdDeadlineNanos - System.nanoTime()) / NANOS_PER_MILLI
                    if (remainingMs <= 0L) {
                        null
                    } else {
                        select {
                            lookupOutcomes.onReceiveCatching { it }
                            onTimeout(remainingMs) { null }
                        }
                    }
                }
                if (received == null) {
                    heldCandidate = null
                    Timber.d(
                        "motion priority settle elapsed; publishing provider=%s title=%s",
                        held?.candidate?.provider,
                        identity.title
                    )
                    if (held != null && !publish(held)) return@supervisorScope
                    continue
                }
                val firstLookup = received.getOrNull() ?: break
                val pendingOutcomes = mutableListOf(firstLookup)
                while (true) {
                    val next = lookupOutcomes.tryReceive().getOrNull() ?: break
                    pendingOutcomes.add(next)
                }
                pendingOutcomes.sortBy { providerRanks[providers[it.index].id] ?: Int.MAX_VALUE }
                while (pendingOutcomes.isNotEmpty()) {
                    val currentLookup = pendingOutcomes.removeAt(0)
                    pendingProviderIndexes.remove(currentLookup.index)
                    val provider = providers[currentLookup.index]
                    val outcome = currentLookup.outcome
                    val summary = when (outcome) {
                        is MotionArtworkProviderResult.Found -> "found=${outcome.candidates.size}"
                        MotionArtworkProviderResult.NoMatch -> "noMatch"
                        is MotionArtworkProviderResult.Failed -> "failed"
                    }
                    Timber.d(
                        "motion provider %s -> %s for %s / %s (album=%s)",
                        provider.id,
                        summary,
                        identity.title,
                        identity.artists.joinToString(),
                        identity.album
                    )
                    if (outcome is MotionArtworkProviderResult.Failed) providerFailed = true
                    if (shouldContinueMotionArtworkFallback(outcome)) continue
                    val candidates = (outcome as MotionArtworkProviderResult.Found).candidates
                    val providerRank = providerRanks[provider.id] ?: Int.MAX_VALUE
                    if (
                        !shouldPublishMotionUpgrade(
                            publishedProviderRank = publishedCandidate?.providerRank ?: heldCandidate?.providerRank,
                            candidateProviderRank = providerRank,
                            forcedSource = forcedSource,
                            upgradesUsed = upgradesUsed
                        )
                    ) {
                        verificationExhaustive = false
                        continue
                    }
                    val ranked = rankMotionCandidates(identity, candidates, config, providerRanks)
                    val verificationPlan = buildMotionArtworkVerificationPlan(ranked)
                    if (!verificationPlan.exhaustive) verificationExhaustive = false
                    var selected: MotionArtworkRankedCandidate? = null
                    for ((index, rankedCandidate) in verificationPlan.candidates.withIndex()) {
                        val candidate = rankedCandidate.candidate
                        when (val result = urlVerifier.verify(candidate)) {
                            MotionArtworkVerificationResult.Verified -> {
                                Timber.d(
                                    "motion verifier VERIFIED provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                                selected = rankedCandidate
                            }
                            MotionArtworkVerificationResult.Invalid -> {
                                Timber.d(
                                    "motion verifier INVALID provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                            }
                            is MotionArtworkVerificationResult.Failed -> {
                                verifierFailed = true
                                Timber.d(
                                    result.cause,
                                    "motion verifier FAILED provider=%s scope=%s title=%s",
                                    candidate.provider,
                                    candidate.scope,
                                    identity.title
                                )
                            }
                        }
                        if (selected != null) {
                            if (index != verificationPlan.candidates.lastIndex) verificationExhaustive = false
                            break
                        }
                    }
                    val accepted = selected ?: continue
                    if (
                        shouldHoldMotionPublication(
                            candidateProviderRank = accepted.providerRank,
                            pendingProviderRanks = pendingProviderRanks(),
                            forcedSource = forcedSource,
                            alreadyPublished = publishedCandidate != null
                        )
                    ) {
                        if (heldCandidate == null) {
                            holdDeadlineNanos = System.nanoTime() + MOTION_PRIORITY_SETTLE_MS * NANOS_PER_MILLI
                        }
                        heldCandidate = accepted
                        Timber.d(
                            "motion priority settle holding provider=%s title=%s pending=%s",
                            accepted.candidate.provider,
                            identity.title,
                            pendingProviderRanks()
                        )
                        continue
                    }
                    heldCandidate = null
                    if (!publish(accepted)) return@supervisorScope
                }
                val stillHeld = heldCandidate
                if (
                    stillHeld != null &&
                    !shouldHoldMotionPublication(
                        candidateProviderRank = stillHeld.providerRank,
                        pendingProviderRanks = pendingProviderRanks(),
                        forcedSource = forcedSource,
                        alreadyPublished = publishedCandidate != null
                    )
                ) {
                    heldCandidate = null
                    if (!publish(stillHeld)) return@supervisorScope
                }
            }
            heldCandidate?.let { held ->
                heldCandidate = null
                if (!publish(held)) return@supervisorScope
            }
        }

        if (!shouldPublishMotionArtwork(networkPolicy.canResolveCurrent())) return@flow

        val stabilized = publishedArtwork
        if (stabilized == null) {
            val conclusive = shouldNegativeCacheMotionArtwork(
                providerFailed = providerFailed,
                verifierFailed = verifierFailed,
                verificationExhaustive = verificationExhaustive
            )
            if (conclusive) {
                Timber.d("motion resolve conclusive miss; saving negative cache title=%s source=%s", identity.title, source)
                repository.saveNegative(
                    identityKey = identityKey,
                    configEpoch = configEpoch,
                    expiresAt = System.currentTimeMillis() + config.negativeTtlMs
                )
            } else {
                Timber.d(
                    "motion resolve transient/non-exhaustive miss title=%s source=%s providerFailed=%b verifierFailed=%b exhaustive=%b",
                    identity.title,
                    source,
                    providerFailed,
                    verifierFailed,
                    verificationExhaustive
                )
            }
            repository.cleanup(configEpoch)
            return@flow
        }

        repository.save(stabilized)
        repository.cleanup(configEpoch)
        Timber.d(
            "motion resolved provider=%s scope=%s title=%s",
            stabilized.provider,
            publishedCandidate?.candidate?.scope,
            identity.title
        )
    }

    private suspend fun runProviderLookup(
        provider: MotionArtworkProvider,
        identity: MotionTrackIdentity,
        config: MotionArtworkConfig
    ): MotionArtworkProviderResult {
        val timeoutMs = motionArtworkProviderTimeoutMs(provider.id, config.requestTimeoutMs)
        return try {
            withTimeoutOrNull(timeoutMs) { provider.find(identity) } ?: run {
                Timber.d(
                    "Motion provider %s TIMEOUT after %dms for %s / %s",
                    provider.id,
                    timeoutMs,
                    identity.title,
                    identity.artists.joinToString()
                )
                MotionArtworkProviderResult.Failed()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Motion provider %s failed", provider.id)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private fun rankMotionCandidates(
        identity: MotionTrackIdentity,
        candidates: List<MotionArtworkCandidate>,
        config: MotionArtworkConfig,
        providerRanks: Map<String, Int>
    ): List<MotionArtworkRankedCandidate> = candidates
        .map { candidate -> candidate to CanonicalTrackMatcher.match(identity, candidate) }
        .onEach { (candidate, match) ->
            Timber.d(
                "motion candidate %s scope=%s score=%d accepted=%b minimum=%d album=%s",
                candidate.provider,
                candidate.scope,
                match.score,
                match.accepted,
                config.minimumConfidence,
                candidate.identity.album
            )
        }
        .mapNotNull { (candidate, match) ->
            if (!match.accepted || match.score < config.minimumConfidence) null
            else MotionArtworkRankedCandidate(
                candidate,
                match.score,
                providerRanks[candidate.provider] ?: Int.MAX_VALUE
            )
        }
        .sortedWith(
            compareBy<MotionArtworkRankedCandidate> { it.providerRank }
                .thenByDescending { it.confidence }
        )

    private fun motionArtworkFrom(
        ranked: MotionArtworkRankedCandidate,
        identityKey: String,
        configEpoch: Long,
        config: MotionArtworkConfig
    ): MotionArtwork {
        val now = System.currentTimeMillis()
        val candidate = ranked.candidate
        return MotionArtwork(
            identityKey = identityKey,
            provider = candidate.provider,
            url = candidate.url,
            mimeType = candidate.mimeType,
            width = candidate.width,
            height = candidate.height,
            confidence = ranked.confidence,
            expiresAtMs = minOf(candidate.expiresAtMs, now + config.positiveTtlMs),
            lastVerifiedAtMs = now,
            configEpoch = configEpoch
        )
    }

    private fun providersFor(epoch: Long, config: MotionArtworkConfig): List<MotionArtworkProvider> = synchronized(runtimeLock) {
        if (activeEpoch != epoch) {
            activeProviders = config.providerOrder.mapNotNull { providerFactories[it]?.invoke(config) }
            activeEpoch = epoch
        }
        activeProviders
    }
}

private data class IndexedMotionProviderOutcome(
    val index: Int,
    val outcome: MotionArtworkProviderResult
)

private const val DEFAULT_MOTION_METADATA_COUNTRY = "IT"
private const val MOTION_METADATA_FOREGROUND_BUDGET_MS = 150L

internal fun motionMetadataForegroundBudgetMs(track: Track): Long =
    if (track.isrc.isNotBlank() && !isUnusableMotionAlbum(track.album)) 0L else MOTION_METADATA_FOREGROUND_BUDGET_MS

internal fun motionArtworkProviderTimeoutMs(providerId: String, configuredTimeoutMs: Long): Long = configuredTimeoutMs

internal class MotionMetadataWarmCache(private val maxEntries: Int = 64) {
    private val entries = LinkedHashMap<String, Track>(16, 0.75f, true)

    @Synchronized
    fun get(track: Track): Track? = entries[key(track)]

    @Synchronized
    fun put(original: Track, prepared: Track) {
        if (prepared == original) return
        entries[key(original)] = prepared
        while (entries.size > maxEntries) {
            val iterator = entries.entries.iterator()
            if (!iterator.hasNext()) return
            iterator.next()
            iterator.remove()
        }
    }

    private fun key(track: Track): String = listOf(
        track.id,
        track.title,
        track.artist,
        track.album,
        track.isrc,
        track.year,
        track.releaseDate,
        track.albumArtist,
        track.albumBrowseId,
        track.artistBrowseIds.joinToString(",")
    ).joinToString("|") { value -> value.trim().lowercase(Locale.ROOT) }
}

internal fun motionArtworkCacheKey(identityKey: String, source: DerbisCanvasSource): String = identityKey

internal fun motionArtworkRequestKey(track: Track, source: DerbisCanvasSource): String =
    motionArtworkCacheKey(MotionArtworkIdentityKey.create(track), source)

internal fun motionArtworkProviderOrder(
    configuredOrder: List<String>,
    source: DerbisCanvasSource
): List<String> = when (source) {
    DerbisCanvasSource.Auto -> configuredOrder.filter {
        it == AppleMusicAnimatedArtworkProvider.PROVIDER_ID ||
            it == SpotifyCanvasProvider.PROVIDER_ID ||
            it == DerbisCanvasCatalogProvider.PROVIDER_ID
    }.distinct()
}

internal data class MotionArtworkRankedCandidate(
    val candidate: MotionArtworkCandidate,
    val confidence: Int,
    val providerRank: Int
)

internal data class MotionArtworkVerificationPlan(
    val candidates: List<MotionArtworkRankedCandidate>,
    val exhaustive: Boolean
)

internal fun buildMotionArtworkVerificationPlan(
    ranked: List<MotionArtworkRankedCandidate>,
    maxCandidatesPerProvider: Int = 3
): MotionArtworkVerificationPlan {
    require(maxCandidatesPerProvider > 0)
    val selectedPerProvider = mutableMapOf<Int, Int>()
    val candidates = ranked.filter { candidate ->
        val selected = selectedPerProvider[candidate.providerRank] ?: 0
        if (selected >= maxCandidatesPerProvider) {
            false
        } else {
            selectedPerProvider[candidate.providerRank] = selected + 1
            true
        }
    }
    return MotionArtworkVerificationPlan(
        candidates = candidates,
        exhaustive = candidates.size == ranked.size
    )
}

internal fun shouldNegativeCacheMotionArtwork(
    providerFailed: Boolean,
    verifierFailed: Boolean,
    verificationExhaustive: Boolean
): Boolean = !providerFailed && !verifierFailed && verificationExhaustive

internal fun shouldContinueMotionArtworkFallback(result: MotionArtworkProviderResult): Boolean =
    result !is MotionArtworkProviderResult.Found || result.candidates.isEmpty()

internal fun shouldPublishMotionArtwork(policyAllowsRemote: Boolean): Boolean = policyAllowsRemote

internal fun cancelMotionArtworkLookups(lookups: Iterable<Job>) {
    lookups.forEach { lookup -> lookup.cancel() }
}

internal const val MAX_MOTION_ARTWORK_UPGRADES = 2

internal fun shouldPublishMotionUpgrade(
    publishedProviderRank: Int?,
    candidateProviderRank: Int,
    forcedSource: Boolean,
    upgradesUsed: Int,
    maxUpgrades: Int = MAX_MOTION_ARTWORK_UPGRADES
): Boolean {
    if (publishedProviderRank == null) return true
    if (forcedSource) return false
    if (upgradesUsed >= maxUpgrades) return false
    return candidateProviderRank < publishedProviderRank
}

internal const val MOTION_PRIORITY_SETTLE_MS = 1_200L
private const val NANOS_PER_MILLI = 1_000_000L

internal fun shouldHoldMotionPublication(
    candidateProviderRank: Int,
    pendingProviderRanks: Collection<Int>,
    forcedSource: Boolean,
    alreadyPublished: Boolean
): Boolean = !forcedSource &&
    !alreadyPublished &&
    pendingProviderRanks.any { pendingRank -> pendingRank < candidateProviderRank }

internal class MotionArtworkRequestCoordinator(
    private val scope: CoroutineScope
) {
    private val sessions = mutableMapOf<String, MotionProgressiveSession>()

    fun cancel(requestKey: String) {
        val session = synchronized(sessions) {
            sessions.remove(requestKey)
        }
        session?.cancel()
    }

    fun share(
        requestKey: String,
        block: suspend (emit: suspend (MotionArtwork) -> Unit) -> Unit
    ): Flow<MotionArtwork> = flow {
        while (true) {
            val session = synchronized(sessions) {
                sessions.getOrPut(requestKey) {
                    MotionProgressiveSession().also { newSession ->
                        val worker = scope.launch(start = CoroutineStart.LAZY) {
                            try {
                                block { artwork ->
                                    newSession.emit(artwork)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Throwable) {
                                Timber.d(error, "Motion artwork resolution failed")
                            } finally {
                                withContext(NonCancellable) {
                                    synchronized(sessions) {
                                        if (sessions[requestKey] === newSession) {
                                            sessions.remove(requestKey)
                                        }
                                    }
                                    newSession.complete()
                                }
                            }
                        }
                        newSession.attachWorker(worker)
                    }
                }
            }
            if (session.collectInto { artwork -> emit(artwork) }) return@flow
            synchronized(sessions) {
                if (sessions[requestKey] === session) {
                    sessions.remove(requestKey)
                }
            }
            if (scope.coroutineContext[Job]?.isActive != true) return@flow
        }
    }
}

internal class MotionProgressiveSession {
    private val stateLock = Any()
    private val collectors = mutableListOf<Channel<MotionArtwork>>()
    @Volatile
    private var worker: Job? = null
    private var workerStarted = false
    private var currentArtwork: MotionArtwork? = null
    private var isCompleted = false
    private var acceptingSubscriptions = true

    fun attachWorker(job: Job) {
        synchronized(stateLock) {
            worker = job
        }
        job.invokeOnCompletion {
            complete()
        }
    }

    suspend fun emit(artwork: MotionArtwork) {
        val targets = synchronized(stateLock) {
            if (isCompleted) {
                emptyList()
            } else {
                currentArtwork = artwork
                collectors.toList()
            }
        }
        for (target in targets) {
            target.trySend(artwork)
        }
    }

    fun complete() {
        val targets = synchronized(stateLock) {
            if (isCompleted) return
            acceptingSubscriptions = false
            isCompleted = true
            collectors.toList()
        }
        for (target in targets) {
            target.close()
        }
    }

    fun cancel() {
        val targets = synchronized(stateLock) {
            if (isCompleted) return
            acceptingSubscriptions = false
            isCompleted = true
            collectors.toList()
        }
        for (target in targets) {
            target.cancel(CancellationException("Motion artwork session cancelled"))
        }
        worker?.cancel()
    }

    suspend fun collectInto(emit: suspend (MotionArtwork) -> Unit): Boolean {
        val channel = Channel<MotionArtwork>(Channel.UNLIMITED)
        val workerToStart: Job?
        synchronized(stateLock) {
            if (!acceptingSubscriptions || isCompleted) return false
            currentArtwork?.let { artwork ->
                channel.trySend(artwork)
            }
            collectors.add(channel)
            workerToStart = if (!workerStarted) worker else null
            if (workerToStart != null) workerStarted = true
        }

        try {
            if (workerToStart != null && !workerToStart.start()) {
                complete()
            }
            var lastEmitted: MotionArtwork? = null
            for (artwork in channel) {
                if (artwork != lastEmitted) {
                    lastEmitted = artwork
                    emit(artwork)
                }
            }
        } finally {
            val cancelWorker = synchronized(stateLock) {
                collectors.remove(channel)
                val shouldCancel = collectors.isEmpty() && !isCompleted
                if (shouldCancel) acceptingSubscriptions = false
                shouldCancel
            }
            if (cancelWorker) worker?.cancel()
        }
        return true
    }
}
