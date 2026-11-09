#!/usr/bin/env python3
"""
Generates the Pixel skin's charm banners as transparent PNG + SVG.

The screenshot this replaces had each charm sitting in a white plate on the band.
The plate is gone: the charm is drawn straight onto the stripes, so every pixel
that is not the charm or the band is fully transparent (alpha 0, not white).

Run from the repo root:  python3 preview/pixel-banners/generate.py
"""

import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))

# ---------------------------------------------------------------- palette ---
# The band stays soft so a saturated charm reads on it without a white plate.
BAND_EDGE = "#FFF0F4"
BAND_HAIR = "#C9748D"
BAND_A = "#FFD9E2"
BAND_B = "#FFC2D3"

PALETTE = {
    ".": None,                     # transparent
    " ": None,
    "o": "#A84765",                # rose ink, every charm's outline
    "P": "#FF5C8A",                # hot pink
    "p": "#FF8FB1",                # blush shade
    "k": "#7A2C46",                # knot / deep
    "y": "#FFD98A",                # butter
    "Y": "#F5B942",                # butter shade
    "w": "#FFF8F6",                # cream highlight
}

# ------------------------------------------------------------------ art ---
# Equal-width ASCII grids. Every '.' and ' ' is transparent, so the band
# shows through where the old white plate used to be.
BOW = [
    ".................",
    "..ooo.......ooo..",
    ".owwPo.....oPwwo.",
    "owPPPPo...oPPPPwo",
    "oPPPPPPo.oPPPPPPo",
    "oPPPPPPPkPPPPPPPo",
    "oPPPPPPo.oPPPPPPo",
    "owPPPPo...oPPPPwo",
    ".owwPo.....oPwwo.",
    "..ooo.......ooo..",
]

STAR = [
    ".....o.....",
    "....oyo....",
    "....oyo....",
    ".oooyoyooo.",
    ".oyyyyyyyo.",
    "..oyyyyyo..",
    "...oyyyo...",
    "..oyyoyyo..",
    "..oyo.oyo..",
]

HEART = [
    "..oo...oo..",
    ".owPo.oPPo.",
    "owwPPoPPPo.",
    "owPPPPPPPPo",
    "oPPPPPPPPPo",
    ".oPPPPPPo..",
    "..oPPPPo...",
    "...oPPo....",
    "....oo.....",
]

CHARMS = [
    ("bow", BOW),
    ("star", STAR),
    ("heart", HEART),
]

# ----------------------------------------------------------------- layout ---
W = 120
H = 30
BAND_TOP, BAND_BOTTOM = 8, 21      # inclusive rows of the band
STRIPE = 4                          # cells per stripe

SCALE = 6                           # PNG upscale, so cells stay crisp


def blank():
    return [[None] * W for _ in range(H)]


def draw_band(canvas):
    for y in range(BAND_TOP, BAND_BOTTOM + 1):
        for x in range(W):
            if y == BAND_TOP:
                canvas[y][x] = BAND_EDGE
            elif y == BAND_BOTTOM:
                canvas[y][x] = BAND_EDGE
            elif y == BAND_TOP + 1 or y == BAND_BOTTOM - 1:
                canvas[y][x] = BAND_HAIR
            else:
                canvas[y][x] = BAND_A if (x // STRIPE) % 2 == 0 else BAND_B


def scale_grid(grid, factor):
    """Duplicate every cell so the charm reads at the band's scale."""
    out = []
    for row in grid:
        wide = "".join(ch * factor for ch in row)
        out.extend([wide] * factor)
    return out


def draw_charm(canvas, grid, factor=2):
    grid = scale_grid(grid, factor)
    gw = len(grid[0])
    gh = len(grid)
    for row in grid:
        assert len(row) == gw, "ragged sprite row: %r" % (row,)
    left = (W - gw) // 2
    # centre the charm on the band: it overhangs the stripes top and bottom, which is
    # what the plate used to fake
    top = (BAND_TOP + BAND_BOTTOM) // 2 + 1 - gh // 2
    for dy, row in enumerate(grid):
        for dx, ch in enumerate(row):
            colour = PALETTE[ch]
            if colour is None:
                continue
            y, x = top + dy, left + dx
            if 0 <= y < H and 0 <= x < W:
                canvas[y][x] = colour
    return left, top, gw, gh


# ------------------------------------------------------------------ chroma ---
def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def to_rgba(canvas, scale):
    """Nearest-neighbour upscale to RGBA rows. Transparent stays (0,0,0,0)."""
    rows = []
    for row in canvas:
        out = bytearray()
        for colour in row:
            if colour is None:
                out += b"\x00\x00\x00\x00"
            else:
                r, g, b = hex_rgb(colour)
                out += bytes((r, g, b, 255))
        scaled = bytes(out) * 1
        for _ in range(scale):
            for _ in range(1):
                pass
        # repeat pixels horizontally, then repeat the finished row vertically
        wide = bytearray()
        for px in range(W):
            wide += scaled[px * 4:(px + 1) * 4] * scale
        for _ in range(scale):
            rows.append(bytes(wide))
    return rows


# -------------------------------------------------------------------- png ---
def write_png(path, width, height, rows):
    raw = b"".join(b"\x00" + r for r in rows)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    blob = (b"\x89PNG\r\n\x1a\n" +
            chunk(b"IHDR", ihdr) +
            chunk(b"IDAT", zlib.compress(raw, 9)) +
            chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(blob)


# -------------------------------------------------------------------- svg ---
def row_runs(row):
    """Horizontal run-length encode so the SVG is rects, not one per pixel."""
    runs, x = [], 0
    while x < W:
        colour = row[x]
        start = x
        while x < W and row[x] == colour:
            x += 1
        if colour is not None:
            runs.append((start, x - start, colour))
    return runs


def write_svg(path, canvas):
    parts = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" '
        'width="%d" height="%d" shape-rendering="crispEdges" role="img">'
        % (W, H, W * 4, H * 4)
    ]
    for y, row in enumerate(canvas):
        for start, length, colour in row_runs(row):
            parts.append(
                '<rect x="%d" y="%d" width="%d" height="1" fill="%s"/>'
                % (start, y, length, colour)
            )
    parts.append("</svg>")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(parts))


# ------------------------------------------------------------------- main ---
def main():
    os.makedirs(HERE, exist_ok=True)
    written = []
    for name, grid in CHARMS:
        canvas = blank()
        draw_band(canvas)
        draw_charm(canvas, grid)

        png = os.path.join(HERE, "%s.png" % name)
        svg = os.path.join(HERE, "%s.svg" % name)
        write_png(png, W * SCALE, H * SCALE, to_rgba(canvas, SCALE))
        write_svg(svg, canvas)
        written += [png, svg]
        print("wrote %s.png  %dx%d  (%d rects in the svg)"
              % (name, W * SCALE, H * SCALE,
                 sum(len(row_runs(r)) for r in canvas)))

    print("\n%d files" % len(written))


if __name__ == "__main__":
    main()
