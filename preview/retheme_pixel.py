#!/usr/bin/env python3
"""
Puts the Pixel skin on the feature mocks in preview/.

The mocks were drawn before the skin existed: near-black grounds, white type, rounded
cards, amber and pink accents. The drawings are still right — the cover-flow geometry,
the sign-in flow, the Wrapped header — so they are not rewritten. What changes is the
palette, the corner radius and the type face, which is what "Pixel is equipped" means.

Nothing is destroyed. The page keeps its original stylesheet verbatim, and the rethemed
copy is written next to it with `disabled` set. The toggle at the bottom swaps which one
is enabled, so the default look is always one click away --- which is also the standing
rule in the app: Pixel applies only while it is equipped.

Artwork colours are deliberately not mapped. An album cover is not the skin's to
recolour, in a mock no less than in the app.

Run from the repo root:  python3 preview/retheme_pixel.py
"""

import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent

# The palette, as shipped in ui/theme/PixelPaper.kt.
PAPER = "#FFEDF1"
CARD = "#FFF8F6"
BAND = "#FFD9E2"
BAND_DEEP = "#FFC9D6"
EDGE = "#A84765"
INK = "#5B2338"
INK_MUTED = "#7E3B57"
ROSE = "#9E1B44"
HOT = "#FF5C8A"

# value -> replacement, applied to the stylesheet copy only.
SWAPS = [
    # grounds
    ("#08080a", PAPER), ("#08090b", PAPER), ("#0b0b0d", PAPER), ("#050506", PAPER),
    ("#0b0710", PAPER), ("#0c0c0e", PAPER), ("#07050a", PAPER), ("#12060f", PAPER),
    ("#0e0e10", PAPER), ("#141416", PAPER), ("#241335", BAND_DEEP), ("#141020", BAND),
    # surfaces
    ("#101015", CARD), ("#141418", CARD), ("#16161c", CARD), ("#16161a", CARD),
    ("#17171c", CARD), ("#131316", CARD), ("#191919", CARD), ("#0f0f12", CARD),
    ("#26262b", BAND_DEEP), ("#2a2a31", BAND_DEEP), ("#07070a", INK),
    # type
    ("#f4f1ea", INK), ("#e9e7e3", INK), ("#f3f1ee", INK), ("#ffffff", INK),
    ("#f5f5f7", INK), ("#e8e6ec", EDGE), ("#111114", INK),
    # secondary type
    ("#9a9aa6", INK_MUTED), ("#cfcfd8", INK_MUTED), ("#6d6d78", INK_MUTED),
    ("#6e6e7a", INK_MUTED), ("#7c7c88", INK_MUTED), ("#b8b8c2", INK_MUTED),
    # accents
    ("#f06a9b", ROSE), ("#ff5e3a", ROSE), ("#f4a0b8", ROSE), ("#d9863f", ROSE),
    ("#ff5f86", ROSE), ("#f0b25e", HOT), ("#8d6bff", BAND_DEEP), ("#ffd166", HOT),
    # semantic: a "fixed" state stays legible on paper
    ("#35d07f", "#2E6B4F"),
    # hairline veils tuned for black; on paper they invert to edge at low alpha
    ("rgba(255,255,255,.07)", "rgba(168,71,101,.18)"),
    ("rgba(255,255,255,.09)", "rgba(168,71,101,.22)"),
    ("rgba(255,255,255,.12)", "rgba(168,71,101,.26)"),
    ("rgba(255,255,255,.14)", "rgba(168,71,101,.30)"),
    ("rgba(255,255,255,.16)", "rgba(168,71,101,.32)"),
    ("rgba(255,255,255,.18)", "rgba(168,71,101,.34)"),
    ("rgba(255, 255, 255, 0.12)", "rgba(168, 71, 101, 0.26)"),
    ("rgba(255, 255, 255, 0.18)", "rgba(168, 71, 101, 0.34)"),
]

# Rules appended to the rethemed copy. The mocks were never written against a light
# ground, so a few things need saying out loud: hard edges, one type face, and a border
# on any mock screen that would otherwise dissolve into the paper behind it.
APPEND = """
  /* --- the skin itself -------------------------------------------------------
     Ground, cards, ink and accent were remapped above. What is left is the two
     decisions a palette cannot carry: no corner radius, and one type face. */
  *, *::before, *::after { border-radius: 0 !important; }
  /* the @font-face itself lives in fonts/pixelify.css, which every page links */
  body, button, input, select, textarea {
    font-family: "Pixelify", ui-monospace, "SF Mono", Menlo, Consolas, monospace !important;
  }
  /* The mocks all used to be darker than the page, so none of them needed an outline.
     Inverted, the page is the band and every mock is paper: each frame gets an edge, or
     it dissolves into the desk behind it. */
  html, body { background: %(band)s !important; }
  .screen, .deck, .vert, .player, .story, .phone, .frame, .card2 {
    border: 2px solid %(edge)s !important;
  }
""" % {"edge": EDGE, "band": BAND}

TOGGLE_CSS = """
  /* the skin switch. Fixed, so it is always reachable, and loud enough to find. */
  #px-toggle {
    position: fixed; right: 20px; bottom: 20px; z-index: 9999;
    display: flex; align-items: center; gap: 10px;
    background: #1B1116; border: 2px solid #A84765; padding: 9px 12px;
    font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
    font-size: 10.5px; letter-spacing: .12em; text-transform: uppercase;
    color: #FFD9E2; cursor: pointer; border-radius: 0;
    box-shadow: 0 0 0 2px #1B1116, 6px 6px 0 rgba(0,0,0,.45);
  }
  #px-toggle .sq { width: 12px; height: 12px; background: #FF5C8A; border: 1px solid #FFD9E2; }
  #px-toggle[data-on="false"] .sq { background: transparent; }
  #px-toggle span.k { color: #FF9DBB; }

  /* says which skin the page is wearing, and where to turn it off */
  .px-lede {
    display: block; margin: 0 0 22px; padding: 10px 13px;
    border-left: 4px solid #9E1B44; background: #FFF8F6; color: #5B2338;
    font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
    font-size: 11.5px; line-height: 1.6; max-width: 760px; border-radius: 0;
  }
  .px-lede b { color: #9E1B44; }

  /* shown in place of a swatch row that only drives the default palette */
  .px-accentnote { display: none; margin: 0 0 12px; font-size: 11px; line-height: 1.6; color: #7E3B57; }
  body.px-on .px-accentnote { display: block; }
  body.px-on .swatches { display: none; }
"""

TOGGLE_HTML = """
<div id="px-toggle" data-on="true" role="button" tabindex="0" aria-pressed="true">
  <span class="sq"></span>
  <span>Skin: <span class="k" id="px-name">Pixel</span></span>
</div>
<script>
  /* Both stylesheets are in the document. The switch enables one and disables the
     other, which is why the default look comes back exactly rather than approximately. */
  (function () {
    var pixel = document.getElementById('css-pixel');
    var plain = document.getElementById('css-default');
    var btn = document.getElementById('px-toggle');
    var name = document.getElementById('px-name');

    function apply(on) {
      pixel.disabled = !on;
      plain.disabled = on;
      document.body.classList.toggle('px-on', on);
      btn.dataset.on = String(on);
      btn.setAttribute('aria-pressed', String(on));
      name.textContent = on ? 'Pixel' : 'Default';
    }
    function toggle() { apply(btn.dataset.on !== 'true'); }

    btn.addEventListener('click', toggle);
    btn.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); }
    });
    apply(true);
  })();
</script>
"""


def map_white_alpha(css):
    """Every remaining white veil is a panel or a highlight tuned for a black ground.
    On paper a white-at-6% fill is invisible, so the veil becomes rose at comparable
    weight rather than being left to disappear."""
    def repl(m):
        raw = m.group(1)
        a = float(raw if "." in raw else "0." + raw if raw.startswith("0") else raw)
        out = min(0.6, a * 2.2) if a <= 0.12 else min(0.6, a * 0.7)
        return "rgba(168,71,101,%.2f)" % out
    return re.sub(r"rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*(\d*\.?\d+)\s*\)", repl, css)


def extract_styles(html):
    """Returns (html_without_styles, list_of_css_blocks)."""
    blocks = re.findall(r"<style[^>]*>(.*?)</style>", html, flags=re.S | re.I)
    stripped = re.sub(r"<style[^>]*>.*?</style>", "", html, flags=re.S | re.I)
    return stripped, blocks


def retheme_style(css):
    out = css
    for old, new in SWAPS:
        out = re.sub(re.escape(old), new, out, flags=re.IGNORECASE)
    out = re.sub(r"#fff\b", INK, out, flags=re.IGNORECASE)
    return map_white_alpha(out)


# A pixel face needs about 11 px before its cells resolve; below that the glyph turns to
# mush. The app's smallest Material role is labelSmall at 11 sp, so the rethemed copy must
# not show 9 px text the app cannot produce. Only the Pixel stylesheet gets the floor ---
# the default look keeps its own sizes, because it is set in a proportional face.
PIXEL_SIZE_FLOOR = 11.0


def raise_sizes(css):
    def repl(m):
        value = float(m.group(1))
        return "font-size: %gpx" % (PIXEL_SIZE_FLOOR if value < PIXEL_SIZE_FLOOR else value)
    return re.sub(r"font-size:\s*([0-9.]+)px", repl, css)


def build(name):
    path = HERE / (name + ".html")
    html = path.read_text(encoding="utf-8")
    if 'id="css-pixel"' in html:
        print("  %-26s already rethemed, skipped" % name)
        return
    if "<style" not in html:
        print("  %-26s no stylesheet, skipped" % name)
        return

    stripped, blocks = extract_styles(html)
    original = "\n".join(blocks)
    skinned = "\n".join(raise_sizes(retheme_style(b)) for b in blocks) + APPEND

    styled = (
        '<style id="css-default" disabled>\n' + original + "\n</style>\n"
        '<style id="css-pixel">\n' + skinned + "\n</style>\n"
        '<style>\n' + TOGGLE_CSS + "</style>\n"
    )
    out = stripped.replace("</head>", styled + "</head>", 1) if "</head>" in stripped else styled + stripped
    out = out.replace("</body>", TOGGLE_HTML + "</body>", 1) if "</body>" in out else out + TOGGLE_HTML

    lede = (
        '<p class="px-lede"><b>Pixel skin applied.</b> This mock was drawn before the skin existed, '
        'so its original stylesheet is still in the page: the switch at the bottom right turns the '
        'skin off and puts that look back, exactly.</p>\n'
    )
    if "</h1>" in out:
        out = out.replace("</h1>", "</h1>\n" + lede, 1)
    elif "<body>" in out:
        out = out.replace("<body>", "<body>\n" + lede, 1)

    # a swatch row that only moves the default accent should say so rather than do nothing
    accent_note = (
        '<p class="px-accentnote">Accent is rose <code>#9E1B44</code> while Pixel is equipped — '
        "the swatches below drive the default theme, so they are hidden here.</p>"
    )
    if '<div class="swatches">' in out:
        out = out.replace('<div class="swatches">', accent_note + '\n        <div class="swatches">', 1)

    path.write_text(out, encoding="utf-8")
    print("  %-26s rethemed (%d style block%s)" % (name, len(blocks), "" if len(blocks) == 1 else "s"))


# Which decks belong here is not a matter of taste — it is `DerbisExperienceSettings`:
#
#   * cover-flow-deck IS Pixel's deck. `PlayerVisualMode.Flow` is the default
#     (`DerbisExperienceSettings.kt:301`), and the Flow deck is the one drawn in pink. Its preview
#     stays on the skin, because that is what the skin looks like out of the box.
#   * vinyl-deck is an alternative the user picks *inside* any skin. Painting its page pink would
#     suggest the turntable only exists in Pixel, which is false, so it keeps its own palette.
#
# The rest of the list is skin- and surface-level mocks, which have no life outside the skin.
# cover-flow-deck, vinyl-deck and lyric-word-sync used to be separate pages; they were retired
# into pixel-motion.html, which is now the one place a deck or an animation is drawn.
PAGES = [
    "discord-connect",
    "spotify-signin-sheet",
    "wrapped-top-bar",
    "wrapped-running-year",
]


def main():
    print("retheming preview mocks onto the Pixel skin")
    for page in PAGES:
        build(page)


if __name__ == "__main__":
    main()
