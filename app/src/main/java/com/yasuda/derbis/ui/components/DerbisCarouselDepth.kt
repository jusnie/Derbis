package com.yasuda.derbis.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import kotlin.math.abs

private const val CarouselDepthMinScale = 0.94f
private const val CarouselDepthMaxTilt = 5f
private const val CarouselDepthMinAlpha = 0.86f
private const val CarouselDepthCameraDistance = 14f

internal fun carouselDepthFraction(itemCenter: Float, viewportCenter: Float, viewportWidth: Float): Float {
    if (viewportWidth <= 0f) return 0f
    return ((itemCenter - viewportCenter) / viewportWidth).coerceIn(-1f, 1f)
}

fun Modifier.derbisCarouselDepth(
    state: LazyListState,
    key: Any,
    enabled: Boolean
): Modifier {
    if (!enabled) return this
    return graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        val fraction = carouselDepthFraction(
            itemCenter = item.offset + item.size / 2f,
            viewportCenter = viewportCenter,
            viewportWidth = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        )
        val distance = abs(fraction)
        val scale = 1f - (1f - CarouselDepthMinScale) * distance
        scaleX = scale
        scaleY = scale
        rotationY = -fraction * CarouselDepthMaxTilt
        cameraDistance = CarouselDepthCameraDistance * density
        alpha = 1f - (1f - CarouselDepthMinAlpha) * distance
        transformOrigin = TransformOrigin(if (fraction < 0f) 1f else 0f, 0.5f)
    }
}

@Composable
fun carouselDepthEnabled(): Boolean =
    LocalAnimationsEnabled.current && LocalDerbisVisualCapabilities.current.depthTransitions
