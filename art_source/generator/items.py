"""Item, utensil and fabric drawings. Each `icon_*` is centred on (0,0) and fits in ~220x220,
so it can be placed in a 256x256 sprite or scaled into scene piles. No text anywhere.
"""
from __future__ import annotations

import math
import random

from svgkit import C, LW_MID, LW_SMALL, circle, ellipse, group, leaf, line, path, poly, rect, shadow

# ---------------------------------------------------------------- fabrics

FABRICS = {
    # id: (base, pattern kind, pattern colour) — patterns differ in shape, not only colour
    "cloth_cream_stripes": ("#f1e2c0", "stripes", "#b8502f"),
    "cloth_indigo_dots": (C["indigo"], "dots", "#f4efe2"),
    "cloth_maroon_checks": ("#8e2f3c", "checks", "#e8c46a"),
    "cloth_leaf_print": ("#e7c75a", "leaves", "#3f6b3a"),
}


def fabric_defs() -> str:
    out = []
    for fid, (base, kind, col) in FABRICS.items():
        if kind == "stripes":
            body = f'<rect width="24" height="24" fill="{base}"/><rect x="0" y="0" width="7" height="24" fill="{col}" opacity=".8"/>'
        elif kind == "dots":
            body = f'<rect width="24" height="24" fill="{base}"/><circle cx="6" cy="6" r="3.2" fill="{col}"/><circle cx="18" cy="18" r="3.2" fill="{col}"/>'
        elif kind == "checks":
            body = (f'<rect width="24" height="24" fill="{base}"/><rect x="0" y="9" width="24" height="5" fill="{col}" opacity=".75"/>'
                    f'<rect x="9" y="0" width="5" height="24" fill="{col}" opacity=".75"/>')
        else:
            body = (f'<rect width="24" height="24" fill="{base}"/><path d="M4,18 Q8,8 14,6 Q12,14 4,18 Z" fill="{col}"/>'
                    f'<path d="M14,22 Q18,15 23,14 Q21,20 14,22 Z" fill="{col}"/>')
        out.append(f'<pattern id="pat_{fid}" width="24" height="24" patternUnits="userSpaceOnUse">{body}</pattern>')
    return "".join(out)


def folded_fabric(fid: str, w: float = 180, h: float = 60) -> str:
    return group(
        shadow(0, h / 2 + 4, w * 0.55, 8),
        rect(-w / 2, -h / 2, w, h, f"url(#pat_{fid})", rx=12, sw=LW_MID),
        path(f"M{-w / 2 + 8},{-h / 2 + 14} Q0,{-h / 2 + 4} {w / 2 - 8},{-h / 2 + 14}", sw=1.6, stroke="#000", extra='opacity=".18"'),
    )


def icon_fabric(fid: str) -> str:
    return group(folded_fabric(fid, 200, 56), group(folded_fabric(fid, 190, 56), transform="translate(6 -56)"))


def swatch(fid: str) -> str:
    return group(rect(-100, -100, 200, 200, f"url(#pat_{fid})", rx=24, sw=LW_MID))


# ---------------------------------------------------------------- produce

def icon_tomato() -> str:
    return group(shadow(0, 70, 80, 12), circle(0, 0, 78, "#d6402f", LW_MID),
                 ellipse(-26, -26, 22, 14, "#ffffff", sw=None, extra='opacity=".35"'),
                 *[leaf(0, -70, 34, 14, a, fill=C["leaf_dark"]) for a in (-110, -50, 10, 70, 130)],
                 circle(0, -72, 8, C["leaf_dark"], sw=1.4))


def icon_banana() -> str:
    def b(rot):
        return group(path("M-90,-10 Q-40,80 70,30 Q80,22 90,30 Q40,110 -70,40 Q-100,20 -90,-10 Z", "#f2cd3c", LW_MID),
                     path("M-60,20 Q0,60 60,38", sw=1.6, stroke="#c9a12a"), rect(-98, -22, 16, 16, "#6b5d2c", rx=4, sw=LW_SMALL),
                     transform=f"rotate({rot})")
    return group(shadow(0, 70, 90, 10), b(-14), b(0), b(14))


def icon_brinjal() -> str:
    return group(shadow(0, 72, 70, 10),
                 path("M-10,-40 Q-80,-10 -60,50 Q-30,90 30,70 Q80,40 50,-20 Q30,-60 -10,-40 Z", "#5a2a6b", LW_MID),
                 ellipse(-30, 10, 12, 26, "#ffffff", sw=None, extra='opacity=".25" transform="rotate(30 -30 10)"'),
                 path("M-20,-46 Q10,-80 40,-56 Q30,-36 10,-36 Q-8,-30 -20,-46 Z", C["leaf"], LW_SMALL),
                 path("M14,-70 Q20,-90 34,-96", sw=6, stroke=C["leaf_dark"]))


def icon_onion() -> str:
    return group(shadow(0, 72, 70, 10),
                 path("M0,-70 Q70,-20 60,30 Q50,72 0,74 Q-50,72 -60,30 Q-70,-20 0,-70 Z", "#b0526a", LW_MID),
                 path("M0,-64 Q30,-10 24,66", sw=1.6, stroke="#7d3549"), path("M0,-64 Q-30,-10 -24,66", sw=1.6, stroke="#7d3549"),
                 path("M0,-70 Q4,-92 14,-100", sw=4, stroke="#c9a476"))


# ---------------------------------------------------------------- spices / pantry

def _bowl(fill, mound) -> str:
    return group(shadow(0, 60, 90, 12),
                 path("M-80,-10 Q-60,-50 0,-56 Q60,-50 80,-10 Z", mound, LW_SMALL),
                 path("M-92,-10 L92,-10 Q86,50 0,56 Q-86,50 -92,-10 Z", fill, LW_MID),
                 path("M-80,8 Q0,20 80,8", sw=1.6, stroke="#000", extra='opacity=".2"'))


def icon_turmeric() -> str:
    return _bowl("#8a5230", "#e8b22a")


def icon_chilli() -> str:
    return _bowl("#8a5230", "#c23b2a")


def icon_rice() -> str:
    g = [_bowl(C["steel"], "#f6f1e3")]
    rnd = random.Random(2)
    for _ in range(18):
        x, y = rnd.uniform(-60, 60), rnd.uniform(-48, -16)
        g.append(ellipse(x, y, 5, 2.2, "#ffffff", sw=0.8, stroke="#c8bfa8"))
    return group(*g)


def icon_coffee_powder() -> str:
    return group(shadow(0, 76, 70, 10),
                 rect(-60, -60, 120, 136, "url(#gSteel)", rx=14, sw=LW_MID),
                 rect(-66, -78, 132, 24, C["steel_dark"], rx=8, sw=LW_MID),
                 rect(-44, -20, 88, 60, "#5b3a24", rx=8, sw=LW_SMALL),
                 *[circle(-26 + i * 13, 10 + (i % 2) * 8, 4, "#3a2416", sw=None) for i in range(5)])


# ---------------------------------------------------------------- flowers

def icon_jasmine() -> str:
    g = [shadow(0, 70, 90, 10)]
    for i in range(22):
        a = math.radians(-160 + i * 15)
        x, y = math.cos(a) * 80, math.sin(a) * 40 + 20
        g.append(ellipse(x, y, 11, 8, C["white"], LW_SMALL))
        if i % 3 == 0:
            g.append(leaf(x, y, 22, 10, i * 15, fill=C["leaf"]))
    return group(*g)


def icon_marigold() -> str:
    g = [shadow(0, 70, 80, 10)]
    for k, (x, y) in enumerate([(-50, 10), (0, -10), (50, 10), (-25, 45), (25, 45)]):
        for r, col in ((40, "#e8891e"), (28, "#f2a531"), (14, "#f7c44f")):
            g.append(circle(x, y, r, col, sw=1.6 if r == 40 else None))
    return group(*g)


def icon_rose() -> str:
    g = [shadow(0, 72, 70, 10), path("M0,0 L-6,80", sw=6, stroke=C["leaf_dark"]),
         leaf(-4, 50, 40, 18, -60), leaf(-4, 40, 40, 18, 60)]
    g += [circle(0, -10, 54, "#c2324a", LW_MID), path("M-30,-10 Q0,-50 30,-10", sw=3, stroke="#8e1f33"),
          path("M-20,4 Q0,-26 22,4", sw=3, stroke="#8e1f33"), path("M-10,14 Q2,-4 12,14", sw=3, stroke="#8e1f33")]
    return group(*g)


ITEM_ICONS = {
    "tomato": icon_tomato, "banana": icon_banana, "brinjal": icon_brinjal, "onion": icon_onion,
    "turmeric": icon_turmeric, "chilli_powder": icon_chilli, "rice": icon_rice, "coffee_powder": icon_coffee_powder,
    "jasmine": icon_jasmine, "marigold": icon_marigold, "rose": icon_rose,
    "cloth_indigo_dots": lambda: icon_fabric("cloth_indigo_dots"),
    "cloth_maroon_checks": lambda: icon_fabric("cloth_maroon_checks"),
    "cloth_leaf_print": lambda: icon_fabric("cloth_leaf_print"),
    "cloth_cream_stripes": lambda: icon_fabric("cloth_cream_stripes"),
}


def pile(item_id: str, w: float = 260) -> str:
    """A basket (or shelf stack for cloth) holding several of one item, for stall counters. Origin bottom-centre."""
    draw = ITEM_ICONS[item_id]
    if item_id.startswith("cloth_"):
        fid = item_id
        return group(*[group(folded_fabric(fid, w * 0.8, 46), transform=f"translate({(k % 2) * 8} {-30 - k * 44})") for k in range(4)])
    if item_id in ("turmeric", "chilli_powder", "rice", "coffee_powder"):
        return group(group(draw(), transform=f"translate(0 -70) scale({w / 220:.3f})"))
    rnd = random.Random(len(item_id))
    items = [path(f"M{-w / 2},-70 L{w / 2},-70 L{w / 2 - 24},0 L{-w / 2 + 24},0 Z", "#b88a4e", LW_MID)]
    for k in range(4):
        items.append(line(-w / 2 + 14 + k * 8, -60 + k * 15, w / 2 - 14 - k * 8, -60 + k * 15, sw=1.6, stroke="#7a5a30"))
    tops = []
    for k in range(5):
        x = -w * 0.32 + k * w * 0.16 + rnd.uniform(-6, 6)
        y = -80 - (k % 2) * 26
        tops.append(group(draw(), transform=f"translate({x:.0f} {y}) scale(0.42)"))
    tops.append(group(draw(), transform=f"translate(0 -116) scale(0.45)"))
    return group(shadow(0, 0, w * 0.55, 12), *tops, *items[:1], *items[1:])


# ---------------------------------------------------------------- kitchen utensils (states)

def filter_vessel(state: str) -> str:
    """South Indian coffee filter: empty | powder | brewing (water added)."""
    g = [shadow(0, 100, 80, 10),
         rect(-62, -10, 124, 110, "url(#gSteel)", rx=12, sw=LW_MID),
         rect(-70, -96, 140, 86, "url(#gSteel)", rx=10, sw=LW_MID),
         ellipse(0, -96, 70, 16, "#e9edef", LW_MID)]
    if state in ("powder", "brewing"):
        g.append(ellipse(0, -94, 56, 11, "#5b3a24" if state == "powder" else "#3d2414", sw=None))
    if state == "brewing":
        g += [path("M-20,-120 q-10,-20 6,-38", sw=4, stroke="#ffffff", extra='opacity=".8"'),
              path("M18,-118 q-10,-18 6,-34", sw=4, stroke="#ffffff", extra='opacity=".7"')]
    g.append(rect(-20, -112, 40, 14, C["steel_dark"], rx=5, sw=LW_SMALL))
    return group(*g)


def tumbler(state: str) -> str:
    """empty | decoction | coffee — tumbler standing in a davara."""
    g = [shadow(0, 92, 100, 12),
         path("M-100,60 Q-100,96 0,96 Q100,96 100,60 Z", "url(#gSteel)", LW_MID),
         ellipse(0, 60, 100, 16, "#dfe4e7", LW_MID),
         path("M-44,64 L-54,-90 L54,-90 L44,64 Z", "url(#gSteel)", LW_MID),
         ellipse(0, -90, 54, 12, "#dfe4e7", LW_MID)]
    if state == "decoction":
        g.append(ellipse(0, -88, 46, 8, "#3d2414", sw=None))
    elif state == "coffee":
        g += [ellipse(0, -88, 46, 8, "#c89a6c", sw=None), ellipse(0, -90, 30, 4, "#f1dfc4", sw=None),
              path("M-14,-110 q-10,-20 6,-38", sw=4, stroke="#ffffff", extra='opacity=".8"')]
    return group(*g)


def kettle() -> str:
    return group(shadow(0, 92, 90, 12),
                 path("M-80,90 Q-96,-10 -40,-40 L40,-40 Q96,-10 80,90 Z", "url(#gSteel)", LW_MID),
                 path("M70,0 Q120,-30 130,-60", sw=14, stroke=C["steel_dark"]), path("M70,0 Q120,-30 130,-60", sw=LW_SMALL),
                 path("M-40,-40 Q0,-110 40,-40", sw=8, stroke=C["wood_dark"]), rect(-16, -56, 32, 16, C["wood_dark"], rx=5, sw=LW_SMALL),
                 path("M-10,-70 q-10,-20 6,-38", sw=4, stroke="#ffffff", extra='opacity=".7"'))


def milk_pot() -> str:
    return group(shadow(0, 86, 80, 10),
                 path("M-70,-50 L70,-50 L60,84 L-60,84 Z", "url(#gBrass)", LW_MID),
                 ellipse(0, -50, 70, 14, "#fbf6ea", LW_MID), path("M-70,-50 Q-90,-60 -96,-40", sw=LW_MID))


def sugar_bowl() -> str:
    return group(_bowl("url(#gSteel)", "#fbf8f0"))


def plate(dirty: bool) -> str:
    g = [shadow(0, 20, 110, 12), ellipse(0, 0, 110, 46, "url(#gSteel)", LW_MID), ellipse(0, 0, 76, 30, "#dfe4e7", LW_SMALL)]
    if dirty:
        g += [ellipse(-24, -6, 22, 9, "#c9963e", sw=None, extra='opacity=".8"'), ellipse(28, 6, 16, 7, "#9a5a2a", sw=None, extra='opacity=".7"'),
              circle(10, -12, 5, "#7a4a22", sw=None)]
    else:
        g.append(path("M-60,-14 Q-20,-30 30,-26", sw=4, stroke="#ffffff", extra='opacity=".8"'))
    return group(*g)


def cup(dirty: bool) -> str:
    g = [shadow(0, 76, 60, 10), path("M-44,70 L-54,-70 L54,-70 L44,70 Z", "url(#gSteel)", LW_MID),
         ellipse(0, -70, 54, 12, "#dfe4e7", LW_MID)]
    if dirty:
        g += [ellipse(0, -68, 40, 7, "#8a5a32", sw=None), path("M-50,-40 q20,10 40,0", sw=5, stroke="#8a5a32", extra='opacity=".6"')]
    else:
        g.append(path("M-30,-40 L-26,40", sw=5, stroke="#ffffff", extra='opacity=".7"'))
    return group(*g)


def bowl(dirty: bool) -> str:
    g = [shadow(0, 60, 96, 12), path("M-96,-20 L96,-20 Q90,56 0,60 Q-90,56 -96,-20 Z", "url(#gBrass)", LW_MID),
         ellipse(0, -20, 96, 22, "#f3d27a", LW_MID)]
    if dirty:
        g += [ellipse(-20, -20, 40, 10, "#c9963e", sw=None), circle(30, -24, 6, "#7a4a22", sw=None)]
    else:
        g.append(path("M-60,-22 Q0,-34 50,-24", sw=4, stroke="#ffffff", extra='opacity=".8"'))
    return group(*g)


# ---------------------------------------------------------------- sewing templates

def template_shape(kind: str) -> str:
    if kind == "bag":
        return "M-80,-50 L80,-50 L92,96 L-92,96 Z M-46,-50 Q-46,-120 0,-120 Q46,-120 46,-50"
    if kind == "pillow":
        return "M-110,-80 Q0,-96 110,-80 Q124,0 110,80 Q0,96 -110,80 Q-124,0 -110,-80 Z"
    return ("M-40,-100 L40,-100 Q50,-84 70,-96 L120,-60 L96,-20 L70,-34 L70,100 L-70,100 L-70,-34 L-96,-20 "
            "L-120,-60 L-70,-96 Q-50,-84 -40,-100 Z")


def creation(kind: str, fid: str) -> str:
    d = template_shape(kind)
    if kind == "bag":
        body, handle = d.split(" M")
        return group(shadow(0, 100, 100, 12), path(body, f"url(#pat_{fid})", LW_MID),
                     path("M" + handle, sw=12, stroke="#6b4a2e"), path("M" + handle, sw=LW_SMALL))
    return group(shadow(0, 100, 110, 12), path(d, f"url(#pat_{fid})", LW_MID),
                 path(d, "none", sw=2, stroke="#ffffff", extra='opacity=".5" stroke-dasharray="8 8" transform="scale(0.9)"'))


def template_outline(kind: str) -> str:
    d = template_shape(kind)
    return group(path(d.split(" M")[0] if kind == "bag" else d, "#fbf3e2", sw=4, stroke="#7a5a40", extra='stroke-dasharray="14 10"'))
