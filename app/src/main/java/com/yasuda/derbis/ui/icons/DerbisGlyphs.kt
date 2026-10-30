package com.yasuda.derbis.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Derbis glyphs: the app's chrome icons rebuilt on a 2 dp stroke, 24 x 24 grid.
 *
 * Sourced from Lucide (ISC licence) through the Open Symbols set, which reshapes popular open
 * icon libraries into Apple's SF Symbols naming and metrics. Thin, evenly weighted strokes are what
 * makes a toolbar read like iOS 26 instead of Material: the shapes here deliberately drop the
 * filled/heavy Material silhouettes in favour of the lighter outline language that sits on glass.
 *
 * Call sites keep using `Icons.Rounded.Name`; only the import changes, so a glyph can be adopted
 * one screen at a time. Icons that are not overridden here keep falling through to Material.
 */
private fun glyph(
    name: String,
    vararg paths: String,
    filled: Boolean = false,
    strokeWidth: Float = 2f
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    paths.forEach { d ->
        addPath(
            pathData = PathParser().parsePathString(d).toNodes().toList(),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 4f
        )
    }
}.build()

private val _playArrow: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("PlayArrow",
        "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"
    )
}

private val _pause: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Pause",
        "M15.000000 3.000000 L18.000000 3.000000 a1.000000 1.000000 0 0 1 1.000000 1.000000 L19.000000 20.000000 a1.000000 1.000000 0 0 1 -1.000000 1.000000 L15.000000 21.000000 a1.000000 1.000000 0 0 1 -1.000000 -1.000000 L14.000000 4.000000 a1.000000 1.000000 0 0 1 1.000000 -1.000000 Z",
        "M6.000000 3.000000 L9.000000 3.000000 a1.000000 1.000000 0 0 1 1.000000 1.000000 L10.000000 20.000000 a1.000000 1.000000 0 0 1 -1.000000 1.000000 L6.000000 21.000000 a1.000000 1.000000 0 0 1 -1.000000 -1.000000 L5.000000 4.000000 a1.000000 1.000000 0 0 1 1.000000 -1.000000 Z"
    )
}

private val _skipNext: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("SkipNext",
        "M21 4v16",
        "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z"
    )
}

private val _skipPrevious: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("SkipPrevious",
        "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z",
        "M3 20V4"
    )
}

private val _close: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Close",
        "M18 6 6 18",
        "m6 6 12 12"
    )
}

private val _check: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Check",
        "M20 6 9 17l-5-5"
    )
}

private val _moreVert: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("MoreVert",
        "M11.000000 12.000000 a1.000000 1.000000 0 1 0 2.000000 0 a1.000000 1.000000 0 1 0 -2.000000 0 Z",
        "M11.000000 5.000000 a1.000000 1.000000 0 1 0 2.000000 0 a1.000000 1.000000 0 1 0 -2.000000 0 Z",
        "M11.000000 19.000000 a1.000000 1.000000 0 1 0 2.000000 0 a1.000000 1.000000 0 1 0 -2.000000 0 Z"
    )
}

private val _download: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Download",
        "M12 15V3",
        "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
        "m7 10 5 5 5-5"
    )
}

private val _favorite: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Favorite",
        "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"
    )
}

private val _search: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Search",
        "m21 21-4.34-4.34",
        "M3.000000 11.000000 a8.000000 8.000000 0 1 0 16.000000 0 a8.000000 8.000000 0 1 0 -16.000000 0 Z"
    )
}

private val _shuffle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Shuffle",
        "m18 14 4 4-4 4",
        "m18 2 4 4-4 4",
        "M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22",
        "M2 6h1.972a4 4 0 0 1 3.6 2.2",
        "M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45"
    )
}

private val _graphicEq: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("GraphicEq",
        "M2 10v3",
        "M6 6v11",
        "M10 3v18",
        "M14 8v7",
        "M18 5v13",
        "M22 10v3"
    )
}

private val _album: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Album",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M6 12c0-1.7.7-3.2 1.8-4.2",
        "M10.000000 12.000000 a2.000000 2.000000 0 1 0 4.000000 0 a2.000000 2.000000 0 1 0 -4.000000 0 Z",
        "M18 12c0 1.7-.7 3.2-1.8 4.2"
    )
}

private val _refresh: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Refresh",
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M8 16H3v5"
    )
}

private val _person: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Person",
        "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
        "M8.000000 7.000000 a4.000000 4.000000 0 1 0 8.000000 0 a4.000000 4.000000 0 1 0 -8.000000 0 Z"
    )
}

private val _checkCircle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("CheckCircle",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "m16 9-5.5 5.5L8 12"
    )
}

private val _musicNote: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("MusicNote",
        "M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19",
        "M15.000000 16.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z",
        "M3.000000 19.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z"
    )
}

private val _add: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Add",
        "M5 12h14",
        "M12 5v14"
    )
}

private val _libraryMusic: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("LibraryMusic",
        "M4.000000 3.000000 L10.000000 3.000000 a1.000000 1.000000 0 0 1 1.000000 1.000000 L11.000000 20.000000 a1.000000 1.000000 0 0 1 -1.000000 1.000000 L4.000000 21.000000 a1.000000 1.000000 0 0 1 -1.000000 -1.000000 L3.000000 4.000000 a1.000000 1.000000 0 0 1 1.000000 -1.000000 Z",
        "M7 3v18",
        "M20.4 18.9c.2.5-.1 1.1-.6 1.3l-1.9.7c-.5.2-1.1-.1-1.3-.6L11.1 5.1c-.2-.5.1-1.1.6-1.3l1.9-.7c.5-.2 1.1.1 1.3.6Z"
    )
}

private val _keyboardArrowDown: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("KeyboardArrowDown",
        "m6 9 6 6 6-6"
    )
}

private val _keyboardArrowUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("KeyboardArrowUp",
        "m18 15-6-6-6 6"
    )
}

private val _restartAlt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("RestartAlt",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
        "M3 3v5h5"
    )
}

private val _history: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("History",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
        "M3 3v5h5",
        "M12 7v5l4 2"
    )
}

private val _delete: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Delete",
        "M10 11v6",
        "M14 11v6",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
        "M3 6h18",
        "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
    )
}

private val _share: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Share",
        "M15.000000 5.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z",
        "M3.000000 12.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z",
        "M15.000000 19.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z",
        "M8.590000 13.510000 L15.420000 17.490000",
        "M15.410000 6.510000 L8.590000 10.490000"
    )
}

private val _repeat: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Repeat",
        "m17 2 4 4-4 4",
        "M3 11v-1a4 4 0 0 1 4-4h14",
        "m7 22-4-4 4-4",
        "M21 13v1a4 4 0 0 1-4 4H3"
    )
}

private val _repeatOne: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("RepeatOne",
        "m17 2 4 4-4 4",
        "M3 11v-1a4 4 0 0 1 4-4h14",
        "m7 22-4-4 4-4",
        "M21 13v1a4 4 0 0 1-4 4H3",
        "M11 10h1v4"
    )
}

private val _equalizer: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Equalizer",
        "M10 8h4",
        "M12 21v-9",
        "M12 8V3",
        "M17 16h4",
        "M19 12V3",
        "M19 21v-5",
        "M3 14h4",
        "M5 10V3",
        "M5 21v-7"
    )
}

private val _settings: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Settings",
        "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
        "M9.000000 12.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z"
    )
}

private val _tune: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Tune",
        "M10 5H3",
        "M12 19H3",
        "M14 3v4",
        "M16 17v4",
        "M21 12h-9",
        "M21 19h-5",
        "M21 5h-7",
        "M8 10v4",
        "M8 12H3"
    )
}

private val _star: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Star",
        "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"
    )
}

private val _radioButtonUnchecked: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("RadioButtonUnchecked",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z"
    )
}

private val _nightlight: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Nightlight",
        "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"
    )
}

private val _palette: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Palette",
        "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
        "M13.000000 6.500000 a0.500000 0.500000 0 1 0 1.000000 0 a0.500000 0.500000 0 1 0 -1.000000 0 Z",
        "M17.000000 10.500000 a0.500000 0.500000 0 1 0 1.000000 0 a0.500000 0.500000 0 1 0 -1.000000 0 Z",
        "M6.000000 12.500000 a0.500000 0.500000 0 1 0 1.000000 0 a0.500000 0.500000 0 1 0 -1.000000 0 Z",
        "M8.000000 7.500000 a0.500000 0.500000 0 1 0 1.000000 0 a0.500000 0.500000 0 1 0 -1.000000 0 Z"
    )
}

private val _lock: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Lock",
        "M5.000000 11.000000 L19.000000 11.000000 a2.000000 2.000000 0 0 1 2.000000 2.000000 L21.000000 20.000000 a2.000000 2.000000 0 0 1 -2.000000 2.000000 L5.000000 22.000000 a2.000000 2.000000 0 0 1 -2.000000 -2.000000 L3.000000 13.000000 a2.000000 2.000000 0 0 1 2.000000 -2.000000 Z",
        "M7 11V7a5 5 0 0 1 10 0v4"
    )
}

private val _headphones: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Headphones",
        "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3"
    )
}

private val _schedule: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Schedule",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M12 6v6l4 2"
    )
}

private val _pushPin: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("PushPin",
        "M12 17v5",
        "M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z"
    )
}

private val _public: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Public",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20",
        "M2 12h20"
    )
}

private val _accountCircle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("AccountCircle",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M9.000000 10.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z",
        "M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662"
    )
}

private val _info: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Info",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M12 16v-4",
        "M12 8h.01"
    )
}

private val _bolt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Bolt",
        "M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z"
    )
}

private val _mic: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Mic",
        "M12 19v3",
        "M19 10v2a7 7 0 0 1-14 0v-2",
        "M12.000000 2.000000 L12.000000 2.000000 a3.000000 3.000000 0 0 1 3.000000 3.000000 L15.000000 12.000000 a3.000000 3.000000 0 0 1 -3.000000 3.000000 L12.000000 15.000000 a3.000000 3.000000 0 0 1 -3.000000 -3.000000 L9.000000 5.000000 a3.000000 3.000000 0 0 1 3.000000 -3.000000 Z"
    )
}

private val _autoAwesome: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("AutoAwesome",
        "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
        "M20 2v4",
        "M22 4h-4",
        "M2.000000 20.000000 a2.000000 2.000000 0 1 0 4.000000 0 a2.000000 2.000000 0 1 0 -4.000000 0 Z"
    )
}

private val _arrowBack: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ArrowBack",
        "m12 19-7-7 7-7",
        "M19 12H5"
    )
}

private val _keyboardArrowRight: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("KeyboardArrowRight",
        "m9 18 6-6-6-6"
    )
}

private val _arrowForward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ArrowForward",
        "M5 12h14",
        "m12 5 7 7-7 7"
    )
}

private val _arrowUpward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ArrowUpward",
        "m5 12 7-7 7 7",
        "M12 19V5"
    )
}

private val _arrowDownward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ArrowDownward",
        "M12 5v14",
        "m19 12-7 7-7-7"
    )
}

private val _expandMore: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ExpandMore",
        "m6 9 6 6 6-6"
    )
}

private val _expandLess: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ExpandLess",
        "m18 15-6-6-6 6"
    )
}

private val _visibility: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Visibility",
        "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0",
        "M9.000000 12.000000 a3.000000 3.000000 0 1 0 6.000000 0 a3.000000 3.000000 0 1 0 -6.000000 0 Z"
    )
}

private val _visibilityOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("VisibilityOff",
        "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49",
        "M14.084 14.158a3 3 0 0 1-4.242-4.242",
        "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143",
        "m2 2 20 20"
    )
}

private val _addCircle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("AddCircle",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M8 12h8",
        "M12 8v8"
    )
}

private val _removeCircle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("RemoveCircle",
        "M2.000000 12.000000 a10.000000 10.000000 0 1 0 20.000000 0 a10.000000 10.000000 0 1 0 -20.000000 0 Z",
        "M8 12h8"
    )
}

private val _edit: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Edit",
        "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
        "m15 5 4 4"
    )
}

private val _contentCopy: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("ContentCopy",
        "M10.000000 8.000000 L20.000000 8.000000 a2.000000 2.000000 0 0 1 2.000000 2.000000 L22.000000 20.000000 a2.000000 2.000000 0 0 1 -2.000000 2.000000 L10.000000 22.000000 a2.000000 2.000000 0 0 1 -2.000000 -2.000000 L8.000000 10.000000 a2.000000 2.000000 0 0 1 2.000000 -2.000000 Z",
        "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
    )
}

private val _filterList: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("FilterList",
        "M2 5h20",
        "M6 12h12",
        "M9 19h6"
    )
}

private val _sort: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Sort",
        "m21 16-4 4-4-4",
        "M17 20V4",
        "m3 8 4-4 4 4",
        "M7 4v16"
    )
}

private val _fullscreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Fullscreen",
        "M8 3H5a2 2 0 0 0-2 2v3",
        "M21 8V5a2 2 0 0 0-2-2h-3",
        "M3 16v3a2 2 0 0 0 2 2h3",
        "M16 21h3a2 2 0 0 0 2-2v-3"
    )
}

private val _closeFullscreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("CloseFullscreen",
        "M8 3v3a2 2 0 0 1-2 2H3",
        "M21 8h-3a2 2 0 0 1-2-2V3",
        "M3 16h3a2 2 0 0 1 2 2v3",
        "M16 21v-3a2 2 0 0 1 2-2h3"
    )
}

val Icons.Rounded.PlayArrow: ImageVector get() = _playArrow
val Icons.Rounded.Pause: ImageVector get() = _pause
val Icons.Rounded.SkipNext: ImageVector get() = _skipNext
val Icons.Rounded.SkipPrevious: ImageVector get() = _skipPrevious
val Icons.Rounded.Close: ImageVector get() = _close
val Icons.Rounded.Check: ImageVector get() = _check
val Icons.Rounded.MoreVert: ImageVector get() = _moreVert
val Icons.Rounded.Download: ImageVector get() = _download
val Icons.Rounded.FavoriteBorder: ImageVector get() = _favorite
val Icons.Rounded.Search: ImageVector get() = _search
val Icons.Rounded.Shuffle: ImageVector get() = _shuffle
val Icons.Rounded.GraphicEq: ImageVector get() = _graphicEq
val Icons.Rounded.Album: ImageVector get() = _album
val Icons.Rounded.Refresh: ImageVector get() = _refresh
val Icons.Rounded.Person: ImageVector get() = _person
val Icons.Rounded.CheckCircle: ImageVector get() = _checkCircle
val Icons.Rounded.MusicNote: ImageVector get() = _musicNote
val Icons.Rounded.Add: ImageVector get() = _add
val Icons.Rounded.LibraryMusic: ImageVector get() = _libraryMusic
val Icons.Rounded.KeyboardArrowDown: ImageVector get() = _keyboardArrowDown
val Icons.Rounded.KeyboardArrowUp: ImageVector get() = _keyboardArrowUp
val Icons.Rounded.RestartAlt: ImageVector get() = _restartAlt
val Icons.Rounded.History: ImageVector get() = _history
val Icons.Rounded.Delete: ImageVector get() = _delete
val Icons.Rounded.Share: ImageVector get() = _share
val Icons.Rounded.Repeat: ImageVector get() = _repeat
val Icons.Rounded.RepeatOne: ImageVector get() = _repeatOne
val Icons.Rounded.Equalizer: ImageVector get() = _equalizer
val Icons.Rounded.Settings: ImageVector get() = _settings
val Icons.Rounded.Tune: ImageVector get() = _tune
val Icons.Rounded.Star: ImageVector get() = _star
val Icons.Rounded.RadioButtonUnchecked: ImageVector get() = _radioButtonUnchecked
val Icons.Rounded.Nightlight: ImageVector get() = _nightlight
val Icons.Rounded.Palette: ImageVector get() = _palette
val Icons.Rounded.Lock: ImageVector get() = _lock
val Icons.Rounded.Headphones: ImageVector get() = _headphones
val Icons.Rounded.Schedule: ImageVector get() = _schedule
val Icons.Rounded.PushPin: ImageVector get() = _pushPin
val Icons.Rounded.Public: ImageVector get() = _public
val Icons.Rounded.AccountCircle: ImageVector get() = _accountCircle
val Icons.Rounded.Info: ImageVector get() = _info
val Icons.Rounded.Bolt: ImageVector get() = _bolt
val Icons.Rounded.Mic: ImageVector get() = _mic
val Icons.Rounded.AutoAwesome: ImageVector get() = _autoAwesome
val Icons.Rounded.ArrowBack: ImageVector get() = _arrowBack
val Icons.Rounded.KeyboardArrowRight: ImageVector get() = _keyboardArrowRight
val Icons.Rounded.ArrowForward: ImageVector get() = _arrowForward
val Icons.Rounded.ArrowUpward: ImageVector get() = _arrowUpward
val Icons.Rounded.ArrowDownward: ImageVector get() = _arrowDownward
val Icons.Rounded.ExpandMore: ImageVector get() = _expandMore
val Icons.Rounded.ExpandLess: ImageVector get() = _expandLess
val Icons.Rounded.Visibility: ImageVector get() = _visibility
val Icons.Rounded.VisibilityOff: ImageVector get() = _visibilityOff
val Icons.Rounded.AddCircle: ImageVector get() = _addCircle
val Icons.Rounded.RemoveCircle: ImageVector get() = _removeCircle
val Icons.Rounded.Edit: ImageVector get() = _edit
val Icons.Rounded.ContentCopy: ImageVector get() = _contentCopy
val Icons.Rounded.FilterList: ImageVector get() = _filterList
val Icons.Rounded.Sort: ImageVector get() = _sort
val Icons.Rounded.Fullscreen: ImageVector get() = _fullscreen
val Icons.Rounded.CloseFullscreen: ImageVector get() = _closeFullscreen

/* Solid heart for the saved state; the outlined heart above is the empty state. */
private val _favoriteFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    glyph("Favorite",
        "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z",
        filled = true
    )
}

val Icons.Rounded.Favorite: ImageVector get() = _favoriteFilled
