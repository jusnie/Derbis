package com.yasuda.derbis.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.data.FuriganaSegment
import com.yasuda.derbis.data.KanjiFurigana
import com.yasuda.derbis.domain.LyricWord

/**
 * Renders Japanese lyrics with Kanji Furigana (Ruby Annotations above Kanji)
 * following the SpMp / Kanon pattern.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaRubyLine(
    lineText: String,
    baseColor: Color,
    rubyColor: Color = baseColor.copy(alpha = 0.82f),
    fontSize: TextUnit = 24.sp,
    lineHeight: TextUnit = (fontSize.value * 1.25f).sp,
    rubyFontSize: TextUnit = (fontSize.value * 0.42f).coerceAtLeast(10f).sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign = TextAlign.Start,
    shadow: Shadow? = null,
    modifier: Modifier = Modifier
) {
    val segments = remember(lineText) { KanjiFurigana.parse(lineText) }
    val arrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End, TextAlign.Right -> Arrangement.End
        else -> Arrangement.Start
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEach { segment ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (segment.hasRuby) {
                    Text(
                        text = segment.rubyText,
                        fontSize = rubyFontSize,
                        lineHeight = rubyFontSize,
                        color = rubyColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(rubyFontSize.value.dp))
                }
                Text(
                    text = segment.baseText,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = baseColor,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = shadow)
                )
            }
        }
    }
}

/**
 * Word-by-word timed karaoke Furigana component.
 * Synchronously highlights base Kanji and its Furigana ruby annotations in real-time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaraokeFuriganaWordTimedText(
    lineText: String,
    words: List<LyricWord>,
    positionProvider: () -> Long,
    isActive: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    textAlign: TextAlign,
    inactiveColor: Color,
    completedColor: Color,
    activeColor: Color,
    pendingColor: Color,
    fontWeight: FontWeight,
    fontFamily: FontFamily? = null,
    shadow: Shadow? = null,
    modifier: Modifier = Modifier
) {
    val segments = remember(lineText) { KanjiFurigana.parse(lineText) }
    val playbackPosition = if (isActive) positionProvider() else Long.MIN_VALUE

    val wordTransition = tween<Color>(
        durationMillis = 115,
        easing = CubicBezierEasing(0.16f, 0f, 0.10f, 1f)
    )
    val scaleTransition = tween<Float>(
        durationMillis = 115,
        easing = CubicBezierEasing(0.16f, 0f, 0.10f, 1f)
    )

    val arrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End, TextAlign.Right -> Arrangement.End
        else -> Arrangement.Start
    }

    val rubyFontSize = (fontSize.value * 0.42f).coerceAtLeast(10f).sp

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var characterCursor = 0
        segments.forEach { segment ->
            val segLen = segment.baseText.length
            val segStart = characterCursor
            val segEnd = characterCursor + segLen
            characterCursor = segEnd

            // Check timing against matching word if available
            val matchedWord = words.firstOrNull { word ->
                val wordTrimmed = word.text.trim()
                wordTrimmed.contains(segment.baseText) || segment.baseText.contains(wordTrimmed)
            }

            val isCurrentWord = matchedWord != null && (playbackPosition in matchedWord.startMs until matchedWord.endMs)
            val isCompletedWord = matchedWord != null && (playbackPosition >= matchedWord.endMs)

            val targetBaseColor = when {
                !isActive -> inactiveColor
                isCurrentWord -> activeColor
                isCompletedWord -> completedColor
                matchedWord == null && isActive -> completedColor
                else -> pendingColor
            }

            val targetRubyColor = targetBaseColor.copy(alpha = if (isCurrentWord) 0.95f else 0.75f)

            val animatedBaseColor by animateColorAsState(
                targetValue = targetBaseColor,
                animationSpec = wordTransition,
                label = "furigana-base-color"
            )
            val animatedRubyColor by animateColorAsState(
                targetValue = targetRubyColor,
                animationSpec = wordTransition,
                label = "furigana-ruby-color"
            )
            val wordScale by animateFloatAsState(
                targetValue = if (isCurrentWord) 1.028f else 1f,
                animationSpec = scaleTransition,
                label = "furigana-word-scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = wordScale
                    scaleY = wordScale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
            ) {
                if (segment.hasRuby) {
                    Text(
                        text = segment.rubyText,
                        fontSize = rubyFontSize,
                        lineHeight = rubyFontSize,
                        color = animatedRubyColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(rubyFontSize.value.dp))
                }
                Text(
                    text = segment.baseText,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = animatedBaseColor,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = if (isCurrentWord) shadow else null)
                )
            }
        }
    }
}
