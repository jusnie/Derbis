package com.yasuda.derbis.ui

import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Immersive system chrome (video-app style) for every skin.
 *
 * The status and navigation bars are hidden on launch and kept hidden while the app is in use;
 * a swipe from the screen edge reveals them transiently ([WindowInsetsCompat.Type.statusBars] or
 * [WindowInsetsCompat.Type.navigationBars] become zero-height insets while hidden, so every
 * `statusBarsPadding` in the tree collapses automatically instead of leaving an empty strip).
 *
 * [ChromeScrollToggle] lets the bars disappear when the listener scrolls down a feed and come
 * back when they scroll up — the same contract YouTube's fullscreen follows.
 */
internal object ChromeImmersion {
    /** Single source of truth for the bar state, surviving recomposition and focus changes. */
    var currentHidden: Boolean = true
        private set

    fun apply(activity: Activity, hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (hidden) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
        } else {
            controller.show(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
        }
        currentHidden = hidden
    }

    /** Re-assert the current state after a resume or a window-focus change. */
    fun reapply(activity: Activity) = apply(activity, currentHidden)
}

/**
 * Root-level nested-scroll watcher. Scrolling down hides the system chrome; scrolling up brings
 * it back. Accumulates drag and fling deltas and flips only after a threshold so tiny wobbles
 * don't flap the bars. Tracks the root's `onPreScroll` — outermost consumer, sees the full
 * delta once, with no double counting from postScroll.
 */
internal class ChromeScrollToggle {
    private var accumulated = 0f

    private val thresholdPx = SCROLL_CHROME_TOGGLE_THRESHOLD_PX

    fun onScroll(deltaY: Float, activity: Activity?) {
        if (activity == null || deltaY == 0f) return
        if ((accumulated > 0f) != (deltaY > 0f)) accumulated = 0f
        accumulated += deltaY
        // Scrolling down the feed (finger drags up) arrives as negative deltas.
        if (accumulated <= -thresholdPx) {
            accumulated = 0f
            if (!ChromeImmersion.currentHidden) ChromeImmersion.apply(activity, true)
        } else if (accumulated >= thresholdPx) {
            accumulated = 0f
            if (ChromeImmersion.currentHidden) ChromeImmersion.apply(activity, false)
        }
    }

    fun connection(activity: Activity?): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // User drags and flings both count; only side-effect (programmatic) scrolls are ignored.
                if (source != NestedScrollSource.SideEffect && available.y != 0f) {
                    onScroll(available.y, activity)
                }
                return Offset.Zero
            }
        }
}

private const val SCROLL_CHROME_TOGGLE_THRESHOLD_PX = 96f
