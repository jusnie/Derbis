package com.yasuda.derbis.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.domain.PixelCharmChoice
import com.yasuda.derbis.domain.PixelMascotHead
import com.yasuda.derbis.feature.icon.DerbisAppIcon
import com.yasuda.derbis.ui.components.DerbisVectorLogo
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class PixelCharmKind {
    Bow,
    Star,
    Heart
}

internal enum class PixelMascotKind(val id: String, val body: Color) {
    Bear("bear", Color(0xFFE3A76B)),
    Bunny("bunny", Color(0xFFFFF8F6)),
    Cat("cat", Color(0xFFFFC2D3)),
    Chick("chick", Color(0xFFFFD98A)),
    Frog("frog", Color(0xFF9FE8C8)),
    Penguin("penguin", Color(0xFFA9C7E0)),
    Sheep("sheep", Color(0xFFFFEDF1)),
    Cloud("cloud", Color(0xFFC9B6FF));

    companion object {
        fun fromId(id: String?): PixelMascotKind? =
            entries.firstOrNull { it.id == id }
    }
}

internal fun resolvePixelCharm(choice: PixelCharmChoice, trackId: String): PixelCharmKind? = when (choice) {
    PixelCharmChoice.Bow -> PixelCharmKind.Bow
    PixelCharmChoice.Star -> PixelCharmKind.Star
    PixelCharmChoice.Heart -> PixelCharmKind.Heart
    PixelCharmChoice.None -> null
    PixelCharmChoice.Auto -> {
        val entries = PixelCharmKind.entries
        if (trackId.isEmpty()) PixelCharmKind.Bow
        else entries[abs(trackId.hashCode()) % entries.size]
    }
}

// ---------------------------------------------------------------------------
// 1. Pixel Derbis Logo (21 x 28 grid resampled from derbis_logo.png)
// ---------------------------------------------------------------------------
private val LogoInk = Color(0xFFA84765)
private val LogoPlum = Color(0xFF5B2338)
private val LogoFill = Color(0xFFFF5C8A)
private val LogoLight = Color(0xFFFFC2D3)
private val LogoSpark = Color(0xFFFFF8F6)

private val PixelLogoRows = arrayOf(
    "......o..............",
    ".....oPo.............",
    "....oPPPo............",
    "...oLLLPPo...........",
    "..oLLLPPPPo..........",
    ".oPLLPPPPPPo.........",
    "oPPLPPwPPPPo.........",
    ".oPPPPPPPPo..........",
    "..oPPPPPPo....o......",
    "...oPPPPo....oPo.....",
    "....oPPo....oPPPo....",
    ".....oo....oLLLPPo...",
    "..........oPLLPPPPo..",
    ".........oPPLPPPPPPo.",
    ".........oPPPPwPPPPPo",
    "..........oPPPPPPPPo.",
    "......o....oPPPPPPo..",
    "....ooPo....oPPPPo...",
    "...oPPPPo....oPPo....",
    "..oLLLLPPo....oo.....",
    ".oPLLLPPPPo..........",
    "oPPLLPwPPPPo.........",
    ".oPLPPPPPPPo.........",
    "..oPPPPPPPo..........",
    "...oPPPPPo...........",
    "....oPPPo............",
    ".....oPo.............",
    "......o.............."
)

private fun pixelLogoColor(ch: Char, plumOutline: Boolean = false): Color? = when (ch) {
    'o' -> if (plumOutline) LogoPlum else LogoInk
    'P' -> LogoFill
    'L' -> if (plumOutline) Color(0xFFFFB3C9) else LogoLight
    'w' -> LogoSpark
    else -> null
}

/**
 * Draws the 21x28 pixelated Derbis 3-diamond mark on a fully transparent ground (alpha 0).
 * Every cell is snapped to square integer coordinates so edges stay razor-sharp at any size.
 */
@Composable
internal fun PixelDerbisLogoMark(
    size: Dp = 28.dp,
    contentDescription: String? = "Derbis",
    modifier: Modifier = Modifier
) {
    val sem = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier = modifier.size(size).then(sem)) {
        val cols = 21
        val rows = 28
        val cell = floor(minOf(this.size.width / cols, this.size.height / rows)).coerceAtLeast(1f)
        val totalW = cell * cols
        val totalH = cell * rows
        val ox = floor((this.size.width - totalW) / 2f)
        val oy = floor((this.size.height - totalH) / 2f)

        for (y in 0 until rows) {
            val row = PixelLogoRows[y]
            var x = 0
            while (x < cols) {
                val col = pixelLogoColor(row[x])
                val start = x
                while (x < cols && pixelLogoColor(row[x]) == col) x++
                if (col != null) {
                    drawRect(
                        color = col,
                        topLeft = Offset(ox + start * cell, oy + y * cell),
                        size = Size((x - start) * cell, cell)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Pixel Charm Banners (bow, star, heart — transparent outside band & charm)
// ---------------------------------------------------------------------------
private val BandEdge = Color(0xFFFFF0F4)
private val BandHair = Color(0xFFC9748D)
private val BandStripeA = Color(0xFFFFD9E2)
private val BandStripeB = Color(0xFFFFC2D3)
private val BandPausedA = Color(0xFFFFE6EC)
private val BandPausedB = Color(0xFFFFD9E2)

private val CharmOutline = Color(0xFFA84765)
private val CharmPink = Color(0xFFFF5C8A)
private val CharmKnot = Color(0xFF7A2C46)
private val CharmButter = Color(0xFFFFD98A)
private val CharmCream = Color(0xFFFFF8F6)

private val BowGrid = arrayOf(
    ".................",
    "..ooo.......ooo..",
    ".owwPo.....oPwwo.",
    "owPPPPo...oPPPPwo",
    "oPPPPPPo.oPPPPPPo",
    "oPPPPPPPkPPPPPPPo",
    "oPPPPPPo.oPPPPPPo",
    "owPPPPo...oPPPPwo",
    ".owwPo.....oPwwo.",
    "..ooo.......ooo.."
)

private val StarGrid = arrayOf(
    ".....o.....",
    "....oyo....",
    "....oyo....",
    ".oooyoyooo.",
    ".oyyyyyyyo.",
    "..oyyyyyo..",
    "...oyyyo...",
    "..oyyoyyo..",
    "..oyo.oyo.."
)

private val HeartGrid = arrayOf(
    "..oo...oo..",
    ".owPo.oPPo.",
    "owwPPoPPPo.",
    "owPPPPPPPPo",
    "oPPPPPPPPPo",
    ".oPPPPPPo..",
    "..oPPPPo...",
    "...oPPo....",
    "....oo....."
)

private fun charmPixelColor(ch: Char): Color? = when (ch) {
    'o' -> CharmOutline
    'P' -> CharmPink
    'k' -> CharmKnot
    'y' -> CharmButter
    'w' -> CharmCream
    else -> null
}

/**
 * Transparent Pixel charm ribbon across the mini player (preview/pixel-banners.html).
 * Outside the 14-cell striped band and the 2x charm sprite, alpha is 0 (no white plate).
 */
@Composable
internal fun PixelCharmBanner(
    charm: PixelCharmKind,
    paused: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 24 vertical cells at ~1dp each -> 24dp total canvas, 14-cell band centered vertically
    // so the 2x charm overhangs cleanly above and below the striped ribbon on alpha 0.
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val totalRows = 24
        val bandTop = 5
        val bandBottom = 18
        val cell = max(1f, (this.size.height / totalRows).roundToInt().toFloat())
        val cols = max(1, floor(this.size.width / cell).toInt())
        val stripeCells = 4
        val stripeA = if (paused) BandPausedA else BandStripeA
        val stripeB = if (paused) BandPausedB else BandStripeB

        // Draw the 14-row ribbon band across the full width; rows outside [bandTop..bandBottom]
        // stay completely untouched (alpha 0).
        for (ry in bandTop..bandBottom) {
            val topY = ry * cell
            when (ry) {
                bandTop, bandBottom -> {
                    drawRect(color = BandEdge, topLeft = Offset(0f, topY), size = Size(this.size.width, cell))
                }
                bandTop + 1, bandBottom - 1 -> {
                    drawRect(color = BandHair, topLeft = Offset(0f, topY), size = Size(this.size.width, cell))
                }
                else -> {
                    var x = 0
                    while (x < cols) {
                        val even = (x / stripeCells) % 2 == 0
                        val end = minOf(cols, ((x / stripeCells) + 1) * stripeCells)
                        val startPx = x * cell
                        val widthPx = if (end == cols) this.size.width - startPx else (end - x) * cell
                        drawRect(
                            color = if (even) stripeA else stripeB,
                            topLeft = Offset(startPx, topY),
                            size = Size(widthPx, cell)
                        )
                        x = end
                    }
                }
            }
        }

        // Draw the 2x scaled charm directly onto the stripes with alpha 0 outside the sprite.
        val sprite = when (charm) {
            PixelCharmKind.Bow -> BowGrid
            PixelCharmKind.Star -> StarGrid
            PixelCharmKind.Heart -> HeartGrid
        }
        val factor = 2
        val gw = sprite[0].length * factor
        val gh = sprite.size * factor
        val leftCell = (cols - gw) / 2
        val topCell = (bandTop + bandBottom) / 2 + 1 - gh / 2

        for (sy in sprite.indices) {
            val row = sprite[sy]
            var sx = 0
            while (sx < row.length) {
                val col = charmPixelColor(row[sx])
                val start = sx
                while (sx < row.length && charmPixelColor(row[sx]) == col) sx++
                if (col != null) {
                    val drawX = (leftCell + start * factor) * cell
                    val drawY = (topCell + sy * factor) * cell
                    val drawW = (sx - start) * factor * cell
                    val drawH = factor * cell
                    drawRect(
                        color = col,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, drawH)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Original Derbis Mascots (16 x 16 grids from preview/pixel-mascots/generate.py)
// ---------------------------------------------------------------------------
private val MascotInk = Color(0xFF5B2338)
private val MascotCheek = Color(0xFFFF8FB0)
private val MascotEar = Color(0xFFFFD9E2)
private val MascotBeak = Color(0xFFE08A2E)
private val MascotCream = Color(0xFFFFF8F6)

private fun mascotRows(kind: PixelMascotKind): Array<String> = when (kind) {
    PixelMascotKind.Bear -> arrayOf(
        "................",
        "....##....##....",
        "...#BB#..#BB#...",
        "..#BBBB##BBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Bunny -> arrayOf(
        "...#......#.....",
        "...#B#....#B#...",
        "...#BI#..#IB#...",
        "...#BI#..#IB#...",
        "...#BB#..#BB#...",
        "..#BBBB##BBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Cat -> arrayOf(
        "................",
        "..#..........#..",
        "..#B#........#B#",
        "..#BB#......#BB#",
        "..#BIB#....#BIB#",
        "..#BBBB####BBBB#",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Chick -> arrayOf(
        "................",
        ".......##.......",
        "......#BB#......",
        "...#BBBBBBBB#...",
        "..#BBBBBBBBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBBNNBBBBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBNNBBBCC",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Frog -> arrayOf(
        "................",
        "..###......###..",
        ".#BBB#....#BBB#.",
        ".#B#BB#..#BB#B#.",
        ".#BBB######BBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Penguin -> arrayOf(
        "................",
        "....########....",
        "...#BBBBBBBB#...",
        "..#BBBBBBBBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBBNNBBBBBB#",
        ".#BCCBBBKBBBBBCC",
        ".#BCCBBBBBBBBBCC",
        "..#BBBKKKKKKBBB#",
        "..#BBBKKKKKKBBB#",
        "...#BBBKKKKBBB#.",
        "....##########..",
        "................"
    )
    PixelMascotKind.Sheep -> arrayOf(
        "................",
        "...####..####...",
        "..#BBBB##BBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "....##########.."
    )
    PixelMascotKind.Cloud -> arrayOf(
        "................",
        "......####......",
        "....##BBBB##....",
        "...#BBBBBBBB#...",
        "..#BBBBBBBBBB#..",
        ".#BBBBBBBBBBBB#.",
        ".#BBBBBBBBBBBB#.",
        ".#BB##BBBBBB##B#",
        ".#BB##BBBBBB##B#",
        ".#BBBBB####BBBB#",
        ".#BCCBB#BB#BBCC#",
        ".#BCCBBBBBBBBCC#",
        "..#BBB#BBBB#BBB#",
        "..#BBBBBBBBBBBB#",
        "...#BBBBBBBBBB#.",
        "...##########..."
    )
}

private fun mascotCellColor(ch: Char, body: Color, alpha: Float = 1f): Color? {
    val base = when (ch) {
        '#' -> MascotInk
        'B' -> body
        'C' -> MascotCheek
        'I' -> MascotEar
        'N' -> MascotBeak
        'K' -> MascotCream
        else -> null
    }
    return base?.let { if (alpha >= 0.999f) it else it.copy(alpha = alpha) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMascotAt(
    kind: PixelMascotKind,
    ox: Float,
    oy: Float,
    cell: Float,
    alpha: Float = 1f
) {
    val rows = mascotRows(kind)
    val body = kind.body
    for (y in 0 until 16) {
        val row = rows[y]
        var x = 0
        while (x < 16) {
            val col = mascotCellColor(row[x], body, alpha)
            val start = x
            while (x < 16 && mascotCellColor(row[x], body, alpha) == col) x++
            if (col != null) {
                drawRect(
                    color = col,
                    topLeft = Offset(ox + start * cell, oy + y * cell),
                    size = Size((x - start) * cell, cell)
                )
            }
        }
    }
}

@Composable
internal fun PixelMascotSprite(
    mascot: PixelMascotKind,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val cell = floor(minOf(this.size.width, this.size.height) / 16f).coerceAtLeast(1f)
        val side = cell * 16f
        val ox = floor((this.size.width - side) / 2f)
        val oy = floor((this.size.height - side) / 2f)
        drawMascotAt(mascot, ox, oy, cell)
    }
}

/**
 * 4 x 2 sheet of all eight mascots (70 x 34 cells, ~2.06:1 aspect) for the Studio picker
 * so the "All eight" tile preserves 1:1 head proportions without squashing.
 */
@Composable
internal fun PixelMascotSheetPreview(
    height: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val cols = 4
    val rows = 2
    val gapCells = 2
    val gridW = cols * 16 + (cols - 1) * gapCells // 70
    val gridH = rows * 16 + (rows - 1) * gapCells // 34
    val width = height * (gridW.toFloat() / gridH.toFloat())
    val ordered = listOf(
        PixelMascotKind.Bear,
        PixelMascotKind.Bunny,
        PixelMascotKind.Cat,
        PixelMascotKind.Chick,
        PixelMascotKind.Frog,
        PixelMascotKind.Penguin,
        PixelMascotKind.Sheep,
        PixelMascotKind.Cloud
    )
    Canvas(modifier = modifier.width(width).height(height)) {
        val cell = floor(minOf(this.size.width / gridW, this.size.height / gridH)).coerceAtLeast(1f)
        val ox = floor((this.size.width - gridW * cell) / 2f)
        val oy = floor((this.size.height - gridH * cell) / 2f)
        ordered.forEachIndexed { index, kind ->
            val col = index % cols
            val row = index / cols
            drawMascotAt(
                kind = kind,
                ox = ox + col * (16 + gapCells) * cell,
                oy = oy + row * (16 + gapCells) * cell,
                cell = cell
            )
        }
    }
}

/**
 * Repeating mascot tile backdrop behind the Pixel mini player (preview/pixel-mascots.html).
 * Opacity follows [opacityPercent] (10..100 -> 0.10f..0.34f) so track text stays readable.
 */
@Composable
internal fun PixelMascotTiledBackdrop(
    head: PixelMascotHead,
    opacityPercent: Int,
    modifier: Modifier = Modifier
) {
    val family = listOf(
        PixelMascotKind.Bear,
        PixelMascotKind.Bunny,
        PixelMascotKind.Cat,
        PixelMascotKind.Chick,
        PixelMascotKind.Frog,
        PixelMascotKind.Penguin,
        PixelMascotKind.Sheep,
        PixelMascotKind.Cloud
    )
    val single = PixelMascotKind.fromId(head.storageId)
    // Map studio opacity (10..100) into the preview's watermark band (12%..32%; 100 -> 30%).
    val normalized = ((opacityPercent.coerceIn(10, 100) - 10) / 90f)
    val baseAlpha = if (single != null) {
        0.12f + normalized * 0.14f // single head: up to ~26% (preview shows 22%)
    } else {
        0.12f + normalized * 0.18f // all eight: up to ~30% (preview shows 16%..30%)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val targetSpriteDp = if (single != null) 28.dp else 22.dp
        val cell = max(1f, (targetSpriteDp.toPx() / 16f).roundToInt().toFloat())
        val step = cell * 18f // 16-cell sprite + 2-cell gap
        val cols = (this.size.width / step).toInt() + 2
        val rows = (this.size.height / step).toInt() + 2
        for (ry in 0 until rows) {
            for (cx in 0 until cols) {
                val kind = single ?: family[(ry * 3 + cx) % family.size]
                val xOffset = if (ry % 2 == 1 && single != null) step * 0.5f else 0f
                drawMascotAt(
                    kind = kind,
                    ox = cx * step - xOffset,
                    oy = ry * step,
                    cell = cell,
                    alpha = baseAlpha
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. 32 x 32 App Icon Tile Preview (glyph/mascot + DERBIS wordmark on blush plate)
// ---------------------------------------------------------------------------
private val WordmarkRows = arrayOf(
    "##..###.##..##..###..##",
    "#.#.#...#.#.#.#..#..#..",
    "#.#.##..##..##...#...#.",
    "#.#.#...#.#.#.#..#....#",
    "##..###.#.#.##..###.##."
)

@Composable
internal fun PixelAppIconTilePreview(
    icon: DerbisAppIcon,
    size: Dp = 78.dp,
    modifier: Modifier = Modifier
) {
    if (icon == DerbisAppIcon.Original) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Black, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            DerbisVectorLogo(size = size * 0.72f, tint = Color.White)
        }
        return
    }

    val mascot = PixelMascotKind.fromId(icon.mascotId)
    Canvas(
        modifier = modifier
            .size(size)
            .background(Color(0xFFFFEDF1), RectangleShape)
    ) {
        val grid = 32
        val cell = floor(minOf(this.size.width, this.size.height) / grid).coerceAtLeast(1f)
        val ox = floor((this.size.width - grid * cell) / 2f)
        val oy = floor((this.size.height - grid * cell) / 2f)

        if (mascot != null) {
            drawMascotAt(mascot, ox + 8f * cell, oy + 1f * cell, cell)
            drawWordmarkAt(ox, oy + 25f * cell, cell)
        } else {
            // Pixelated Derbis mark (21x28 cropped/centered inside top 24 rows above wordmark)
            for (y in 2 until 26) {
                val srcY = ((y - 2) * 28) / 24
                val row = PixelLogoRows[srcY]
                for (x in 0 until 21) {
                    val col = pixelLogoColor(row[x], plumOutline = true) ?: continue
                    val gx = 5 + x
                    if (gx in 0 until 32) {
                        drawRect(
                            color = col,
                            topLeft = Offset(ox + gx * cell, oy + y * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
            drawWordmarkAt(ox, oy + 26f * cell, cell)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWordmarkAt(
    ox: Float,
    topY: Float,
    cell: Float
) {
    val wordW = WordmarkRows[0].length // 23 cells
    val startX = ((32 - wordW) / 2) * cell
    for (wy in WordmarkRows.indices) {
        val row = WordmarkRows[wy]
        var wx = 0
        while (wx < wordW) {
            val on = row[wx] == '#'
            val start = wx
            while (wx < wordW && (row[wx] == '#') == on) wx++
            if (on) {
                drawRect(
                    color = MascotInk,
                    topLeft = Offset(ox + startX + start * cell, topY + wy * cell),
                    size = Size((wx - start) * cell, cell)
                )
            }
        }
    }
}
