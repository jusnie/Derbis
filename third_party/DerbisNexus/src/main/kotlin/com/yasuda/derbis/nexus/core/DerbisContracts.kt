package com.yasuda.derbis.nexus.core

data class DerbisRequestContext(
    val languageCode: String = "en",
    val countryCode: String = "US",
    val offlineAllowed: Boolean = true,
    val deadlineMs: Long = 30_000L,
    val attributes: Map<String, String> = emptyMap()
)

fun interface DerbisCatalogGateway<Q, R> {
    suspend fun execute(query: Q, context: DerbisRequestContext): DerbisResult<R>
}

fun interface DerbisStreamResolver<I, O> {
    suspend fun resolve(input: I, context: DerbisRequestContext): DerbisResult<O>
}

fun interface DerbisLyricsGateway<I, O> {
    suspend fun load(input: I, context: DerbisRequestContext): DerbisResult<O>
}

interface DerbisQueueStore<T> {
    suspend fun load(): DerbisResult<List<T>>
    suspend fun save(items: List<T>): DerbisResult<Unit>
}

fun interface DerbisRecommendationEngine<T, C> {
    suspend fun rank(candidates: List<T>, context: C): DerbisResult<List<T>>
}

fun interface DerbisPlaybackHistoryStore<E> {
    suspend fun append(event: E): DerbisResult<Unit>
}

interface DerbisPlaybackPort<T> {
    suspend fun prepare(item: T): DerbisResult<Unit>
    suspend fun play(): DerbisResult<Unit>
    suspend fun pause(): DerbisResult<Unit>
    suspend fun seekTo(positionMs: Long): DerbisResult<Unit>
    suspend fun stop(): DerbisResult<Unit>
}
