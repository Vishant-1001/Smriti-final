"""Reusable props, drawn in local coordinates.

Convention: each function returns an SVG fragment; unless stated otherwise the origin is the
bottom-centre of the object's footprint so it can be "stood" on a floor line.
"""
from __future__ import annotations

import math
import random

from svgkit import (C, LW_BIG, LW_MID, LW_SMALL, OUT, Box, circle, ellipse, f, group, leaf,
                    line, path, poly, rect, shade, shadow)


# ---------------------------------------------------------------- plants

def potted_plant(kind: str = "broad", scale: float = 1.0, seed: int = 1) -> str:
    """Terracotta pot with foliage. Origin: bottom-centre of pot. Pot ~90 wide, plant ~220 tall."""
    rnd = random.Random(seed)
    items = [shadow(0, 0, 60, 12)]
    leaves = []
    if kind == "broad":
        for i in range(9):
            a = -70 + i * 17 + rnd.uniform(-6, 6)
            leaves.append(leaf(rnd.uniform(-8, 8), -70, rnd.uniform(110, 160), rnd.uniform(40, 55), a))
    elif kind == "tall":
        for i in range(11):
            a = -35 + i * 7 + rnd.uniform(-4, 4)
            leaves.append(leaf(rnd.uniform(-6, 6), -70, rnd.uniform(150, 230), rnd.uniform(18, 26), a))
    elif kind == "tulsi":
        # Holy basil in a raised square planter is a common courtyard object; drawn simply as a bushy plant.
        for i in range(16):
            a = -80 + i * 10 + rnd.uniform(-5, 5)
            leaves.append(leaf(rnd.uniform(-10, 10), -75 - rnd.uniform(0, 30), rnd.uniform(50, 80), rnd.uniform(26, 34), a,
                               fill=C["leaf_dark"] if i % 3 == 0 else "url(#gLeaf)"))
    elif kind == "flowering":
        for i in range(10):
            a = -75 + i * 16 + rnd.uniform(-5, 5)
            leaves.append(leaf(rnd.uniform(-6, 6), -70, rnd.uniform(80, 120), rnd.uniform(34, 44), a))
        for i in range(6):
            ang = math.radians(-90 + rnd.uniform(-60, 60))
            r = rnd.uniform(60, 105)
            fx, fy = math.cos(ang) * r, -70 + math.sin(ang) * r
            leaves.append(hibiscus(fx, fy, 15))
    items += leaves
    # pot
    items += [
        path("M-46,-78 L46,-78 L36,0 L-36,0 Z", "url(#gTerracotta)", LW_MID),
        rect(-52, -92, 104, 18, C["terracotta"], rx=6, sw=LW_MID),
        shade([(10, -74), (44, -74), (35, -2), (14, -2)], 0.2),
        path("M-38,-46 Q0,-36 38,-46", sw=1.6, stroke=C["terracotta_dark"]),
    ]
    return group(*items, transform=f"scale({scale:.3f})")


def hibiscus(x, y, r) -> str:
    petals = []
    for i in range(5):
        a = i * 72
        petals.append(ellipse(0, -r * 0.6, r * 0.55, r * 0.7, C["red"], sw=1.5,
                              extra=f'transform="rotate({a})"'))
    return group(*petals, circle(0, 0, r * 0.22, "#f4d35e", sw=1.2), transform=f"translate({f(x)} {f(y)})")


def hanging_plant(rope: float = 120, seed: int = 3) -> str:
    """Origin: ceiling attachment point."""
    rnd = random.Random(seed)
    items = [line(-20, rope - 40, 0, 0, sw=1.6), line(20, rope - 40, 0, 0, sw=1.6)]
    for i in range(9):
        a = 110 + i * 18 + rnd.uniform(-6, 6)
        items.append(leaf(rnd.uniform(-20, 20), rope - 30, rnd.uniform(60, 110), rnd.uniform(22, 30), a))
    items += [
        path(f"M-34,{rope - 44} L34,{rope - 44} Q30,{rope} 0,{rope + 6} Q-30,{rope} -34,{rope - 44} Z", "url(#gTerracotta)", LW_MID),
        ellipse(0, rope - 44, 34, 7, C["terracotta"], sw=LW_SMALL),
    ]
    for i in range(6):
        a = 200 + i * 28 + rnd.uniform(-6, 6)
        items.append(leaf(rnd.uniform(-25, 25), rope - 40, rnd.uniform(50, 80), rnd.uniform(20, 26), a))
    return group(*items)


def coconut_palm(height: float = 520, lean: float = 30, seed: int = 5) -> str:
    """Origin: base of trunk."""
    rnd = random.Random(seed)
    tx, ty = lean, -height
    trunk = path(f"M-16,0 Q{f(lean * 0.2 - 10)},{f(-height * 0.5)} {f(tx - 9)},{f(ty)} L{f(tx + 9)},{f(ty)} "
                 f"Q{f(lean * 0.2 + 12)},{f(-height * 0.5)} 16,0 Z", "#9b7653", LW_MID)
    rings = [path(f"M{f(-13 + lean * (k / 12) ** 1.4)},{f(-height * k / 12)} q12,5 26,0", sw=1.4, stroke="#6d4f35")
             for k in range(1, 12)]
    fronds = []
    for i in range(9):
        a = -170 + i * 22 + rnd.uniform(-6, 6)
        ln = rnd.uniform(170, 230)
        rad = math.radians(a)
        ex, ey = math.cos(rad) * ln, math.sin(rad) * ln * 0.55 + ln * 0.25
        cx, cy = math.cos(rad) * ln * 0.5, math.sin(rad) * ln * 0.5 - 40
        fronds.append(path(f"M0,0 Q{f(cx)},{f(cy)} {f(ex)},{f(ey)} Q{f(cx * 0.9)},{f(cy + 24)} 0,10 Z",
                           C["leaf"] if i % 2 else C["leaf_dark"], LW_SMALL))
        # leaflet hints
        for k in range(1, 7):
            t = k / 7
            px = (1 - t) ** 2 * 0 + 2 * (1 - t) * t * cx + t * t * ex
            py = (1 - t) ** 2 * 0 + 2 * (1 - t) * t * cy + t * t * ey
            fronds.append(line(px, py, px + 10 * math.cos(rad + 1.2), py + 18, sw=1.2, stroke=C["leaf_dark"]))
    nuts = [circle(-10, 14, 11, "#6b8c3a", sw=LW_SMALL), circle(10, 16, 11, "#5d7d33", sw=LW_SMALL),
            circle(0, 24, 11, "#6b8c3a", sw=LW_SMALL)]
    return group(trunk, *rings, group(*nuts, *fronds, transform=f"translate({f(tx)} {f(ty)})"))


def bush(w: float, h: float, seed: int = 7, flowers: str | None = None) -> str:
    """Rounded shrub. Origin bottom-centre."""
    rnd = random.Random(seed)
    blobs = []
    n = max(4, int(w / 45))
    for i in range(n):
        x = -w / 2 + w * (i + 0.5) / n + rnd.uniform(-10, 10)
        r = rnd.uniform(h * 0.42, h * 0.62)
        blobs.append(circle(x, -r * 0.9, r, C["leaf"] if i % 2 else C["leaf_light"], sw=LW_SMALL))
    top = []
    for i in range(n - 1):
        x = -w / 2 + w * (i + 1) / n
        r = rnd.uniform(h * 0.35, h * 0.5)
        top.append(circle(x, -h + r * 0.6, r, C["leaf_light"], sw=LW_SMALL))
    fl = []
    if flowers:
        for i in range(int(w / 30)):
            fx = rnd.uniform(-w / 2 + 15, w / 2 - 15)
            fy = rnd.uniform(-h * 0.95, -h * 0.25)
            if flowers == "hibiscus":
                fl.append(hibiscus(fx, fy, 10))
            else:
                fl.append(circle(fx, fy, 6, flowers, sw=1.2))
    return group(shadow(0, 0, w * 0.55, 14), *blobs, *top, *fl)


def banana_plant(height: float = 300, seed: int = 11) -> str:
    rnd = random.Random(seed)
    items = [shadow(0, 0, 70, 14),
             path(f"M-14,0 L-10,{f(-height * 0.55)} L10,{f(-height * 0.55)} L14,0 Z", "#8aa65a", LW_MID)]
    for i in range(6):
        a = -150 + i * 24 + rnd.uniform(-6, 6)
        ln = rnd.uniform(height * 0.55, height * 0.75)
        items.append(group(
            path(f"M0,0 Q{f(ln * 0.18)},{f(-ln * 0.5)} 0,{f(-ln)} Q{f(-ln * 0.2)},{f(-ln * 0.5)} 0,0 Z",
                 C["leaf_light"] if i % 2 else C["leaf"], LW_SMALL),
            line(0, 0, 0, -ln * 0.95, sw=1.6, stroke="#d6e3a8"),
            transform=f"translate(0 {f(-height * 0.5)}) rotate({f(a + 90)})"))
    return group(*items)


# ---------------------------------------------------------------- architecture

def pillar(h: float, w: float = 34) -> str:
    """Turned wooden pillar on stone base. Origin bottom-centre."""
    return group(
        shadow(0, 0, w * 1.4, 10),
        rect(-w * 0.95, -26, w * 1.9, 26, "#cbb08a", rx=3),
        rect(-w * 0.75, -40, w * 1.5, 16, C["wood_dark"], rx=4),
        rect(-w / 2, -h + 40, w, h - 80, "url(#gWood)", rx=4),
        ellipse(0, -h * 0.55, w * 0.62, 10, C["wood_dark"], sw=LW_SMALL),
        ellipse(0, -h * 0.35, w * 0.62, 10, C["wood_dark"], sw=LW_SMALL),
        rect(-w * 0.8, -h + 26, w * 1.6, 16, C["wood_dark"], rx=4),
        path(f"M{f(-w * 1.25)},{f(-h)} L{f(w * 1.25)},{f(-h)} L{f(w * 0.8)},{f(-h + 26)} L{f(-w * 0.8)},{f(-h + 26)} Z",
             "url(#gWoodDark)", LW_MID),
        shade([(w * 0.1, -h + 42), (w / 2, -h + 42), (w / 2, -42), (w * 0.1, -42)], 0.2),
    )


def wooden_door(w: float, h: float, ajar: float = 0.0, outside: str | None = None, knob=True) -> str:
    """Double-leaf carved wooden door with frame. Origin: top-left of frame. ajar in [0,1] opens the right leaf."""
    fr = 14
    items = [rect(-fr, -fr, w + 2 * fr, h + fr, "url(#gWoodDark)", rx=4, sw=LW_MID)]
    if outside:
        items.append(rect(0, 0, w, h, outside, sw=LW_SMALL))
    else:
        items.append(rect(0, 0, w, h, "#3a2316", sw=LW_SMALL))
    lw = w / 2

    def leaf_panel(x0, width):
        pi = [rect(x0, 0, width, h, "url(#gWood)", sw=LW_MID)]
        for k in range(3):
            py = 16 + k * (h - 32) / 3
            ph = (h - 32) / 3 - 14
            pi.append(rect(x0 + 12, py, width - 24, ph, C["wood"], rx=3, sw=LW_SMALL))
            pi.append(rect(x0 + 20, py + 8, width - 40, ph - 16, "none", rx=2, sw=1.2, stroke=C["wood_dark"]))
        return pi

    items += leaf_panel(0, lw)
    if ajar > 0:
        nw = lw * (1 - ajar * 0.8)
        items.append(poly([(lw, 0), (lw + nw, -10 * ajar), (lw + nw, h + 10 * ajar), (lw, h)], "url(#gWoodDark)", LW_MID))
        items.append(line(lw + nw * 0.85, h * 0.45, lw + nw * 0.85, h * 0.55, sw=6, stroke=C["brass"]))
    else:
        items += leaf_panel(lw, lw)
    if knob:
        items += [circle(lw - 10, h * 0.5, 6, "url(#gBrass)", sw=1.5)]
        if ajar <= 0:
            items.append(circle(lw + 10, h * 0.5, 6, "url(#gBrass)", sw=1.5))
    # lintel carving
    items.append(rect(-fr - 6, -fr - 18, w + 2 * fr + 12, 20, C["wood_dark"], rx=3, sw=LW_MID))
    items.append(path(f"M{f(w * 0.3)},{-fr - 8} Q{f(w * 0.5)},{-fr - 22} {f(w * 0.7)},{-fr - 8}", sw=1.6, stroke=C["brass"]))
    return group(*items)


def window(w: float, h: float, shutters: str = C["teal"], view: str = "leaves", curtains: bool = False) -> str:
    """Wooden window with vertical grill bars. Origin top-left."""
    items = [rect(-12, -12, w + 24, h + 24, "url(#gWoodDark)", rx=3)]
    # view outside
    items.append(rect(0, 0, w, h, "#bfe3d9", sw=LW_SMALL))
    if view == "leaves":
        rnd = random.Random(int(w + h))
        for i in range(10):
            items.append(circle(rnd.uniform(0, w), rnd.uniform(h * 0.3, h * 1.0), rnd.uniform(22, 40),
                                C["leaf_light"] if i % 2 else C["leaf"], sw=None, extra='opacity=".9"'))
    items.append(rect(0, 0, w, h, "none", sw=LW_SMALL))
    for k in range(1, 6):
        x = w * k / 6
        items.append(line(x, 0, x, h, sw=4, stroke="#3d3a3a"))
    items.append(line(w / 2, 0, w / 2, h, sw=6, stroke=C["wood_dark"]))
    if shutters:
        sw_ = w * 0.32
        items.append(poly([(0, 0), (-sw_, -8), (-sw_, h + 8), (0, h)], shutters, LW_MID))
        items.append(poly([(w, 0), (w + sw_, -8), (w + sw_, h + 8), (w, h)], shutters, LW_MID))
        for k in range(1, 6):
            yy = h * k / 6
            items.append(line(-sw_ + 6, yy - 4, -6, yy, sw=1.4, stroke=C["teal_dark"]))
            items.append(line(w + 6, yy, w + sw_ - 6, yy - 4, sw=1.4, stroke=C["teal_dark"]))
    if curtains:
        cw = w * 0.34
        items.append(path(f"M-6,-6 L{f(cw)},-6 Q{f(cw * 0.6)},{f(h * 0.5)} {f(cw * 0.9)},{f(h + 6)} L-6,{f(h + 6)} Z",
                          "#f3e3c3", LW_SMALL))
        items.append(path(f"M{f(w + 6)},-6 L{f(w - cw)},-6 Q{f(w - cw * 0.6)},{f(h * 0.5)} {f(w - cw * 0.9)},{f(h + 6)} L{f(w + 6)},{f(h + 6)} Z",
                          "#f3e3c3", LW_SMALL))
        rnd = random.Random(4)
        for i in range(14):
            side = i % 2
            x = rnd.uniform(4, cw * 0.65) if not side else rnd.uniform(w - cw * 0.65, w - 4)
            items.append(circle(x, rnd.uniform(10, h - 10), 5, C["terracotta"], sw=None, extra='opacity=".75"'))
        items.append(rect(-20, -22, w + 40, 10, C["brass_dark"], rx=5, sw=LW_SMALL))
    items.append(rect(-16, h + 10, w + 32, 12, "url(#gWoodDark)", rx=3))
    return group(*items)


def kolam(r: float = 120, color: str = C["white"]) -> str:
    """Flat dot-and-loop kolam centred at origin; squash vertically for floor perspective."""
    items = []
    for i in range(-2, 3):
        for j in range(-2, 3):
            if abs(i) + abs(j) <= 3:
                items.append(circle(i * r / 3, j * r / 3, r * 0.025, color, sw=None))
    for k in range(8):
        a = k * 45
        items.append(path(f"M0,0 Q{f(r * 0.3)},{f(-r * 0.55)} 0,{f(-r * 0.95)} Q{f(-r * 0.3)},{f(-r * 0.55)} 0,0",
                          sw=r * 0.03, stroke=color, extra=f'transform="rotate({a})"'))
    items.append(circle(0, 0, r * 0.42, "none", sw=r * 0.03, stroke=color))
    items.append(circle(0, 0, r * 0.98, "none", sw=r * 0.025, stroke=color, extra=f'stroke-dasharray="{f(r * 0.08)} {f(r * 0.06)}"'))
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        items.append(circle(math.cos(a) * r * 0.7, math.sin(a) * r * 0.7, r * 0.08, "none", sw=r * 0.025, stroke=color))
    return group(*items)


def brass_lamp(h: float = 150) -> str:
    """Kuthu vilakku (standing brass oil lamp), unlit. Origin bottom-centre."""
    return group(
        shadow(0, 0, 40, 8),
        ellipse(0, -8, 38, 10, "url(#gBrass)", LW_SMALL),
        path("M-30,-10 Q0,-40 30,-10 Z", "url(#gBrass)", LW_SMALL),
        rect(-6, -h * 0.75, 12, h * 0.75 - 26, "url(#gBrass)", sw=LW_SMALL),
        ellipse(0, -h * 0.45, 13, 7, "url(#gBrass)", LW_SMALL),
        path(f"M-40,{f(-h * 0.75)} Q0,{f(-h * 0.62)} 40,{f(-h * 0.75)} L30,{f(-h * 0.78)} L-30,{f(-h * 0.78)} Z", "url(#gBrass)", LW_SMALL),
        path(f"M-8,{f(-h * 0.8)} Q0,{f(-h - 6)} 8,{f(-h * 0.8)} Z", "url(#gBrass)", LW_SMALL),
    )


# ---------------------------------------------------------------- furniture & objects

def table(w: float, h: float, top_depth: float = 30, fill="url(#gWoodV)") -> str:
    """Front-facing wooden table. Origin: bottom-centre between front legs."""
    items = [shadow(0, 2, w * 0.55, 14)]
    items += [rect(-w / 2 + 10, -h, 20, h, "url(#gWoodDark)", sw=LW_MID),
              rect(w / 2 - 30, -h, 20, h, "url(#gWoodDark)", sw=LW_MID)]
    items.append(poly([(-w / 2 - 6, -h - top_depth), (w / 2 + 6, -h - top_depth), (w / 2 + 16, -h), (-w / 2 - 16, -h)], fill, LW_MID))
    items.append(rect(-w / 2 - 16, -h, w + 32, 18, C["wood"], rx=2, sw=LW_MID))
    items.append(rect(-w / 2 + 30, -h + 18, w - 60, 40, C["wood_light"], sw=LW_SMALL))
    items.append(circle(0, -h + 38, 5, "url(#gBrass)", sw=1.2))
    return group(*items)


def sewing_machine(scale: float = 1.0) -> str:
    """Black hand-crank/treadle-style sewing machine head with gold accents. Origin bottom-centre (on table top)."""
    items = [
        shadow(0, 0, 150, 12, 0.3),
        rect(-150, -22, 300, 22, C["wood_dark"], rx=4, sw=LW_MID),  # base board
        # arm and head
        path("M-120,-22 L-120,-150 Q-120,-180 -90,-180 L110,-180 Q140,-180 140,-150 L140,-120 L100,-120 L100,-150 L-80,-150 L-80,-22 Z",
             "#22201f", LW_MID),
        rect(100, -130, 46, 108, "#22201f", rx=8, sw=LW_MID),  # head
        rect(112, -22 - 30, 12, 30, C["steel"], sw=LW_SMALL),  # needle bar
        line(118, -22, 118, -6, sw=2, stroke="#666"),
        circle(-128, -100, 44, "#2c2a29", sw=LW_MID),  # hand wheel
        circle(-128, -100, 30, "none", sw=2, stroke=C["brass"]),
        circle(-128, -100, 8, C["brass"], sw=1.4),
        # gold decals (abstract scrolls, no lettering)
        path("M-60,-168 q20,-8 40,0 t40,0 t40,0", sw=2.4, stroke=C["brass"]),
        path("M-100,-120 q8,-14 0,-28", sw=2.4, stroke=C["brass"]),
        rect(60, -196, 10, 16, C["steel"], sw=1.4),  # spool pin
        ellipse(65, -204, 16, 8, C["red"], sw=1.6),  # thread spool
        line(65, -204, 118, -60, sw=1, stroke=C["red"]),
    ]
    return group(*items, transform=f"scale({scale:.3f})")


def fabric_stack(w: float = 160, colors=None) -> str:
    """Folded fabrics stacked. Origin bottom-centre."""
    colors = colors or [C["indigo"], C["maroon"], "#e0a83a", C["teal"], "#d0607a"]
    items = [shadow(0, 0, w * 0.6, 8)]
    y = 0
    for i, col in enumerate(colors):
        hh = 22
        items.append(rect(-w / 2 + (i % 2) * 6, y - hh, w - 6, hh, col, rx=6, sw=LW_SMALL))
        items.append(line(-w / 2 + 12, y - hh / 2, w / 2 - 18, y - hh / 2, sw=1.4, stroke="#ffffff",
                          extra='opacity=".35" stroke-dasharray="6 6"'))
        y -= hh
    return group(*items)


def radio(scale: float = 1.0) -> str:
    """Vintage wooden valve radio with cloth grille. Origin bottom-centre. ~300 wide x 210 tall."""
    items = [
        shadow(0, 0, 160, 12, 0.32),
        path("M-150,0 L-150,-150 Q-150,-210 -90,-210 L90,-210 Q150,-210 150,-150 L150,0 Z", "url(#gWood)", LW_BIG),
        path("M-132,-12 L-132,-146 Q-132,-192 -86,-192 L86,-192 Q132,-192 132,-146 L132,-12 Z", C["wood_dark"], LW_SMALL),
        # grille cloth
        path("M-118,-26 L-118,-140 Q-118,-178 -80,-178 L20,-178 L20,-26 Z", "#d9b98a", LW_SMALL),
    ]
    for k in range(9):
        x = -110 + k * 14
        items.append(line(x, -170 if k > 2 else -150, x, -32, sw=3, stroke=C["wood_dark"], extra='opacity=".55"'))
    items += [
        # dial window
        rect(34, -168, 86, 44, "#f6e2a6", rx=8, sw=LW_SMALL),
        line(46, -146, 108, -146, sw=1.4, stroke=C["wood_dark"]),
        *[line(46 + i * 10, -156, 46 + i * 10, -150, sw=1.2, stroke=C["wood_dark"]) for i in range(7)],
        line(80, -164, 80, -130, sw=2.2, stroke=C["red"]),
        # knobs
        circle(56, -74, 22, "url(#gBrass)", LW_SMALL), circle(56, -74, 8, C["brass_dark"], sw=1.2),
        circle(104, -74, 22, "url(#gBrass)", LW_SMALL), circle(104, -74, 8, C["brass_dark"], sw=1.2),
        rect(-150, -8, 300, 10, C["wood_dark"], rx=3, sw=LW_SMALL),
        rect(-136, 0, 22, 8, C["wood_dark"], sw=1.4), rect(114, 0, 22, 8, C["wood_dark"], sw=1.4),
        shade([(60, -205), (148, -150), (148, -10), (120, -10), (120, -180)], 0.12),
    ]
    return group(*items, transform=f"scale({scale:.3f})")


def memory_book_open(scale: float = 1.0) -> str:
    """Open photo album lying on a desk, mild perspective. Origin bottom-centre. ~330 wide x 120 tall."""
    items = [
        shadow(0, 2, 175, 16, 0.3),
        # cover
        poly([(-170, 0), (170, 0), (150, -112), (-150, -112)], C["maroon"], LW_MID),
        # pages
        poly([(-158, -10), (-4, -6), (-6, -106), (-142, -104)], C["white"], LW_SMALL),
        poly([(4, -6), (158, -10), (142, -104), (6, -106)], "#fbf3e2", LW_SMALL),
        line(0, -4, 0, -108, sw=LW_MID),
        # photo cards (abstract scenes, no people/text)
        poly([(-140, -24), (-82, -22), (-84, -62), (-136, -64)], "#9fd3e6", LW_SMALL),
        poly([(-140, -40), (-82, -36), (-82, -22), (-140, -24)], C["leaf"], sw=None),
        poly([(-70, -22), (-16, -20), (-18, -60), (-68, -62)], "#f2c879", LW_SMALL),
        circle(-42, -44, 10, C["terracotta"], sw=1.2),
        poly([(20, -20), (76, -22), (78, -64), (24, -62)], "#d9eecf", LW_SMALL),
        poly([(34, -26), (52, -58), (68, -26)], C["leaf_dark"], sw=1.2),
        poly([(88, -22), (140, -24), (134, -62), (88, -60)], "#f3d6c4", LW_SMALL),
        path("M96,-34 q20,-30 36,0", sw=2, stroke=C["maroon"]),
        # ribbon bookmark
        path("M-10,-6 L-14,18 L-6,10 L2,18 L-2,-6 Z", "#e0a83a", LW_SMALL),
    ]
    return group(*items, transform=f"scale({scale:.3f})")


def table_lamp(scale: float = 1.0) -> str:
    """Origin bottom-centre. ~120 tall glow is separate."""
    return group(
        shadow(0, 0, 40, 8),
        ellipse(0, -6, 34, 9, "url(#gBrass)", LW_SMALL),
        rect(-5, -96, 10, 90, "url(#gBrass)", sw=LW_SMALL),
        path("M-52,-96 L52,-96 L32,-170 L-32,-170 Z", "#f6dca0", LW_MID),
        path("M-52,-96 L52,-96", sw=LW_SMALL, stroke=C["brass_dark"]),
        transform=f"scale({scale:.3f})",
    )


def picture_frame(w: float, h: float, kind: str = "hills") -> str:
    """Framed painting of a landscape (no people, no text). Origin top-left."""
    items = [rect(-10, -10, w + 20, h + 20, "url(#gWoodDark)", rx=2, sw=LW_MID),
             rect(0, 0, w, h, "#bfe0e8", sw=LW_SMALL)]
    if kind == "hills":
        items += [path(f"M0,{f(h * 0.7)} Q{f(w * 0.3)},{f(h * 0.3)} {f(w * 0.55)},{f(h * 0.6)} Q{f(w * 0.75)},{f(h * 0.4)} {f(w)},{f(h * 0.62)} L{f(w)},{f(h)} L0,{f(h)} Z",
                       C["leaf"], sw=1.4),
                  circle(w * 0.78, h * 0.25, h * 0.1, "#f4d35e", sw=1.2)]
    elif kind == "sea":
        items += [rect(0, h * 0.55, w, h * 0.45, "#4f8fbf", sw=None),
                  path(f"M{f(w * 0.15)},{f(h)} L{f(w * 0.2)},{f(h * 0.35)}", sw=5, stroke="#8a6a45"),
                  *[leaf(w * 0.2, h * 0.35, h * 0.28, h * 0.1, a) for a in (-120, -70, -20, 30, 80)]]
    elif kind == "flowers":
        items += [rect(0, 0, w, h, "#f6ead2", sw=None),
                  ellipse(w / 2, h * 0.8, w * 0.22, h * 0.12, C["terracotta"], sw=1.4)]
        for i in range(5):
            items.append(hibiscus(w * (0.3 + i * 0.1), h * (0.45 + (i % 2) * 0.12), h * 0.09))
    items.append(rect(0, 0, w, h, "none", sw=LW_SMALL))
    return group(*items)


def steel_shelf(w: float = 200) -> str:
    """Wall shelf with stainless-steel and brass vessels. Origin: left end of shelf top."""
    items = [rect(0, 0, w, 14, "url(#gWoodV)", rx=2, sw=LW_SMALL)]
    x = 14
    k = 0
    while x < w - 30:
        if k % 3 == 0:
            items.append(path(f"M{f(x)},0 L{f(x + 4)},-34 L{f(x + 30)},-34 L{f(x + 34)},0 Z", "url(#gSteel)", LW_SMALL))
            x += 44
        elif k % 3 == 1:
            items.append(path(f"M{f(x)},0 Q{f(x - 6)},-30 {f(x + 14)},-40 Q{f(x + 34)},-30 {f(x + 28)},0 Z", "url(#gBrass)", LW_SMALL))
            x += 40
        else:
            items.append(rect(x, -26, 24, 26, "url(#gSteel)", rx=3, sw=LW_SMALL))
            x += 34
        k += 1
    return group(*items)


def chair(scale: float = 1.0) -> str:
    """Simple wooden chair, three-quarter view. Origin bottom-centre."""
    return group(
        shadow(0, 0, 70, 10),
        rect(-50, -90, 10, 90, C["wood_dark"], sw=LW_SMALL), rect(40, -90, 10, 90, C["wood_dark"], sw=LW_SMALL),
        rect(-36, -100, 8, 80, C["wood_dark"], sw=LW_SMALL), rect(28, -100, 8, 80, C["wood_dark"], sw=LW_SMALL),
        poly([(-56, -96), (56, -96), (44, -120), (-40, -120)], "url(#gWood)", LW_MID),
        rect(-44, -230, 12, 112, "url(#gWood)", sw=LW_SMALL), rect(32, -230, 12, 112, "url(#gWood)", sw=LW_SMALL),
        rect(-48, -236, 96, 22, "url(#gWood)", rx=4, sw=LW_SMALL),
        rect(-34, -200, 68, 60, "#e9d3a3", sw=LW_SMALL, extra='opacity=".95"'),
        *[line(-30 + i * 10, -198, -30 + i * 10, -142, sw=1, stroke=C["wood_dark"]) for i in range(7)],
        transform=f"scale({scale:.3f})",
    )


def cupboard(w: float = 220, h: float = 420) -> str:
    """Wooden almirah. Origin bottom-centre."""
    return group(
        shadow(0, 0, w * 0.55, 12),
        rect(-w / 2, -h, w, h, "url(#gWood)", rx=4, sw=LW_BIG),
        rect(-w / 2 - 10, -h - 16, w + 20, 18, C["wood_dark"], rx=3, sw=LW_MID),
        rect(-w / 2 + 14, -h + 16, w / 2 - 20, h - 60, C["wood"], rx=3, sw=LW_SMALL),
        rect(6, -h + 16, w / 2 - 20, h - 60, C["wood"], rx=3, sw=LW_SMALL),
        rect(-w / 2 + 28, -h + 36, w / 2 - 48, h - 100, "none", rx=2, sw=1.4, stroke=C["wood_dark"]),
        rect(20, -h + 36, w / 2 - 48, h - 100, "none", rx=2, sw=1.4, stroke=C["wood_dark"]),
        circle(-10, -h / 2, 6, "url(#gBrass)", sw=1.2), circle(10, -h / 2, 6, "url(#gBrass)", sw=1.2),
        shade([(w * 0.2, -h + 4), (w / 2 - 4, -h + 4), (w / 2 - 4, -4), (w * 0.2, -4)], 0.15),
    )


def rug(w: float, h: float) -> str:
    """Flat woven floor mat in perspective (trapezoid). Origin centre."""
    return group(
        poly([(-w * 0.42, -h / 2), (w * 0.42, -h / 2), (w / 2, h / 2), (-w / 2, h / 2)], C["indigo_light"], LW_SMALL),
        poly([(-w * 0.36, -h * 0.36), (w * 0.36, -h * 0.36), (w * 0.43, h * 0.36), (-w * 0.43, h * 0.36)], "#e0a83a", sw=1.4),
        poly([(-w * 0.3, -h * 0.22), (w * 0.3, -h * 0.22), (w * 0.35, h * 0.22), (-w * 0.35, h * 0.22)], C["maroon"], sw=1.4),
    )
