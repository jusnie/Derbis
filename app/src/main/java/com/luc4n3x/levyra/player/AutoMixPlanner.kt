package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.DerbisBeatGrid
import com.luc4n3x.levyra.domain.DerbisMixStudio
import com.luc4n3x.levyra.domain.DerbisMixTransitionPreset
import com.luc4n3x.levyra.domain.DerbisMixTransitionSetting
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class CrossfadeCurve { EqualPower, LinearFade, IncomingRise }

data class AutoMixPlan(
    val transitionMs: Long,
    val preloadLeadMs: Long,
    val curve: CrossfadeCurve = CrossfadeCurve.EqualPower,
    val transitionStartMs: Long? = null,
    val incomingStartMs: Long = 0L,
    val incomingTempo: Float = 1f,
    val beatAligned: Boolean = false
)
data class CrossfadeGains(val outgoing: Float, val incoming: Float)

private data class BeatAlignedAutoMix(
    val startMs: Long,
    val durationMs: Long,
    val incomingStartMs: Long,
    val incomingTempo: Float
)

internal fun planAutoMix(
    current: Track,
    next: Track?,
    settings: LevyraAudioSettings,
    repeatMode: RepeatMode,
    videoMode: Boolean,
    lowRam: Boolean,
    shuffleEnabled: Boolean = false,
    mixStudioSetting: DerbisMixTransitionSetting? = null,
    currentBeatGrid: DerbisBeatGrid? = null,
    nextBeatGrid: DerbisBeatGrid? = null
): AutoMixPlan? {
    val normalized = settings.normalized()
    val savedMix = mixStudioSetting?.normalized()
    if ((!normalized.gaplessEnabled || normalized.crossfadeSeconds <= 0) && savedMix == null) return null
    if (next == null || repeatMode == RepeatMode.One || videoMode || lowRam || current.durationMs <= MIN_TRACK_MS) return null
    if (savedMix == null && continuesSameRelease(current, next, shuffleEnabled)) return null
    val base = savedMix?.durationMs
        ?: if (savedMix != null) DEFAULT_STUDIO_TRANSITION_MS else (normalized.crossfadeSeconds * 1_000L)
    val adaptive = if (savedMix == null && normalized.djSoftMode) {
        val energyDistance = abs(current.energy - next.energy)
        val vocalOverlap = (current.vocal + next.vocal) / 2
        when {
            vocalOverlap >= 70 -> base - 1_500L
            energyDistance <= 15 -> base + 1_500L
            energyDistance >= 45 -> base - 1_000L
            else -> base
        }
    } else base
    val styled = when (savedMix?.preset) {
        DerbisMixTransitionPreset.Cut -> MIN_TRANSITION_MS
        else -> adaptive
    }
    val requestedTransition = styled.coerceIn(MIN_TRANSITION_MS, MAX_TRANSITION_MS)
        .coerceAtMost(current.durationMs / 4L)
    val manualOutgoingStart = savedMix?.outgoingStartMs?.coerceIn(
        0L,
        (current.durationMs - MIN_TRANSITION_MS).coerceAtLeast(0L)
    )
    val manualIncomingStart = savedMix?.incomingStartMs?.coerceIn(
        0L,
        (next.durationMs - MIN_TRANSITION_MS).coerceAtLeast(0L)
    )
    // A manually chosen timeline position must be honoured exactly, never replaced by analysis.
    val beatPlan = if (
        manualOutgoingStart == null && manualIncomingStart == null &&
        normalized.beatSyncEnabled && savedMix?.beatSyncEnabled == true && currentBeatGrid != null && nextBeatGrid != null
    ) {
        val sync = DerbisMixStudio.beatSync(currentBeatGrid, nextBeatGrid)
        val start = DerbisMixStudio.beatAlignedTransitionStartMs(current.durationMs, requestedTransition, currentBeatGrid)
        if (sync.aligned && start != null) {
            val duration = current.durationMs - start
            if (duration in MIN_TRANSITION_MS..MAX_TRANSITION_MS) {
                BeatAlignedAutoMix(start, duration, sync.incomingSeekMs, sync.incomingTempo)
            } else null
        } else null
    } else null
    val transition = (beatPlan?.durationMs ?: requestedTransition)
        .coerceAtMost(manualOutgoingStart?.let { current.durationMs - it } ?: Long.MAX_VALUE)
    if (transition < MIN_TRANSITION_MS) return null
    return AutoMixPlan(
        transitionMs = transition,
        preloadLeadMs = (transition + PRELOAD_MARGIN_MS).coerceAtMost(MAX_PRELOAD_LEAD_MS),
        curve = when (savedMix?.preset) {
            DerbisMixTransitionPreset.Fade -> CrossfadeCurve.LinearFade
            DerbisMixTransitionPreset.Rise -> CrossfadeCurve.IncomingRise
            else -> CrossfadeCurve.EqualPower
        },
        transitionStartMs = manualOutgoingStart ?: beatPlan?.startMs,
        incomingStartMs = manualIncomingStart ?: beatPlan?.incomingStartMs ?: 0L,
        incomingTempo = beatPlan?.incomingTempo ?: 1f,
        beatAligned = beatPlan != null
    )
}

internal fun continuesSameRelease(current: Track, next: Track, shuffleEnabled: Boolean): Boolean {
    val runningOrderKnown = current.trackNumber > 0 && next.trackNumber > 0
    val sameRelease = catalogIdentityMatch(current.albumBrowseId, next.albumBrowseId)
        ?: catalogIdentityMatch(current.upc, next.upc)
        ?: (runningOrderKnown && sameTitledRelease(current, next))
    if (!sameRelease) return false
    return if (runningOrderKnown) followsInRunningOrder(current, next) else !shuffleEnabled
}

private fun catalogIdentityMatch(first: String, second: String): Boolean? {
    val left = first.trim()
    val right = second.trim()
    if (left.isEmpty() || right.isEmpty()) return null
    return left.equals(right, ignoreCase = true)
}

private fun sameTitledRelease(current: Track, next: Track): Boolean =
    catalogIdentityMatch(current.album, next.album) == true &&
        catalogIdentityMatch(current.artist, next.artist) == true

private fun followsInRunningOrder(current: Track, next: Track): Boolean {
    val currentDisc = current.discNumber
    val nextDisc = next.discNumber
    val sameDisc = currentDisc == nextDisc || currentDisc <= 0 || nextDisc <= 0
    if (sameDisc && next.trackNumber == current.trackNumber + 1) return true
    return currentDisc > 0 && nextDisc == currentDisc + 1 && next.trackNumber == 1
}

internal fun equalPowerCrossfade(
    progress: Float,
    curve: CrossfadeCurve = CrossfadeCurve.EqualPower
): CrossfadeGains {
    val linear = progress.coerceIn(0f, 1f)
    if (curve == CrossfadeCurve.LinearFade) {
        return CrossfadeGains(outgoing = 1f - linear, incoming = linear)
    }
    val t = when (curve) {
        CrossfadeCurve.EqualPower -> linear
        CrossfadeCurve.IncomingRise -> linear * linear
        CrossfadeCurve.LinearFade -> linear
    }
    val angle = t * (PI.toFloat() / 2f)
    return CrossfadeGains(outgoing = cos(angle), incoming = sin(angle))
}

internal fun crossfadeHandoffNeedsResync(
    primaryPositionMs: Long,
    secondaryPositionMs: Long,
    toleranceMs: Long = 250L
): Boolean = abs(primaryPositionMs.coerceAtLeast(0L) - secondaryPositionMs.coerceAtLeast(0L)) > toleranceMs.coerceAtLeast(0L)

internal fun crossfadeHandoffSeekPosition(
    secondaryPositionMs: Long,
    durationMs: Long,
    leadMs: Long = 120L
): Long {
    val requested = secondaryPositionMs.coerceAtLeast(0L) + leadMs.coerceAtLeast(0L)
    if (durationMs <= 0L) return requested
    return requested.coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
}

internal fun crossfadeStepWallClockMs(
    mediaStepMs: Long,
    playbackSpeed: Float,
    minWallClockMs: Long = 10L
): Long {
    val speed = if (playbackSpeed.isFinite() && playbackSpeed > 0f) playbackSpeed else 1f
    return ((mediaStepMs.toFloat() / speed).toLong()).coerceAtLeast(minWallClockMs)
}

private const val MIN_TRACK_MS = 30_000L
private const val MIN_TRANSITION_MS = 800L
private const val MAX_TRANSITION_MS = 30_000L
private const val PRELOAD_MARGIN_MS = 8_000L
private const val MAX_PRELOAD_LEAD_MS = 40_000L
private const val DEFAULT_STUDIO_TRANSITION_MS = 6_000L
