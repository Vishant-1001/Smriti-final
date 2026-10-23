"""Scenes for Pass A: kitchen, sewing corner, bus stop, bus interior, market entrance and four stalls.

Same conventions as scenes.py: hotspots are computed from the drawing coordinates.
"""
from __future__ import annotations

import random

import items as I
import people
import props as P
from svgkit import (C, LW_BIG, LW_MID, LW_SMALL, Box, Scene, circle, ellipse, f, group, line, path, poly, rect,
                    shade, shadow)


def _size(variant):
    return (1920, 1080) if variant == "land" else (1080, 2400)


def _room(s: Scene, wall_top: str, wall_bottom: str, floor: str, horizon: float, tiles: bool = False):
    W, H = s.width, s.height
    s.add(rect(0, 0, W, horizon, wall_top, sw=None))
    if tiles:
        s.add(rect(0, horizon * 0.45, W, horizon * 0.55, wall_bottom, sw=None))
        for x in range(0, W, 60):
            s.add(line(x, horizon * 0.45, x, horizon, sw=1.2, stroke="#c9b48c"))
        y = horizon * 0.45
        while y < horizon:
            s.add(line(0, y, W, y, sw=1.2, stroke="#c9b48c"))
            y += 60
    s.add(rect(0, horizon, W, H - horizon, floor, sw=None))
    s.add(line(0, horizon, W, horizon, sw=LW_MID))


# ================================================================ kitchen

def kitchen(variant: str) -> Scene:
    W, H = _size(variant)
    s = Scene("kitchen", variant, W, H, background=C["cream"])
    land = variant == "land"
    counter_y = 640 if land else 1150
    _room(s, C["ochre_wall"], "#f4ead2", "#b4532f", counter_y + 200, tiles=True)
    # window + shelf
    win = Box(W / 2 - 170, 120 if land else 380, 340, 240)
    s.add(ellipse(win.x + win.w / 2, win.y + win.h, 420, 260, "url(#gWarmLight)", sw=None))
    s.add(group(P.window(win.w, win.h, shutters=C["teal"]), transform=f"translate({f(win.x)} {f(win.y)})"))
    s.add(group(P.steel_shelf(360), transform=f"translate({60 if land else 40} {260 if land else 960})"))
    s.add(group(P.steel_shelf(360), transform=f"translate({W - 420 if land else W - 400} {260 if land else 1160})"))
    s.add(group(P.potted_plant("tulsi", 0.6, seed=4), transform=f"translate({f(win.x + 60)} {f(win.y + win.h + 22)})"))
    # counter
    s.add(rect(0, counter_y, W, 46, "#d9c39a", sw=LW_MID))
    s.add(rect(0, counter_y + 46, W, 200, "url(#gWoodV)", sw=LW_MID))
    for k in range(1, 6):
        x = W * k / 6
        s.add(line(x, counter_y + 60, x, counter_y + 236, sw=LW_SMALL, stroke=C["wood_dark"]))
    # cooking area: stove + filter + tumbler + milk + powder
    cx = W * (0.27 if land else 0.5)
    sc = 1.0 if land else 1.3
    cook = []
    cook.append(rect(-200, -60, 400, 60, "#2c2a29", rx=8, sw=LW_MID))
    cook += [ellipse(-100, -60, 70, 16, "#4a4644", LW_SMALL), ellipse(100, -60, 70, 16, "#4a4644", LW_SMALL)]
    cook.append(group(I.filter_vessel("powder"), transform="translate(-100 -170) scale(0.85)"))
    cook.append(group(I.tumbler("coffee"), transform="translate(100 -150) scale(0.8)"))
    cook.append(group(I.icon_coffee_powder(), transform="translate(-260 -60) scale(0.6)"))
    cook.append(group(I.milk_pot(), transform="translate(270 -60) scale(0.6)"))
    s.add(group(*cook, transform=f"translate({f(cx)} {counter_y + 10}) scale({sc})"))
    cook_box = Box(cx - 320 * sc, counter_y - 290 * sc, 640 * sc, 330 * sc)
    # sink area with dishes
    kx = W * (0.73 if land else 0.5)
    if not land:
        # a second (front) counter for the sink in the tall layout
        sink_y = 1850
        s.add(rect(0, sink_y, W, 46, "#d9c39a", sw=LW_MID))
        s.add(rect(0, sink_y + 46, W, 230, "url(#gWoodV)", sw=LW_MID))
    else:
        sink_y = counter_y
    sink = [rect(-230, -10, 460, 40, "#9aa3a8", rx=8, sw=LW_MID), rect(-200, -6, 400, 26, "#6f787d", rx=6, sw=LW_SMALL),
            path("M150,-10 L150,-120 Q150,-150 110,-150 L80,-150", sw=12, stroke=C["steel_dark"]),
            path("M150,-10 L150,-120 Q150,-150 110,-150 L80,-150", sw=LW_SMALL),
            group(I.plate(True), transform="translate(-110 -36) scale(0.6)"),
            group(I.plate(True), transform="translate(-100 -56) scale(0.6)"),
            group(I.cup(True), transform="translate(40 -60) scale(0.5)"),
            group(I.bowl(True), transform="translate(-200 -40) scale(0.4)")]
    s.add(group(*sink, transform=f"translate({f(kx)} {sink_y}) scale({sc})"))
    sink_box = Box(kx - 250 * sc, sink_y - 190 * sc, 500 * sc, 240 * sc)
    s.hotspot("cook_area", cook_box.grow(10))
    s.hotspot("sink", sink_box.grow(10))
    return s


# ================================================================ sewing corner

def sewing(variant: str) -> Scene:
    W, H = _size(variant)
    land = variant == "land"
    s = Scene("sewing", variant, W, H, background=C["cream"])
    s.defs.append(I.fabric_defs())
    floor_y = 860 if land else 1880
    _room(s, C["ochre_wall"], C["ochre_wall"], "#b4532f", floor_y)
    s.add(rect(0, floor_y - 70, W, 70, "#d6a86a", sw=LW_SMALL, extra='opacity=".7"'))
    win = Box(W * (0.28 if land else 0.2), 110 if land else 420, 300, 230)
    s.add(ellipse(win.x + 150, win.y + 300, 420, 300, "url(#gWarmLight)", sw=None))
    s.add(group(P.window(win.w, win.h, shutters=None, curtains=True), transform=f"translate({f(win.x)} {f(win.y)})"))
    # sewing table + machine
    tx = W * (0.36 if land else 0.42)
    tsc = 1.15 if land else 1.25
    s.add(group(P.table(420, 200), group(P.sewing_machine(1.0), transform="translate(-10 -230)"),
                group(P.fabric_stack(130, ["#f1e2c0", C["teal"], "#e0a83a"]), transform="translate(170 -230)"),
                transform=f"translate({f(tx)} {f(floor_y + 20)}) scale({tsc})"))
    machine_box = Box(tx - 250 * tsc, floor_y + 20 - 460 * tsc, 500 * tsc, 470 * tsc)
    s.add(group(P.chair(0.9), transform=f"translate({f(tx + 30)} {f(floor_y + 180)})"))
    # creations rack (wooden rack with empty pegs; creations are drawn by the app)
    rx_ = W * (0.8 if land else 0.73)
    ry = 260 if land else 520
    rw, rh = (380, 470) if land else (420, 480)
    rack = [rect(-rw / 2, 0, rw, rh, "#e9d1a6", rx=10, sw=LW_MID), rect(-rw / 2, 0, rw, 24, C["wood_dark"], rx=6, sw=LW_MID),
            rect(-rw / 2 + 20, rh / 2, rw - 40, 14, C["wood"], rx=4, sw=LW_SMALL),
            rect(-rw / 2 - 10, rh, rw + 20, 26, C["wood_dark"], rx=6, sw=LW_MID)]
    rack += [circle(-rw / 2 + 60 + k * (rw - 120) / 2, 50, 10, C["brass"], sw=1.4) for k in range(3)]
    s.add(group(*rack, transform=f"translate({f(rx_)} {f(ry)})"))
    s.hotspot("sewing_machine", machine_box.grow(8))
    s.hotspot("creations_rack", Box(rx_ - rw / 2, ry, rw, rh + 26).grow(10))
    # fabric shelf (decor)
    s.add(group(P.potted_plant("broad", 1.1, seed=5), transform=f"translate({W - 120 if land else 120} {f(floor_y + 60)})"))
    return s


# ================================================================ bus stop

BUS_COLOURS = [("#e0a83a", "#a8761a"), ("#3c8a6a", "#24604a"), ("#4f7fbf", "#2e5a8f"), ("#c8643b", "#8f3e22")]


def bus_front(fill: str, dark: str) -> str:
    """Front of a city bus. Local 300 wide x 340 tall, origin top-left. The number board (top) is blank."""
    return group(
        shadow(150, 340, 170, 16),
        rect(0, 0, 300, 330, fill, rx=26, sw=LW_BIG),
        rect(30, 18, 240, 56, "#1f1f22", rx=8, sw=LW_MID),               # number board: app draws the number here
        rect(22, 88, 256, 120, "#bfe3e8", rx=10, sw=LW_MID),             # windscreen
        path("M30,96 L120,200", sw=4, stroke="#ffffff", extra='opacity=".5"'),
        rect(0, 220, 300, 18, dark, sw=None),
        circle(52, 270, 20, "#f7e7a0", LW_SMALL), circle(248, 270, 20, "#f7e7a0", LW_SMALL),
        rect(100, 258, 100, 26, "#d8d8d8", rx=6, sw=LW_SMALL),
        rect(20, 318, 50, 30, "#222", rx=6, sw=LW_SMALL), rect(230, 318, 50, 30, "#222", rx=6, sw=LW_SMALL),
    )


def bus_stop(variant: str) -> Scene:
    W, H = _size(variant)
    land = variant == "land"
    s = Scene("bus_stop", variant, W, H, background=C["sky_bottom"])
    horizon = 560 if land else 1100
    s.add(rect(0, 0, W, horizon, "url(#gSky)", sw=None))
    rnd = random.Random(31)
    for i in range(12):
        x = i * W / 11
        s.add(circle(x, horizon - 30, rnd.uniform(70, 110), C["leaf_dark"] if i % 2 else "#4f7a42", sw=LW_SMALL))
    s.add(group(P.coconut_palm(420, 30, seed=3), transform=f"translate({W - 160} {horizon + 20})"))
    s.add(rect(0, horizon, W, H - horizon, "#a9a39a", sw=None))                # road
    s.add(rect(0, horizon, W, 60, "#cbbf9f", sw=LW_SMALL))                    # pavement
    for x in range(40, W, 220):
        s.add(rect(x, horizon + (300 if land else 900), 120, 14, "#f4efe2", sw=None))
    # shelter
    shx, shy = (90, horizon - 260) if land else (60, horizon - 300)
    s.add(rect(shx, shy, 360, 26, C["indigo"], rx=6, sw=LW_MID))
    s.add(rect(shx + 20, shy + 26, 14, 290, "#4a4a4a", sw=LW_SMALL))
    s.add(rect(shx + 326, shy + 26, 14, 290, "#4a4a4a", sw=LW_SMALL))
    s.add(rect(shx + 40, shy + 180, 280, 24, C["wood"], rx=4, sw=LW_SMALL))
    s.add(rect(shx + 380, shy - 60, 12, 380, "#4a4a4a", sw=LW_SMALL))
    s.add(rect(shx + 346, shy - 110, 80, 60, C["indigo"], rx=8, sw=LW_MID))   # blank sign (no text)
    s.add(circle(shx + 386, shy - 80, 16, "#f4efe2", sw=LW_SMALL))
    # four buses
    if land:
        positions = [(660 + k * 312, horizon - 90) for k in range(4)]
        bs = 1.0
    else:
        positions = [(120 + (k % 2) * 450, 1180 + (k // 2) * 470) for k in range(4)]
        bs = 1.2
    for k, (bx, by) in enumerate(positions):
        fill, dark = BUS_COLOURS[k]
        s.add(group(bus_front(fill, dark), transform=f"translate({bx} {by}) scale({bs})"))
        s.hotspot(f"bus_{k + 1}", Box(bx, by, 300 * bs, 330 * bs).grow(6))
    return s


# ================================================================ bus interior

def bus_interior(variant: str) -> Scene:
    W, H = _size(variant)
    land = variant == "land"
    s = Scene("bus", variant, W, H, background="#d9d2c3")
    # ceiling and walls
    s.add(rect(0, 0, W, H, "#e8e1cf", sw=None))
    s.add(poly([(0, 0), (W, 0), (W * 0.62, H * 0.22), (W * 0.38, H * 0.22)], "#f2ecdc", LW_MID))
    # windows on both sides (perspective strips)
    for side in (-1, 1):
        x_out = 0 if side < 0 else W
        x_in = W * 0.38 if side < 0 else W * 0.62
        pts = [(x_out, H * 0.12), (x_in, H * 0.26), (x_in, H * 0.44), (x_out, H * 0.62)]
        s.add(poly(pts, "#bfe3d9", LW_MID))
        rnd = random.Random(side + 5)
        for i in range(7):
            t = rnd.random()
            x = x_out + (x_in - x_out) * t
            y = H * (0.3 + 0.12 * (1 - t))
            s.add(circle(x, y, 40 + 40 * (1 - t), C["leaf_light"] if i % 2 else C["leaf"], sw=None, extra='opacity=".9"'))
        s.add(poly(pts, "none", LW_MID))
        for k in range(1, 4):
            t = k / 4
            s.add(line(x_out + (x_in - x_out) * t, H * (0.12 + 0.14 * t), x_out + (x_in - x_out) * t, H * (0.62 - 0.18 * t), sw=8, stroke="#8e989f"))
    # aisle floor
    s.add(poly([(W * 0.38, H * 0.44), (W * 0.62, H * 0.44), (W * 0.8, H), (W * 0.2, H)], "#8e8a82", LW_MID))
    # yellow poles
    for x in (W * 0.33, W * 0.67):
        s.add(rect(x - 10, 0, 20, H * 0.82, "#e8b93a", sw=LW_SMALL))
    # conductor in the aisle (lower body hidden by the seat backs)
    cs = 0.9 if land else 1.15
    cx, cy = W * 0.5 - 256 * cs, H * (0.24 if land else 0.3)
    s.add(group(people.bust(people.CONDUCTOR), transform=f"translate({f(cx)} {f(cy)}) scale({cs})"))
    conductor_box = Box(cx + 120 * cs, cy + 60 * cs, 272 * cs, 420 * cs)
    # passengers' heads in seats ahead and seat backs
    seat_y = H * (0.66 if land else 0.62)
    for side in (-1, 1):
        for k in range(2):
            x = W * 0.5 + side * W * (0.3 + 0.12 * k)
            y = seat_y + k * 120
            s.add(circle(x, y - 40, 50, C["hair"] if (k + side) % 2 else "#9a948c", LW_MID))
            s.add(rect(x - 140, y, 280, 220, "#3c5ca0", rx=30, sw=LW_MID))
            s.add(rect(x - 140, y, 280, 40, "#2e4a86", rx=20, sw=LW_SMALL))
            s.add(rect(x - 150, y - 20, 300, 24, "#c9cfd3", rx=10, sw=LW_SMALL))
    # the player's seat back right in front, bottom
    s.add(rect(W * 0.1, H * 0.86, W * 0.8, H * 0.2, "#3c5ca0", rx=40, sw=LW_BIG))
    s.add(rect(W * 0.1, H * 0.86, W * 0.8, 50, "#c9cfd3", rx=20, sw=LW_MID))
    s.hotspot("conductor", conductor_box)
    return s


# ================================================================ market entrance

STALLS = [
    ("stall_produce", "#c8643b", "#f4efe2", ["tomato", "banana", "brinjal", "onion"]),
    ("stall_spice", "#e0a83a", "#8e2f3c", ["turmeric", "chilli_powder", "rice", "coffee_powder"]),
    ("stall_flower", "#d0607a", "#f4efe2", ["jasmine", "marigold", "rose"]),
    ("stall_cloth", C["indigo"], "#e0a83a", ["cloth_indigo_dots", "cloth_maroon_checks", "cloth_leaf_print"]),
]


def awning(w: float, c1: str, c2: str) -> str:
    stripes = []
    n = 8
    for k in range(n):
        stripes.append(poly([(-w / 2 + k * w / n, 0), (-w / 2 + (k + 1) * w / n, 0), (-w / 2 + (k + 1) * w / n + 10, 70), (-w / 2 + k * w / n + 10, 70)],
                            c1 if k % 2 == 0 else c2, LW_SMALL))
    scallops = [path(f"M{-w / 2 + k * w / n + 10},70 q{w / n / 2},30 {w / n},0", c1 if k % 2 == 0 else c2, LW_SMALL) for k in range(n)]
    return group(*stripes, *scallops)


def stall_front(stall_id, c1, c2, goods, w=380) -> str:
    """Small stall seen from the street, for the market entrance. Origin bottom-centre."""
    g = [shadow(0, 0, w * 0.6, 14),
         rect(-w / 2, -330, w, 330, "#e9d1a6", sw=LW_MID),
         rect(-w / 2 + 20, -310, w - 40, 170, "#c9a776", sw=LW_SMALL),
         group(awning(w + 40, c1, c2), transform="translate(0 -400)"),
         rect(-w / 2 - 10, -140, w + 20, 140, "url(#gWoodV)", sw=LW_MID)]
    for k, item in enumerate(goods[:3]):
        g.append(group(I.pile(item, 120), transform=f"translate({-w / 3 + k * w / 3} -132)"))
    return group(*g)


def market(variant: str) -> Scene:
    W, H = _size(variant)
    land = variant == "land"
    s = Scene("market", variant, W, H, background=C["sky_bottom"])
    s.defs.append(I.fabric_defs())
    horizon = 520 if land else 900
    s.add(rect(0, 0, W, horizon, "url(#gSky)", sw=None))
    for i in range(10):
        s.add(circle(i * W / 9, horizon - 20, 90, "#4f7a42" if i % 2 else C["leaf_dark"], sw=LW_SMALL))
    s.add(rect(0, horizon, W, H - horizon, "url(#gSand)", sw=None))
    s.add(poly([(W * 0.42, horizon), (W * 0.58, horizon), (W * 0.9, H), (W * 0.1, H)], "#d9bf8a", sw=LW_SMALL))
    if land:
        spots = [(330, 900, 1.0), (760, 720, 0.8), (1160, 720, 0.8), (1590, 900, 1.0)]
    else:
        spots = [(280, 1400, 1.15), (800, 1400, 1.15), (280, 1960, 1.15), (800, 1960, 1.15)]
    for (sid, c1, c2, goods), (x, y, sc) in zip(STALLS, spots):
        s.add(group(stall_front(sid, c1, c2, goods), transform=f"translate({x} {y}) scale({sc})"))
        s.hotspot(sid, Box(x - 210 * sc, y - 410 * sc, 420 * sc, 420 * sc).grow(4))
    # arch frame (no lettering)
    if land:
        s.add(rect(40, 60, 60, H - 60, C["terracotta"], sw=LW_MID))
        s.add(rect(W - 100, 60, 60, H - 60, C["terracotta"], sw=LW_MID))
        s.add(path(f"M40,90 Q{W / 2},-40 {W - 40},90 L{W - 40},140 Q{W / 2},10 40,140 Z", C["terracotta"], LW_MID))
    return s


# ================================================================ stall interiors

def stall(variant: str, stall_index: int) -> Scene:
    sid, c1, c2, goods = STALLS[stall_index]
    W, H = _size(variant)
    land = variant == "land"
    s = Scene(sid, variant, W, H, background="#e9d1a6")
    s.defs.append(I.fabric_defs())
    wall = ["#f0d9a8", "#e9cf9a", "#f4e0d8", "#dfe4ee"][stall_index]
    s.add(rect(0, 0, W, H, wall, sw=None))
    # shelves behind the vendor with decorative stock
    for r in range(3):
        y = (180 + r * 150) if land else (520 + r * 200)
        s.add(rect(40, y, W - 80, 18, "url(#gWoodV)", rx=4, sw=LW_SMALL))
        for k in range(10 if land else 6):
            x = 100 + k * ((W - 200) / (9 if land else 5))
            if stall_index == 3:
                s.add(group(I.folded_fabric(list(I.FABRICS)[(k + r) % 4], 120, 36), transform=f"translate({f(x)} {y - 22})"))
            elif stall_index == 1:
                s.add(rect(x - 26, y - 70, 52, 70, "#f6efe0", rx=8, sw=LW_SMALL))
                s.add(rect(x - 20, y - 40, 40, 36, ["#e8b22a", "#c23b2a", "#5b3a24", "#f6f1e3"][(k + r) % 4], rx=4, sw=None))
            elif stall_index == 2:
                for j in range(4):
                    s.add(circle(x - 20 + j * 13, y - 30 - (j % 2) * 10, 12, ["#f2a531", "#ffffff", "#c2324a"][(k + j + r) % 3], sw=1.2))
            else:
                s.add(group(getattr(I, "icon_" + ["tomato", "onion", "brinjal", "banana"][(k + r) % 4])(), transform=f"translate({f(x)} {y - 34}) scale(0.32)"))
    s.add(group(awning(W + 40, c1, c2), transform=f"translate({W / 2} -10)"))
    # vendor behind the counter
    vendor = {0: people.VEG_VENDOR, 1: people.SPICE_VENDOR, 2: people.FLOWER_VENDOR, 3: people.CLOTH_VENDOR}[stall_index]
    counter_y = 700 if land else 1260
    vs = 1.1 if land else 1.35
    vx, vy = W / 2 - 256 * vs, counter_y - 470 * vs
    s.add(group(people.bust(vendor), transform=f"translate({f(vx)} {f(vy)}) scale({vs})"))
    vendor_box = Box(vx + 140 * vs, vy + 70 * vs, 232 * vs, (counter_y - (vy + 70 * vs)) - 10)
    # counter
    s.add(rect(-10, counter_y, W + 20, H - counter_y + 10, "url(#gWoodV)", sw=LW_BIG))
    s.add(rect(-10, counter_y, W + 20, 40, C["wood_light"], sw=LW_MID))
    # piles on the counter
    n = len(goods)
    if land:
        xs = [W * (k + 0.5) / n for k in range(n)]
        slots = [(x, counter_y + 300, 1.15) for x in xs]
    else:
        slots = [(W * (0.27 if k % 2 == 0 else 0.73), counter_y + 340 + (k // 2) * 430, 1.25) for k in range(n)]
    for item, (x, y, sc) in zip(goods, slots):
        s.add(group(I.pile(item, 280), transform=f"translate({f(x)} {f(y)}) scale({sc})"))
        s.hotspot(item, Box(x - 160 * sc, y - 240 * sc, 320 * sc, 250 * sc).grow(4))
    s.hotspot("vendor", vendor_box)
    return s


BUILDERS = [kitchen, sewing, bus_stop, bus_interior, market] + [
    (lambda i: (lambda v: stall(v, i)))(i) for i in range(4)
]
