#!/usr/bin/env python3
"""
Builds the Pixel skin's mascot heads.

Eight original cute animal heads, drawn here as 16 x 16 character grids. They are
Derbis's own designs — no third-party character art is read, traced or shipped.
Everything is generated from the tables below, so the sprite sheet is reproducible
and the repo carries no binary art it cannot regenerate.

Grammar shared by all eight, so a row of them reads as one family:
  - one ink outline, two cells thick at the silhouette only
  - two eyes, two cells wide, on the same rows
  - one blush dab under each eye, on the same rows
  - a two-cell mouth/snout mark, one cell below the blush
  - a transparent 1-cell margin all round, so tiling never clips a silhouette

Outputs (alpha 0 outside the silhouette, never a white plate):
  <name>.png / <name>.svg   one per mascot, 16 x 16 cells at SCALE
  sheet.png                 all eight in a row, for the preview page

Run from the repo root:  python3 preview/pixel-mascots/generate.py
"""

import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))

SIZE = 16
SCALE = 10

# One grammar for the whole family, one body tone per mascot so a row of them reads
# apart at 16 px. Tokens are roles, not colours: the per-mascot body tone is the
# only thing that varies.
INK = "#5B2338"        # outline, eyes, mouth
CHEEK = "#FF8FB0"      # the blush dab under each eye
EAR = "#FFD9E2"        # inner ear / wing detail
BEAK = "#E08A2E"       # beak, nose
CREAM = "#FFF8F6"      # catchlight, belly, face patch

BODIES = {
    "bear": "#E3A76B",     # caramel
    "bunny": "#FFF8F6",    # cream
    "cat": "#FFC2D3",      # blush
    "chick": "#FFD98A",    # butter
    "frog": "#9FE8C8",     # mint
    "penguin": "#A9C7E0",  # sky
    "sheep": "#FFEDF1",    # pale
    "cloud": "#C9B6FF",    # lilac
}

TOKENS = {"#": INK, "B": None, "C": CHEEK, "I": EAR, "N": BEAK, "K": CREAM}

# ---------------------------------------------------------------------------
# The sprites. Every string must be exactly SIZE characters.
# ---------------------------------------------------------------------------
MASCOTS = {
    "bear": [
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
        "....##########..",
    ],
    "bunny": [
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
        "....##########..",
    ],
    "cat": [
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
        "....##########..",
    ],
    "chick": [
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
        "....##########..",
    ],
    "frog": [
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
        "....##########..",
    ],
    "penguin": [
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
        "................",
    ],
    "sheep": [
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
        "....##########..",
    ],
    "cloud": [
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
        "...##########...",
    ],
}

# ---------------------------------------------------------------------------
# App-icon variants. 32 x 32 cells: the mark (or a glyph) on a blush plate, the
# wordmark underneath. The launcher alias picks between these in the app.
# ---------------------------------------------------------------------------
ICON = 32

# 3 x 5 capitals: the smallest face that still reads under a 16-cell head
FONT = {
    "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"],
    "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "B": ["##.", "#.#", "##.", "#.#", "##."],
    "I": ["###", ".#.", ".#.", ".#.", "###"],
    "S": [".##", "#..", ".#.", "..#", "##."],
}
WORD_ROWS = 5


def wordmark(text):
    glyphs = [FONT[char] for char in text]
    width = sum(len(g[0]) for g in glyphs) + (len(glyphs) - 1)
    cells = [["."] * width for _ in range(WORD_ROWS)]
    x = 0
    for index, glyph in enumerate(glyphs):
        if index:
            x += 1
        for row in range(WORD_ROWS):
            for offset, cell in enumerate(glyph[row]):
                cells[row][x + offset] = cell
        x += len(glyph[0])
    return ["".join(row) for row in cells]


def digit_rows(letter):
    return wordmark(letter)

# ---------------------------------------------------------------------------
# png / svg writers (no third-party imaging library is available here)
# ---------------------------------------------------------------------------
def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def write_png(path, width, height, canvas, scale):
    """canvas is a 2-D list of hex strings or None."""

    def pixel(x, y):
        colour = canvas[y][x]
        if colour is None:
            return b"\x00\x00\x00\x00"
        r, g, b = hex_rgb(colour)
        return bytes((r, g, b, 255))

    out_rows = []
    for y in range(height):
        wide = bytearray()
        for x in range(width):
            wide += pixel(x, y) * scale
        for _ in range(scale):
            out_rows.append(bytes(wide))

    raw = b"".join(b"\x00" + row for row in out_rows)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width * scale, height * scale, 8, 6, 0, 0, 0)
    with open(path, "wb") as handle:
        handle.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) +
                     chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def row_runs(row):
    runs, x, width = [], 0, len(row)
    while x < width:
        colour = row[x]
        start = x
        while x < width and row[x] == colour:
            x += 1
        if colour is not None:
            runs.append((start, x - start, colour))
    return runs


def write_svg(path, canvas):
    rows, columns = len(canvas), len(canvas[0])
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" '
             'height="%d" shape-rendering="crispEdges" role="img">'
             % (columns, rows, columns * 4, rows * 4)]
    for y, row in enumerate(canvas):
        for start, length, colour in row_runs(row):
            parts.append('<rect x="%d" y="%d" width="%d" height="1" fill="%s"/>'
                         % (start, y, length, colour))
    parts.append("</svg>")
    open(path, "w", encoding="utf-8").write("\n".join(parts))


def validate(name, rows):
    if len(rows) != SIZE:
        raise SystemExit("%s: %d rows, expected %d" % (name, len(rows), SIZE))
    for index, row in enumerate(rows):
        if len(row) != SIZE:
            raise SystemExit("%s: row %d is %d cells, expected %d — %r"
                             % (name, index, len(row), SIZE, row))
        for cell in row:
            if cell != "." and cell not in TOKENS:
                raise SystemExit("%s: row %d has unknown cell %r" % (name, index, cell))


def canvas_of(rows, body):
    out = []
    for row in rows:
        out.append([None if cell == "." else (body if cell == "B" else TOKENS[cell])
                    for cell in row])
    return out


def preview(rows):
    glyphs = {"#": "#", "B": "o", "C": "+", "I": "i", "N": "n", "K": "k"}
    for row in rows:
        print("  " + "".join(glyphs.get(c, ".") for c in row))


def main():
    os.makedirs(HERE, exist_ok=True)
    names = sorted(MASCOTS)
    for name in names:
        rows = MASCOTS[name]
        validate(name, rows)
        canvas = canvas_of(rows, BODIES[name])
        write_png(os.path.join(HERE, name + ".png"), SIZE, SIZE, canvas, SCALE)
        write_svg(os.path.join(HERE, name + ".svg"), canvas)
        print(name)
        preview(rows)
        print()

    # Two composed images, because they are consumed differently and a strip cannot
    # serve both: the preview draws them in a square slot, and the mini player tiles
    # them as a repeating background. A single 8-across strip is 8.4:1, so any square
    # slot squashes all eight heads into a band — they read as too narrow and too close
    # together. Both blocks below are laid out on a grid with a real gap, so each head
    # keeps its 1:1 aspect wherever it is drawn.
    def block(grid_names, columns, gap, out_name, scale=4):
        rows = (len(grid_names) + columns - 1) // columns
        width = columns * SIZE + (columns - 1) * gap
        height = rows * SIZE + (rows - 1) * gap
        canvas = [[None] * width for _ in range(height)]
        for index, name in enumerate(grid_names):
            head = canvas_of(MASCOTS[name], BODIES[name])
            col, row = index % columns, index // columns
            ox = col * (SIZE + gap)
            oy = row * (SIZE + gap)
            for y in range(SIZE):
                for x in range(SIZE):
                    canvas[oy + y][ox + x] = head[y][x]
        write_png(os.path.join(HERE, out_name), width, height, canvas, scale)
        return width, height

    # All eight, 4 x 2, for the picker and the head row on the preview page.
    sw, sh = block(names, columns=4, gap=2, out_name="sheet.png")

    # The repeating background. 2 x 2 at gap 2 is exactly 34 x 34 cells, which is the
    # size the preview's long-standing `background-size: 34px 34px` already expects —
    # it was only ever wrong because the file it pointed at was a strip. Every head
    # carries a transparent 1-cell margin, so the edge margins add up to the same 2-cell
    # gap as the interior and the block repeats without a seam.
    tw, th = block(names[:4], columns=2, gap=2, out_name="tile.png")

    print("wrote %s" % ", ".join(n + ".png" for n in names))
    print("  sheet.png %d x %d px (%.2f:1)" % (sw * 4, sh * 4, sw / sh))
    print("  tile.png  %d x %d cells -> %d x %d px" % (tw, th, tw * 4, th * 4))


# ---------------------------------------------------------------------------
# Icon composition: 32 x 32, glyph above, wordmark below, on the blush plate.
# ---------------------------------------------------------------------------
PLATE = "#FFEDF1"
PLATE_EDGE = "#A84765"

def place(canvas, rows, origin_x, origin_y, swap):
    for y, row in enumerate(rows):
        for x, cell in enumerate(row):
            if cell == ".":
                continue
            canvas[origin_y + y][origin_x + x] = swap.get(cell, swap.get("*", INK))


def read_png_cells(path, cell_px):
    """Reads a PNG written by this generator family and returns its cell grid as hex or None."""
    data = open(path, "rb").read()
    pos, idat, width, height = 8, b"", 0, 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height = struct.unpack(">II", body[:8])
        elif tag == b"IDAT":
            idat += body
        pos += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4

    def paeth(a, b, c):
        q = a + b - c
        pa, pb, pc = abs(q - a), abs(q - b), abs(q - c)
        return a if pa <= pb and pa <= pc else (b if pb <= pc else c)

    rows, previous, cursor = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[cursor]; cursor += 1
        line = bytearray(raw[cursor:cursor + stride]); cursor += stride
        if filt == 1:
            for i in range(4, stride):
                line[i] = (line[i] + line[i - 4]) & 255
        elif filt == 2:
            for i in range(stride):
                line[i] = (line[i] + previous[i]) & 255
        elif filt == 3:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                line[i] = (line[i] + ((a + previous[i]) >> 1)) & 255
        elif filt == 4:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                c = previous[i - 4] if i >= 4 else 0
                line[i] = (line[i] + paeth(a, previous[i], c)) & 255
        rows.append(bytes(line))
        previous = line

    grid = []
    for cy in range(height // cell_px):
        line = []
        for cx in range(width // cell_px):
            x, y = cx * cell_px + cell_px // 2, cy * cell_px + cell_px // 2
            pixel = rows[y][x * 4:x * 4 + 4]
            line.append(None if pixel[3] == 0 else "#%02X%02X%02X" % tuple(pixel[:3]))
        grid.append(line)
    return grid


def fit_cells(grid, target_w, target_h):
    """Nearest-neighbour resample of a cell grid, keeping it centred in target_w x target_h."""
    source_h, source_w = len(grid), len(grid[0])
    out = [[None] * target_w for _ in range(target_h)]
    off_x = (target_w - source_w) // 2
    off_y = (target_h - source_h) // 2
    for y in range(source_h):
        for x in range(source_w):
            ty, tx = off_y + y, off_x + x
            if 0 <= tx < target_w and 0 <= ty < target_h:
                out[ty][tx] = grid[y][x]
    return out


def place_cells(canvas, grid, origin_x, origin_y, recolor=None):
    for y, row in enumerate(grid):
        for x, colour in enumerate(row):
            if colour is None:
                continue
            tx, ty = origin_x + x, origin_y + y
            if 0 <= tx < ICON and 0 <= ty < ICON:
                canvas[ty][tx] = recolor(colour) if recolor else colour


def head_grid(name):
    body = BODIES[name]
    return [[None if cell == "." else (body if cell == "B" else TOKENS[cell])
             for cell in row] for row in MASCOTS[name]]


def blank_icon():
    return [[PLATE] * ICON for _ in range(ICON)]


def draw_wordmark(canvas, y):
    cells = wordmark("DERBIS")
    place_cells(canvas, [[INK if c == "#" else None for c in row] for row in cells],
                (ICON - len(cells[0])) // 2, y)


def main_icons():
    written = []

    # the real mark, read back from pixel-logo/logo.png (21 x 28 cells at scale 8)
    source = os.path.join(HERE, "..", "pixel-logo", "logo.png")
    mark = read_png_cells(source, 8) if os.path.exists(source) else None

    if mark:
        # mark above the wordmark: 32 - 5 - 2 = 25 rows of room
        shrunk = fit_cells(mark, 18, 24)

        canvas = blank_icon()
        place_cells(canvas, shrunk, (ICON - 18) // 2, 0)
        draw_wordmark(canvas, 26)
        write_png(os.path.join(HERE, "icon-mark.png"), ICON, ICON, canvas, 8)
        written.append("icon-mark.png")

        canvas = blank_icon()
        place_cells(canvas, shrunk, (ICON - 18) // 2, 0,
                    recolor=lambda c: {"#A84765": INK, "#FFC2D3": "#FFB3C9"}.get(c, c))
        draw_wordmark(canvas, 26)
        write_png(os.path.join(HERE, "icon-pixel.png"), ICON, ICON, canvas, 8)
        written.append("icon-pixel.png")

    for name in sorted(MASCOTS):
        canvas = blank_icon()
        place_cells(canvas, head_grid(name), 8, 1)
        draw_wordmark(canvas, 25)
        write_png(os.path.join(HERE, "icon-%s.png" % name), ICON, ICON, canvas, 8)
        written.append("icon-%s.png" % name)

    print("wrote " + ", ".join(written))


def contact_sheet(paths, scale, out_name):
    """A row of icons at one scale, for eyeballing in the preview page."""
    tiles = [read_png_cells(path, 8) for path in paths]
    gap = 2
    width = len(tiles) * (ICON + gap) - gap
    sheet = [[None] * width for _ in range(ICON)]
    for index, tile in enumerate(tiles):
        origin = index * (ICON + gap)
        for y in range(ICON):
            for x in range(ICON):
                sheet[y][origin + x] = tile[y][x] or PLATE
    write_png(os.path.join(HERE, out_name), width, ICON, sheet, scale)
    print("wrote " + out_name)


if __name__ == "__main__":
    main()
    main_icons()
    contact_sheet([os.path.join(HERE, "icon-mark.png"),
                   os.path.join(HERE, "icon-pixel.png")] +
                  [os.path.join(HERE, "icon-%s.png" % n) for n in sorted(MASCOTS)],
                  4, "icons-sheet.png")
