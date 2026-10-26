package com.luc4n3x.derbis.nexus.core

enum class DerbisDataSource {
    NETWORK,
    CACHE,
    FALLBACK,
    LOCAL
}

enum class DerbisFreshness {
    LIVE,
    FRESH,
    STALE
}

enum class DerbisFailureType {
    NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    ACCESS_DENIED,
    NOT_FOUND,
    INVALID_DATA,
    CANCELLED,
    SECURITY,
    UNSUPPORTED,
    UNKNOWN
}

sealed interface DerbisResult<out T> {
    data class Success<T>(
        val value: T,
        val source: DerbisDataSource = DerbisDataSource.NETWORK,
        val freshness: DerbisFreshness = DerbisFreshness.LIVE,
        val metadata: Map<String, String> = emptyMap()
    ) : DerbisResult<T>

    data class Failure(
        val type: DerbisFailureType,
        val retryable: Boolean,
        val message: String = "",
        val cause: Throwable? = null,
        val metadata: Map<String, String> = emptyMap()
    ) : DerbisResult<Nothing>
}

inline fun <T, R> DerbisResult<T>.map(transform: (T) -> R): DerbisResult<R> = when (this) {
    is DerbisResult.Success -> DerbisResult.Success(
        value = transform(value),
        source = source,
        freshness = freshness,
        metadata = metadata
    )
    is DerbisResult.Failure -> this
}

inline fun <T, R> DerbisResult<T>.fold(
    onSuccess: (DerbisResult.Success<T>) -> R,
    onFailure: (DerbisResult.Failure) -> R
): R = when (this) {
    is DerbisResult.Success -> onSuccess(this)
    is DerbisResult.Failure -> onFailure(this)
}
