@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.data.KanjiFurigana
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.ui.lyrics.FuriganaRubyLine
import com.yasuda.derbis.ui.lyrics.KaraokeFuriganaWordTimedText
import com.yasuda.derbis.ui.lyrics.buildTimedLyricText
import com.yasuda.derbis.ui.lyrics.wordHighlightTimings
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.OutfitFont

@Composable
internal fun SpicyVerseCard(
    line: LyricLine,
    lineNumber: Int,
    isActive: Boolean,
    isPast: Boolean,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    positionProvider: () -> Long,
    synced: Boolean,
    progressiveKaraoke: Boolean = true,
    showRomanization: Boolean = false,
    showTranslation: Boolean = false,
    showFurigana: Boolean = true,
    animationsEnabled: Boolean,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focus by animateFloatAsState(
        targetValue = if (isActive || isSelected) 1f else 0f,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(220)),
        label = "spicy-verse-focus"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive || isSelected) SpicyPaper else if (isPast) Color(0xFFA699A2) else SpicyMuted,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "spicy-verse-ink"
    )
    val fontSize = if (compact) 18.sp else 26.sp
    val lineHeight = if (compact) 25.sp else 34.sp
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { if (selectionMode) selected = isSelected }
            .then(clickModifier),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        Column(
            modifier = Modifier
                .width(if (compact) 24.dp else 34.dp)
                .padding(top = if (compact) 12.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = lineNumber.toString().padStart(2, '0'),
                color = if (isActive || isSelected) SpicyAccentCoral else SpicyMuted.copy(alpha = 0.82f),
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (synced && !compact) {
                Text(
                    text = formatSpicySeekMillis(line.startMs),
                    color = SpicyMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(SpicyVerseShape)
                .background(SpicyAccentCoral.copy(alpha = focus * if (isSelected) 0.19f else 0.10f))
                .border(1.dp, SpicyAccentCoral.copy(alpha = focus * 0.38f), SpicyVerseShape)
                .drawBehind {
                    if (focus > 0f) {
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 2.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = SpicyAccentCoral.copy(alpha = focus),
                            start = Offset(x, 17.dp.toPx()),
                            end = Offset(x, (size.height - 17.dp.toPx()).coerceAtLeast(17.dp.toPx())),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                .padding(horizontal = if (compact) 12.dp else 17.dp, vertical = if (compact) 10.dp else 14.dp)
        ) {
            val hasKanji = remember(line.text) { KanjiFurigana.containsKanji(line.text) }
            val words = remember(line) { wordHighlightTimings(line) }
            val karaoke = isActive && synced && progressiveKaraoke && !selectionMode
            when {
                showFurigana && hasKanji && karaoke -> KaraokeFuriganaWordTimedText(
                    lineText = line.text,
                    words = words,
                    positionProvider = positionProvider,
                    isActive = true,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    textAlign = TextAlign.Start,
                    inactiveColor = textColor,
                    completedColor = SpicyPaper,
                    activeColor = SpicyAccentGold,
                    pendingColor = SpicyMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = OutfitFont
                )
                showFurigana && hasKanji -> FuriganaRubyLine(
                    lineText = line.text,
                    baseColor = textColor,
                    rubyColor = if (isActive || isSelected) SpicyAccentGold else SpicyMuted,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = OutfitFont
                )
                karaoke -> SpicyWordHighlight(
                    line = line,
                    positionProvider = positionProvider,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    animationsEnabled = animationsEnabled
                )
                else -> Text(
                    text = line.text,
                    color = textColor,
                    fontFamily = OutfitFont,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.35).sp
                )
            }
            if (showRomanization && line.romanized.isNotBlank()) {
                Text(
                    text = line.romanized,
                    color = SpicyPaper.copy(alpha = 0.76f),
                    fontFamily = OutfitFont,
                    fontSize = if (compact) 12.sp else 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
            if (showTranslation && line.translated.isNotBlank()) {
                Text(
                    text = line.translated,
                    color = SpicyAccentGold,
                    fontFamily = OutfitFont,
                    fontSize = if (compact) 13.sp else 16.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SpicyWordHighlight(
    line: LyricLine,
    positionProvider: () -> Long,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    animationsEnabled: Boolean
) {
    val timedText = remember(line) { buildTimedLyricText(line.text, wordHighlightTimings(line)) }
    if (timedText.words.isEmpty()) {
        Text(line.text, color = SpicyPaper, fontFamily = OutfitFont, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.ExtraBold)
        return
    }
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        var consumed = 0
        timedText.words.forEach { word ->
            val end = (word.startIndex + word.length).coerceIn(consumed, timedText.text.length)
            val segment = timedText.text.substring(consumed, end)
            consumed = end
            val phase by remember(word, positionProvider) {
                derivedStateOf {
                    val position = positionProvider()
                    when {
                        position >= word.endMs -> 2
                        position >= word.startMs -> 1
                        else -> 0
                    }
                }
            }
            val wordColor by animateColorAsState(
                targetValue = when (phase) {
                    2 -> SpicyPaper
                    1 -> SpicyAccentGold
                    else -> SpicyMuted
                },
                animationSpec = DerbisMotion.spec(animationsEnabled, tween(100)),
                label = "spicy-word-ink"
            )
            Text(
                text = segment,
                color = wordColor,
                fontFamily = OutfitFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = (-0.35).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.drawBehind {
                    if (phase == 1) {
                        val progress = ((positionProvider() - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                        val from = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
                        val to = if (layoutDirection == LayoutDirection.Rtl) size.width * (1f - progress) else size.width * progress
                        drawLine(
                            color = SpicyAccentCoral,
                            start = Offset(from, size.height - 1.dp.toPx()),
                            end = Offset(to, size.height - 1.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            )
        }
    }
}
