#!/usr/bin/env python3
"""
Builds the Pixel skin's logo from the real Derbis mark.

The shipped logo (app/src/main/res/drawable/derbis_logo.png) is a white mark on
transparent: three diamonds. Nothing here redraws the mark by hand — the alpha
channel of that file is read, cropped, resampled onto a pixel grid and then
inlaid with a rose outline and a blush highlight, so the Pixel logo is the same
shape at a coarser resolution rather than a different logo.

Outputs (all transparent where they should be, alpha 0, never white):
  logo.png / logo.svg     the mark alone, for in-app slots
  icon.png / icon.svg     the mark on the blush launcher plate

Run from the repo root:  python3 preview/pixel-logo/generate.py
"""

import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SOURCE = os.path.join(ROOT, "app", "src", "main", "res", "drawable", "derbis_logo.png")

COLUMNS = 21
ROWS = 28
SCALE = 8

INK = "#A84765"        # rose outline
FILL = "#FF5C8A"       # hot pink body
LIGHT = "#FFC2D3"      # blush highlight
SPARK = "#FFF8F6"      # cream catchlight

PLATE = "#FFEDF1"      # launcher plate
PLATE_EDGE = "#A84765"


# ---------------------------------------------------------------- png read ---
def read_rgba(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
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
        p = a + b - c
        pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
        if pa <= pb and pa <= pc:
            return a
        return b if pb <= pc else c

    rows, previous, cursor = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[cursor]
        cursor += 1
        line = bytearray(raw[cursor:cursor + stride])
        cursor += stride
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
    return width, height, rows


def read_alpha(path):
    width, height, rows = read_rgba(path)
    return width, height, [[rows[y][x * 4 + 3] for x in range(width)] for y in range(height)]


# ------------------------------------------------------------- resampling ---
def bbox(alpha, threshold=8):
    xs, ys = [], []
    for y, row in enumerate(alpha):
        for x, value in enumerate(row):
            if value > threshold:
                xs.append(x)
                ys.append(y)
    return min(xs), min(ys), max(xs) + 1, max(ys) + 1


def resample(alpha, box, columns, rows, on_threshold=110):
    x0, y0, x1, y1 = box
    grid = [[False] * columns for _ in range(rows)]
    # keep the mark's aspect inside the cell grid, centred
    mark_w, mark_h = x1 - x0, y1 - y0
    box_ratio = mark_w / mark_h
    grid_ratio = columns / rows
    if box_ratio > grid_ratio:
        used_rows = max(1, round(rows * grid_ratio / box_ratio))
        used_columns = columns
    else:
        used_columns = max(1, round(columns * box_ratio / grid_ratio))
        used_rows = rows
    off_x = (columns - used_columns) // 2
    off_y = (rows - used_rows) // 2

    for gy in range(used_rows):
        sy0 = y0 + gy * mark_h // used_rows
        sy1 = y0 + (gy + 1) * mark_h // used_rows
        for gx in range(used_columns):
            sx0 = x0 + gx * mark_w // used_columns
            sx1 = x0 + (gx + 1) * mark_w // used_columns
            total = 0
            count = 0
            for yy in range(sy0, max(sy1, sy0 + 1)):
                row = alpha[yy]
                for xx in range(sx0, max(sx1, sx0 + 1)):
                    total += row[xx]
                    count += 1
            grid[off_y + gy][off_x + gx] = (total / max(count, 1)) > on_threshold
    return grid


# --------------------------------------------------------------- inlay ---
def neighbours_on(grid, x, y):
    rows, columns = len(grid), len(grid[0])
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if nx < 0 or ny < 0 or nx >= columns or ny >= rows or not grid[ny][nx]:
            return False
    return True


def components(grid):
    rows, columns = len(grid), len(grid[0])
    seen = [[False] * columns for _ in range(rows)]
    groups = []
    for y in range(rows):
        for x in range(columns):
            if not grid[y][x] or seen[y][x]:
                continue
            stack, group = [(x, y)], []
            seen[y][x] = True
            while stack:
                cx, cy = stack.pop()
                group.append((cx, cy))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = cx + dx, cy + dy
                    if 0 <= nx < columns and 0 <= ny < rows and grid[ny][nx] and not seen[ny][nx]:
                        seen[ny][nx] = True
                        stack.append((nx, ny))
            groups.append(group)
    return groups


def inlay(grid):
    """Returns { (x, y): colour } — outline, body, highlight, catchlight."""
    cells = {}
    rows, columns = len(grid), len(grid[0])

    for group in components(grid):
        if len(group) < 3:
            continue
        cx = sum(p[0] for p in group) / len(group)
        cy = sum(p[1] for p in group) / len(group)
        reach = max(max(abs(p[0] - cx), abs(p[1] - cy)) for p in group) or 1.0
        for x, y in group:
            if not neighbours_on(grid, x, y):
                cells[(x, y)] = INK
                continue
            dx, dy = x - cx, y - cy
            # a crescent of light on the upper-left of every diamond, like the mark's
            # own specular edge, and one cream cell at its centre
            if dx + dy < -0.35 * reach and max(abs(dx), abs(dy)) < 0.55 * reach:
                cells[(x, y)] = LIGHT
            elif abs(dx) < 0.5 and abs(dy) < 0.5:
                cells[(x, y)] = SPARK
            else:
                cells[(x, y)] = FILL
    return cells


# ---------------------------------------------------------------- writers ---
def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def write_png(path, width, height, canvas, scale, plate=None, plate_corner=0):
    """canvas is a 2-D list of hex or None. plate fills the whole frame when given."""
    def pixel(x, y):
        if plate is not None:
            # rounded plate: corners knocked out
            c = plate_corner
            if c:
                for cx, cy in ((c - 1, c - 1), (width - c, c - 1), (c - 1, height - c), (width - c, height - c)):
                    if abs(x - cx) + abs(y - cy) < c - 1 and (
                        (x < c and y < c) or (x >= width - c and y < c)
                        or (x < c and y >= height - c) or (x >= width - c and y >= height - c)
                    ):
                        return b"\x00\x00\x00\x00"
            colour = canvas[y][x] if canvas[y][x] else plate
        else:
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

    raw = b"".join(b"\x00" + r for r in out_rows)

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


def write_svg(path, canvas, plate=None, plate_edge=None):
    rows, columns = len(canvas), len(canvas[0])
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" '
             'height="%d" shape-rendering="crispEdges" role="img">'
             % (columns, rows, columns * 4, rows * 4)]
    if plate:
        parts.append('<rect width="%d" height="%d" fill="%s"/>' % (columns, rows, plate))
    for y, row in enumerate(canvas):
        for start, length, colour in row_runs(row):
            parts.append('<rect x="%d" y="%d" width="%d" height="1" fill="%s"/>'
                         % (start, y, length, colour))
    if plate_edge:
        parts.append('<rect x="0" y="0" width="%d" height="%d" fill="none" stroke="%s" '
                     'stroke-width="1"/>' % (columns, rows, plate_edge))
    parts.append("</svg>")
    open(path, "w", encoding="utf-8").write("\n".join(parts))


# ------------------------------------------------------------------- main ---
def grid_to_canvas(cells, columns, rows):
    return [[cells.get((x, y)) for x in range(columns)] for y in range(rows)]


def ascii_preview(cells, columns, rows):
    glyph = {INK: "#", FILL: "o", LIGHT: "+", SPARK: "*"}
    for y in range(rows):
        line = "".join(glyph.get(cells.get((x, y)), ".") for x in range(columns))
        print("  " + line)


def main():
    width, height, alpha = read_alpha(SOURCE)
    box = bbox(alpha)
    grid = resample(alpha, box, COLUMNS, ROWS)
    cells = inlay(grid)
    canvas = grid_to_canvas(cells, COLUMNS, ROWS)

    os.makedirs(HERE, exist_ok=True)

    write_png(os.path.join(HERE, "logo.png"), COLUMNS, ROWS, canvas, SCALE)
    write_svg(os.path.join(HERE, "logo.svg"), canvas)

    # launcher plate: mark inset in a 32 x 32 blush tile with a rose edge
    ICON = 32
    inset = (ICON - COLUMNS) // 2
    vertical = (ICON - ROWS) // 2
    icon_canvas = [[None] * ICON for _ in range(ICON)]
    for y in range(ROWS):
        for x in range(COLUMNS):
            colour = canvas[y][x]
            if colour is not None:
                icon_canvas[vertical + y][inset + x] = colour
    write_png(os.path.join(HERE, "icon.png"), ICON, ICON, icon_canvas, SCALE,
              plate=PLATE, plate_corner=3)
    write_svg(os.path.join(HERE, "icon.svg"), icon_canvas, plate=PLATE, plate_edge=PLATE_EDGE)

    print("source  %dx%d  mark %dx%d" % (width, height, box[2] - box[0], box[3] - box[1]))
    print("grid    %dx%d  cells on %d" % (COLUMNS, ROWS, sum(1 for v in cells.values())))
    print()
    ascii_preview(cells, COLUMNS, ROWS)
    print()
    print("wrote logo.png/svg and icon.png/svg")


if __name__ == "__main__":
    main()
