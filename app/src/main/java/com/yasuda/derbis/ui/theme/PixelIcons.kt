package com.yasuda.derbis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object PixelIcons {
    // Stepped 16x16 / 24x24 pixel-grid filled glyphs — zero curves, zero round caps.
    val Play = pixelFill(
        "Play",
        "M6,4h3v2h3v2h3v2h3v4h-3v2h-3v2h-3v2H6V4z"
    )
    val Pause = pixelFill(
        "Pause",
        "M6,4h4v16H6V4z M14,4h4v16h-4V4z"
    )
    val Previous = pixelFill(
        "Previous",
        "M5,5h3v14H5V5z M17,5h2v14h-2v-2h-3v-2h-3v-2H9v-2h2V9h3V7h3V5z"
    )
    val Next = pixelFill(
        "Next",
        "M16,5h3v14h-3V5z M5,5h2v2h3v2h3v2h2v2h-2v2h-3v2H7v2H5V5z"
    )
    val Shuffle = pixelFill(
        "Shuffle",
        "M3,6h5v2h2v2H8V8H3V6z M14,6h3V4h2v2h2v2h-2v2h-2V8h-3V6z M10,10h4v4h-4v-4z M3,16h5v-2h2v2H8v2H3v-2z M14,16h3v-2h2v2h2v2h-2v2h-2v-2h-3v-2z"
    )
    val Repeat = pixelFill(
        "Repeat",
        "M5,6h11V4h2v2h2v2h-2v2h-2V8H7v4H5V6z M17,12h2v6H8v2H6v-2H4v-2h2v-2h2v2h9v-4z"
    )
    val RepeatOne = pixelFill(
        "RepeatOne",
        "M5,6h11V4h2v2h2v2h-2v2h-2V8H7v4H5V6z M17,12h2v6H8v2H6v-2H4v-2h2v-2h2v2h9v-4z M11,9h2v6h-2v-4h-1V9h1z"
    )
    val Heart = pixelFill(
        "Heart",
        "M5,4h4v2h2v2h2V6h2V4h4v2h2v6h-2v2h-2v2h-2v2h-2v2h-2v-2H9v-2H7v-2H5v-2H3V6h2V4z"
    )
    val HeartBorder = pixelFill(
        "HeartBorder",
        "M5,4h4v2h2v2h2V6h2V4h4v2h2v6h-2v2h-2v2h-2v2h-2v2h-2v-2H9v-2H7v-2H5v-2H3V6h2V4z M5,6v6h2v2h2v2h2v2h2v-2h2v-2h2v-2h2V6h-4v2h-2v2h-2V8H9V6H5z",
        evenOdd = true
    )
    val Queue = pixelFill(
        "Queue",
        "M3,5h14v2H3V5z M3,10h14v2H3v-2z M3,15h9v2H3v-2z M15,13h2v2h2v2h-2v2h-2v-6z"
    )
    val Record = pixelFill(
        "Record",
        "M7,3h10v2h2v2h2v10h-2v2h-2v2H7v-2H5v-2H3V7h2V5h2V3z M7,5v2H5v10h2v2h10v-2h2V7h-2V5H7z M10,10h4v4h-4v-4z",
        evenOdd = true
    )
    val Note = pixelFill(
        "Note",
        "M9,4h11v4h-9v10H5v-4h4V4z M18,8h2v8h-6v-4h4V8z"
    )
    val Artist = pixelFill(
        "Artist",
        "M9,3h6v2h2v4h-2v2H9V9H7V5h2V3z M5,15h14v2h2v4H3v-4h2v-2z"
    )
    val Download = pixelFill(
        "Download",
        "M11,3h2v8h3v2h-2v2h-4v-2H8v-2h3V3z M4,17h2v2h12v-2h2v4H4v-4z"
    )
    val Home = pixelFill(
        "Home",
        "M11,3h2v2h3v2h3v2h2v2h-2v9h-5v-6h-4v6H5v-9H3V9h2V7h3V5h3V3z"
    )
    val Search = pixelFill(
        "Search",
        "M6,3h8v2h2v2h2v6h-2v2h2v2h2v2h-2v2h-2v-2h-2v-2h-6v-2H4V7h2V5h0V3z M6,7v6h8V7H6z",
        evenOdd = true
    )
    val Explore = pixelFill(
        "Explore",
        "M7,3h10v2h2v2h2v10h-2v2h-2v2H7v-2H5v-2H3V7h2V5h2V3z M7,5v2H5v10h2v2h10v-2h2V7h-2V5H7z M13,8h3v3h-2v2h-2v2H9v-3h2v-2h2V8z",
        evenOdd = true
    )
    val Library = pixelFill(
        "Library",
        "M3,4h2v14h15v2H3V4z M7,4h13v12H7V4z M9,6v8h9V6H9z M12,8h3v4h-3V8z",
        evenOdd = true
    )
    val Settings = pixelFill(
        "Settings",
        "M10,3h4v2h3v2h2v3h2v4h-2v3h-2v2h-3v2h-4v-2H7v-2H5v-3H3v-4h2V7h2V5h3V3z M9,9v6h6V9H9z",
        evenOdd = true
    )
    val MoreHoriz = pixelFill(
        "MoreHoriz",
        "M4,10h4v4H4v-4z M10,10h4v4h-4v-4z M16,10h4v4h-4v-4z"
    )
    val MoreVert = pixelFill(
        "MoreVert",
        "M10,4h4v4h-4V4z M10,10h4v4h-4v-4z M10,16h4v4h-4v-4z"
    )
    val Back = pixelFill(
        "Back",
        "M11,5h2v2H9v2H7v2h13v2H7v2h2v2h4v2h-2v-2H7v-2H5v-2H3v-2h2V9h2V7h4V5z"
    )

    fun resolve(icon: ImageVector): ImageVector = when (icon.name.substringAfterLast('.')) {
        "ArrowBack" -> Back
        "PlayArrow" -> Play
        "Pause" -> Pause
        "SkipPrevious" -> Previous
        "SkipNext" -> Next
        "Shuffle" -> Shuffle
        "Repeat" -> Repeat
        "RepeatOne" -> RepeatOne
        "Favorite" -> Heart
        "FavoriteBorder" -> HeartBorder
        "QueueMusic", "PlaylistPlay" -> Queue
        "Album" -> Record
        "MusicNote" -> Note
        "Mic" -> Artist
        "Download", "DownloadDone" -> Download
        "Home" -> Home
        "Search" -> Search
        "Explore" -> Explore
        "LibraryMusic" -> Library
        "Settings" -> Settings
        "MoreHoriz" -> MoreHoriz
        "MoreVert" -> MoreVert
        else -> icon
    }

    private fun pixelFill(name: String, path: String, evenOdd: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = "Pixel.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.White)
        ).build()
}
