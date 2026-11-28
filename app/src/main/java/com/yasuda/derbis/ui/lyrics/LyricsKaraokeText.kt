package com.yasuda.derbis.ui.lyrics

import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.LyricWord

fun buildTimedLyricText(lineText: String, words: List<LyricWord>): TimedLyricText =
    alignTimedLyricText(lineText, words) ?: buildTimedLyricText(words)

/**
 * Supplies a stable word timeline even when a lyrics provider only supplies line timings.
 *
 * Provider word timestamps always win. The fallback estimates how long each word is actually
 * *sung* - a fixed onset plus a cost per character - and fits that estimate into the line
 * instead of stretching the words across the whole line.
 *
 * That difference is the whole point. An LRC line ends where the *next* line starts, so the
 * tail between the last word and that start is a breath, a reverb, or several bars of nothing.
 * Spreading the tokens across that tail fired every word after the syllable being sung, which
 * is exactly what made word-by-word highlight read as lagging behind the vocal.
 */
fun wordHighlightTimings(line: LyricLine): List<LyricWord> {
    val providerWords = line.words.filter { word ->
        word.text.isNotBlank() && word.endMs > word.startMs
    }
    if (providerWords.isNotEmpty()) return providerWords
    if (line.isInstrumental || line.text.isBlank()) return emptyList()

    val tokens = WORD_TOKEN.findAll(line.text).map { it.value }.toList()
    if (tokens.isEmpty()) return emptyList()

    val weights = tokens.map { token -> WORD_ONSET_MS + WORD_MS_PER_CHARACTER * token.characterCost() }
    val estimateMs = weights.sum()
    val spanMs = (line.endMs - line.startMs).takeIf { it > 0L }
    val sungMs = when {
        spanMs == null -> estimateMs
        // A line that is shorter than the estimate compresses the words: they cannot outlive it.
        // A line that is much longer does not stretch them, because the extra time is silence.
        // MAX_WORD_STRETCH leaves room for a genuinely slow take without letting a gap invent
        // word timings that never happened.
        else -> minOf(spanMs, (estimateMs * MAX_WORD_STRETCH).toLong())
    }.coerceAtLeast(tokens.size.toLong())

    val lineEnd = line.startMs + sungMs
    var cursor = line.startMs
    return tokens.mapIndexed { index, token ->
        val duration = if (index == tokens.lastIndex) {
            (lineEnd - cursor).coerceAtLeast(1L)
        } else {
            (weights[index] * sungMs / estimateMs).coerceAtLeast(1L)
        }
        val end = (cursor + duration).coerceAtMost(lineEnd).coerceAtLeast(cursor + 1L)
        LyricWord(startMs = cursor, endMs = end, text = token).also { cursor = end }
    }
}

private fun String.characterCost(): Int =
    count { character -> character.isLetterOrDigit() }.coerceAtLeast(1)

private val WORD_TOKEN = Regex("\\S+")
/** A word needs an attack before it is heard, then time for each character actually sung. */
private const val WORD_ONSET_MS = 90L
private const val WORD_MS_PER_CHARACTER = 52L
/** A slow take can hold each word longer than the estimate, but only this much longer. */
private const val MAX_WORD_STRETCH = 1.35f

fun buildTimedLyricText(words: List<LyricWord>): TimedLyricText {
    val sourceSpacing = words.any { word ->
        word.text.firstOrNull()?.isWhitespace() == true || word.text.lastOrNull()?.isWhitespace() == true
    }
    val text = StringBuilder()
    val timedWords = ArrayList<TimedLyricWord>(words.size)
    var previousEndedWithWhitespace = false
    words.forEach { word ->
        val value = word.text.trim()
        if (value.isNotBlank()) {
            val separated = if (sourceSpacing) {
                previousEndedWithWhitespace || word.text.first().isWhitespace()
            } else {
                !value.first().isPunctuationWithoutLeadingSpace()
            }
            if (text.isNotEmpty() && separated) text.append(' ')
            val startIndex = text.length
            text.append(value)
            timedWords += timedLyricWord(startIndex, value.length, word)
        }
        previousEndedWithWhitespace = word.text.lastOrNull()?.isWhitespace() == true
    }
    return TimedLyricText(text.toString(), timedWords)
}

private fun alignTimedLyricText(lineText: String, words: List<LyricWord>): TimedLyricText? {
    if (lineText.isBlank() || words.isEmpty()) return null
    val tokens = words.filter { it.text.isNotBlank() }
    val timedWords = ArrayList<TimedLyricWord>(tokens.size)
    var cursor = 0
    tokens.forEachIndexed { index, word ->
        val wordText = word.text.trim()
        val start = lineText.skipWhile(cursor) { it.isWhitespace() }
        val direct = matchLyricToken(lineText, start, wordText)
        val tokenStart = if (direct != null) start else lineText.skipWhile(start) { it.isLyricSeparator() }
        val end = direct ?: matchLyricToken(lineText, tokenStart, wordText) ?: return null
        val nextTokenStart = tokens.getOrNull(index + 1)?.text?.trim()?.firstOrNull()
        val extendedEnd = lineText.skipWhile(end) { it.isTrailingLyricPunctuation() && it != nextTokenStart }
        timedWords += timedLyricWord(tokenStart, extendedEnd - tokenStart, word)
        cursor = extendedEnd
    }
    for (index in cursor until lineText.length) {
        if (!lineText[index].isLyricSeparator()) return null
    }
    return if (timedWords.isEmpty()) null else TimedLyricText(lineText, timedWords)
}

private inline fun String.skipWhile(start: Int, predicate: (Char) -> Boolean): Int {
    var index = start
    while (index < length && predicate(this[index])) index++
    return index
}

private fun matchLyricToken(text: String, start: Int, token: String): Int? {
    var textIndex = start
    var tokenIndex = 0
    while (tokenIndex < token.length) {
        val expected = token[tokenIndex]
        if (expected.isWhitespace()) {
            tokenIndex++
            continue
        }
        while (textIndex < text.length && text[textIndex].isWhitespace()) textIndex++
        if (textIndex >= text.length || !text[textIndex].equals(expected, ignoreCase = true)) return null
        textIndex++
        tokenIndex++
    }
    return textIndex
}

private fun timedLyricWord(startIndex: Int, length: Int, word: LyricWord) = TimedLyricWord(
    startIndex = startIndex,
    length = length,
    startMs = word.startMs,
    endMs = word.endMs.coerceAtLeast(word.startMs + 1L)
)

private fun Char.isLyricSeparator(): Boolean = !isLetterOrDigit() && !isSurrogate()

private fun Char.isTrailingLyricPunctuation(): Boolean = !isWhitespace() && !isLetterOrDigit() && !isSurrogate()
