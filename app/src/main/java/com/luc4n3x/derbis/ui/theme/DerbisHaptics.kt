package com.luc4n3x.derbis.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

enum class DerbisHapticAction {
    Favorite,
    SeekSnap,
    TrackSwipe,
    Reorder,
    Confirm,
    Transport
}

@Immutable
class DerbisHaptics(
    private val feedback: HapticFeedback?,
    private val enabled: Boolean
) {
    fun perform(action: DerbisHapticAction) {
        if (!enabled) return
        val target = feedback ?: return
        target.performHapticFeedback(typeFor(action))
    }

    private fun typeFor(action: DerbisHapticAction): HapticFeedbackType = when (action) {
        DerbisHapticAction.SeekSnap, DerbisHapticAction.Reorder -> HapticFeedbackType.TextHandleMove
        DerbisHapticAction.Favorite, DerbisHapticAction.TrackSwipe, DerbisHapticAction.Confirm ->
            HapticFeedbackType.LongPress
        DerbisHapticAction.Transport -> HapticFeedbackType.ContextClick
    }

    companion object {
        val Disabled = DerbisHaptics(feedback = null, enabled = false)
    }
}

val LocalDerbisHaptics = staticCompositionLocalOf { DerbisHaptics.Disabled }

@Composable
fun rememberDerbisHaptics(enabled: Boolean): DerbisHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { DerbisHaptics(feedback, enabled) }
}
