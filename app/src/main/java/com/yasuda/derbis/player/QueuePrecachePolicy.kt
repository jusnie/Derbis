package com.yasuda.derbis.player

const val DEFAULT_STREAM_PRELOAD_TRIGGER_REMAINING_MS = 30_000L
const val SHORT_TRACK_STREAM_PRELOAD_THRESHOLD_RATIO = 0.70f

internal fun shouldTriggerStreamPreload(
    durationMs: Long,
    positionMs: Long,
    preloadEnabled: Boolean,
    alreadyPreloaded: Boolean,
    triggerRemainingMs: Long = DEFAULT_STREAM_PRELOAD_TRIGGER_REMAINING_MS,
    thresholdRatio: Float = SHORT_TRACK_STREAM_PRELOAD_THRESHOLD_RATIO
): Boolean {
    if (!preloadEnabled || alreadyPreloaded || durationMs <= 0L || positionMs < 0L) return false
    val remaining = durationMs - positionMs
    if (remaining <= 0L) return false
    if (remaining <= triggerRemainingMs) return true
    if (durationMs > 0L && (positionMs.toFloat() / durationMs.toFloat()) >= thresholdRatio) return true
    return false
}

internal fun isStreamPreloadValid(
    preparedTargetIdentity: String,
    requestedTargetIdentity: String,
    streamPreloaded: Boolean,
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean =
    streamPreloaded &&
        queuePrecacheMatchesTarget(
            preparedTargetIdentity = preparedTargetIdentity,
            requestedTargetIdentity = requestedTargetIdentity,
            preparedAtElapsedMs = preparedAtElapsedMs,
            nowElapsedMs = nowElapsedMs,
            maxAgeMs = maxAgeMs
        )

internal fun isQueuePrecacheFresh(
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean {
    if (preparedAtElapsedMs < 0L || nowElapsedMs < preparedAtElapsedMs || maxAgeMs < 0L) return false
    return nowElapsedMs - preparedAtElapsedMs <= maxAgeMs
}

internal fun queuePrecacheMatchesTransition(
    preparedSourceIdentity: String,
    preparedTargetIdentity: String,
    currentSourceIdentity: String,
    currentTargetIdentity: String,
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean =
    preparedSourceIdentity == currentSourceIdentity &&
        preparedTargetIdentity == currentTargetIdentity &&
        isQueuePrecacheFresh(preparedAtElapsedMs, nowElapsedMs, maxAgeMs)

internal fun queuePrecacheMatchesTarget(
    preparedTargetIdentity: String,
    requestedTargetIdentity: String,
    preparedAtElapsedMs: Long,
    nowElapsedMs: Long,
    maxAgeMs: Long
): Boolean =
    preparedTargetIdentity == requestedTargetIdentity &&
        isQueuePrecacheFresh(preparedAtElapsedMs, nowElapsedMs, maxAgeMs)

internal fun queuePrecacheMatchesTrackId(
    targetTrackId: String,
    resolvedTrackId: String,
    requestedTrackId: String
): Boolean =
    requestedTrackId.isNotBlank() &&
        (requestedTrackId == targetTrackId || requestedTrackId == resolvedTrackId)
