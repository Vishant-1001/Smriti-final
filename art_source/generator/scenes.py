"""Scene compositions for the M1 slice: house exterior, veranda hub, bedroom.

Each builder returns a Scene with hotspot boxes computed from the same coordinates used to
draw the interactive objects, so hit regions always match the art.
"""
from __future__ import annotations

import random

import props as P
from svgkit import (C, LW_BIG, LW_MID, LW_SMALL, Box, Placement, Scene, circle, ellipse, f, group,
                    line, path, poly, rect, shade, shadow)


# ================================================================ house exterior

HOUSE_DOOR = Box(440, 330, 120, 230)
HOUSE_STEPS = Box(415, 560, 170, 95)


def house(seed: int = 2) -> str:
    """South Indian single-storey house with hipped tiled roof and pillared veranda. Local 1000x660."""
    items = []
    # back wall
    items.append(rect(110, 240, 780, 330, "url(#gWallCream)", sw=LW_BIG))
    items.append(rect(110, 500, 780, 70, C["cream_dark"], sw=None, extra='opacity=".5"'))
    # windows
    items.append(group(P.window(110, 130, shutters=C["teal"]), transform="translate(205 320)"))
    items.append(group(P.window(110, 130, shutters=C["teal"]), transform="translate(685 320)"))
    # door
    items.append(group(P.wooden_door(HOUSE_DOOR.w, HOUSE_DOOR.h), transform=f"translate({HOUSE_DOOR.x} {HOUSE_DOOR.y})"))
    # small lamp niche shadows / wall decoration
    items.append(path("M350,300 q10,-22 20,0 Z", C["cream_dark"], LW_SMALL))
    items.append(path("M630,300 q10,-22 20,0 Z", C["cream_dark"], LW_SMALL))
    # thinnai (raised platforms either side of the steps)
    items.append(rect(90, 560, 330, 50, "url(#gTerracotta)", sw=LW_MID))
    items.append(rect(580, 560, 330, 50, "url(#gTerracotta)", sw=LW_MID))
    items.append(rect(80, 548, 350, 16, C["terracotta"], rx=3, sw=LW_MID))
    items.append(rect(570, 548, 350, 16, C["terracotta"], rx=3, sw=LW_MID))
    # steps
    for k in range(3):
        y = 560 + k * 30
        items.append(rect(430 - k * 12, y, 140 + k * 24, 32, "#d9c39a" if k % 2 == 0 else "#cdb489", sw=LW_MID))
    # pillars in front
    for x in (130, 360, 640, 870):
        items.append(group(P.pillar(320, 30), transform=f"translate({x} 560)"))
    # roof
    roof = [(40, 248), (210, 70), (790, 70), (960, 248)]
    items.append(f'<clipPath id="roofclip{seed}"><polygon points="{" ".join(f"{x},{y}" for x, y in roof)}"/></clipPath>')
    items.append(poly(roof, "url(#gRoof)", LW_BIG))
    tiles = []
    for r in range(0, 9):
        y = 84 + r * 20
        for k in range(0, 52):
            x = 20 + k * 20 + (10 if r % 2 else 0)
            tiles.append(path(f"M{x},{y} q10,9 20,0", sw=1.6, stroke=C["roof_dark"]))
    items.append(group(*tiles, extra=f'clip-path="url(#roofclip{seed})"'))
    items.append(poly([(40, 248), (960, 248), (950, 262), (50, 262)], C["roof_dark"], LW_MID))
    items.append(rect(200, 58, 600, 16, C["roof_dark"], rx=4, sw=LW_MID))  # ridge
    items.append(path("M210,58 q-14,-26 -4,-40", sw=LW_MID))
    items.append(path("M790,58 q14,-26 4,-40", sw=LW_MID))
    items.append(shade([(110, 262), (890, 262), (890, 300), (110, 300)], 0.22))  # eave shadow on wall
    return group(*items)


def exterior(variant: str) -> Scene:
    if variant == "land":
        W, H = 1920, 1080
        hp = Placement(460, 60, 1.0)
        horizon = 640
    else:
        W, H = 1080, 2400
        hp = Placement(40, 780, 1.0)
        horizon = 1400
    s = Scene("exterior", variant, W, H, background=C["sky_bottom"])
    s.add(rect(0, 0, W, horizon + 40, "url(#gSky)", sw=None))
    rnd = random.Random(10)
    for i in range(5 if variant == "land" else 4):
        cx, cy = rnd.uniform(0.05, 0.95) * W, rnd.uniform(0.06, 0.25) * (horizon)
        s.add(ellipse(cx, cy, rnd.uniform(90, 160), rnd.uniform(26, 40), "#ffffff", sw=None, extra='opacity=".7" filter="url(#soft)"'))
    # far trees line
    trees = []
    for i in range(14 if variant == "land" else 9):
        x = i * W / (13 if variant == "land" else 8)
        r = rnd.uniform(70, 120)
        trees.append(circle(x, horizon - r * 0.4, r, C["leaf_dark"] if i % 2 else "#4f7a42", sw=LW_SMALL))
    s.add(*trees)
    # palms behind the house
    palm_pos = [(300, horizon + 20, 560, -40), (1640, horizon + 30, 600, 50), (1500, horizon + 10, 480, -30)] if variant == "land" \
        else [(120, horizon - 20, 640, 40), (960, horizon - 10, 700, -50), (820, horizon - 40, 520, 30)]
    for x, y, h, lean in palm_pos:
        s.add(group(P.coconut_palm(h, lean, seed=int(x)), transform=f"translate({x} {y})"))
    # ground
    s.add(rect(0, horizon, W, H - horizon, "url(#gGrass)", sw=None))
    # house
    s.add(group(house(), transform=hp.transform))
    door = hp.box(HOUSE_DOOR)
    steps = hp.box(HOUSE_STEPS)
    base_y = hp.ty + 655 * hp.s
    cx = door.x + door.w / 2
    # path to door
    s.add(poly([(cx - 90, base_y - 4), (cx + 90, base_y - 4), (cx + (420 if variant == "land" else 330), H + 10),
                (cx - (420 if variant == "land" else 330), H + 10)], "url(#gSand)", LW_MID))
    for k in range(1, 7):
        t = k / 7
        y = base_y + (H - base_y) * t * t
        half = 90 + ((420 if variant == "land" else 330) - 90) * t * t
        s.add(line(cx - half, y, cx + half, y, sw=1.4, stroke=C["sand_dark"], extra='opacity=".6"'))
    # kolam on the path
    ky = base_y + (H - base_y) * 0.42
    s.add(group(P.kolam(150 if variant == "land" else 140), transform=f"translate({f(cx)} {f(ky)}) scale(1 0.36)"))
    # compound wall + gate pillars
    wall_y = base_y + (H - base_y) * (0.22 if variant == "land" else 0.18)
    wh = 110 if variant == "land" else 120
    gap = 230 if variant == "land" else 200
    for side in (-1, 1):
        x0 = 0 if side < 0 else cx + gap
        x1 = cx - gap if side < 0 else W
        s.add(rect(x0 - 10, wall_y - wh, x1 - x0 + 20, wh, "url(#gWallCream)", sw=LW_MID))
        s.add(rect(x0 - 16, wall_y - wh - 16, x1 - x0 + 32, 18, C["terracotta"], rx=3, sw=LW_MID))
        s.add(shade([(x0 - 10, wall_y - 30), (x1 + 10, wall_y - 30), (x1 + 10, wall_y), (x0 - 10, wall_y)], 0.12))
        px = cx + side * gap
        s.add(rect(px - 34, wall_y - wh - 70, 68, wh + 70, "url(#gWallCream)", sw=LW_MID))
        s.add(rect(px - 42, wall_y - wh - 88, 84, 22, C["terracotta"], rx=4, sw=LW_MID))
        s.add(group(P.potted_plant("tulsi", 0.45, seed=3), transform=f"translate({f(px)} {f(wall_y - wh - 88)})"))
        # gate leaf opened outward (iron, dark green)
        gx = px - side * 34
        gate = [poly([(gx, wall_y - wh + 10), (gx - side * 120, wall_y - wh + 40), (gx - side * 120, wall_y + 40), (gx, wall_y)], "none", LW_MID, stroke="#2f3d33")]
        for k in range(1, 7):
            t = k / 7
            gate.append(line(gx - side * 120 * t, wall_y - wh + 10 + 30 * t, gx - side * 120 * t, wall_y + 40 * t, sw=4, stroke="#2f3d33"))
        s.add(*gate)
    # bushes and plants
    if variant == "land":
        s.add(group(P.banana_plant(300), transform=f"translate(240 {f(wall_y - wh + 20)})"))
        s.add(group(P.bush(260, 110, seed=4, flowers="hibiscus"), transform=f"translate(1660 {f(wall_y - wh + 10)})"))
        s.add(group(P.bush(220, 120, seed=6), transform=f"translate(120 {H - 40})"))
        s.add(group(P.potted_plant("flowering", 1.0, seed=8), transform=f"translate({f(cx - 300)} {H - 70})"))
        s.add(group(P.potted_plant("broad", 1.1, seed=9), transform=f"translate({f(cx + 320)} {H - 60})"))
        s.add(group(P.bush(300, 130, seed=12, flowers="#f8f3e6"), transform=f"translate(1790 {H - 30})"))
    else:
        s.add(group(P.banana_plant(280), transform=f"translate(90 {f(wall_y - wh + 20)})"))
        s.add(group(P.bush(200, 110, seed=4, flowers="hibiscus"), transform=f"translate(990 {f(wall_y - wh + 10)})"))
        s.add(group(P.potted_plant("flowering", 1.1, seed=8), transform=f"translate(170 {H - 130})"))
        s.add(group(P.potted_plant("broad", 1.2, seed=9), transform=f"translate(910 {H - 120})"))
        s.add(group(P.bush(260, 140, seed=12, flowers="#f8f3e6"), transform=f"translate(540 {H + 30})"))
    # soft sunlight
    s.add(ellipse(W * 0.75, H * 0.12, W * 0.35, H * 0.25, "url(#gWarmLight)", sw=None))
    tap = door.union(steps).grow(12)
    s.hotspot("front_door", tap)
    if variant == "land":
        s.safe = Box(door.x - 520, door.y - 140, door.w + 1040, steps.y + steps.h - door.y + 200)
    else:
        s.safe = Box(30, door.y - 300, W - 60, steps.y + steps.h - door.y + 520)
    return s


# ================================================================ veranda (home hub)

def floor_tiles(poly_pts, vp, bottom_y, x_from, x_to, step, rows, colors=("#c46a3c", "#b45a30")) -> str:
    """Checkered terracotta floor in one-point perspective, clipped to poly_pts."""
    vx, vy = vp
    cid = f"floor{int(vx)}{int(vy)}{int(bottom_y)}"
    out = [f'<clipPath id="{cid}"><polygon points="{" ".join(f"{f(x)},{f(y)}" for x, y in poly_pts)}"/></clipPath>']
    top_y = min(y for _, y in poly_pts)

    def xat(xb, y):
        return vx + (xb - vx) * (y - vy) / (bottom_y - vy)

    ys = []
    for k in range(rows + 1):
        # perspective spacing between top_y and bottom_y
        d0 = 1.0 / (top_y - vy)
        d1 = 1.0 / (bottom_y - vy)
        d = d0 + (d1 - d0) * (k / rows)
        ys.append(vy + 1.0 / d)
    cells = []
    xb_list = list(range(int(x_from), int(x_to) + 1, int(step)))
    for i in range(len(xb_list) - 1):
        for j in range(len(ys) - 1):
            y0, y1 = ys[j], ys[j + 1]
            pts = [(xat(xb_list[i], y0), y0), (xat(xb_list[i + 1], y0), y0), (xat(xb_list[i + 1], y1), y1), (xat(xb_list[i], y1), y1)]
            cells.append(poly(pts, colors[(i + j) % 2], sw=1.2, stroke="#8e3f22"))
    out.append(group(*cells, extra=f'clip-path="url(#{cid})"'))
    out.append(poly(poly_pts, "none", LW_MID))
    return "".join(out)


def kitchen_glimpse(w: float, h: float) -> str:
    """What is seen through the kitchen doorway: shelves with vessels and warm light. Origin top-left."""
    return group(
        rect(0, 0, w, h, "#f2d9a6", sw=None),
        ellipse(w / 2, h * 0.35, w * 0.6, h * 0.3, "url(#gWarmLight)", sw=None),
        group(P.steel_shelf(w - 30), transform=f"translate(15 {f(h * 0.3)})"),
        group(P.steel_shelf(w - 30), transform=f"translate(15 {f(h * 0.55)})"),
        rect(0, h * 0.72, w, h * 0.28, "#d9c39a", sw=LW_SMALL),
        rect(10, h * 0.72, w - 20, 14, "#bfa778", sw=LW_SMALL),
    )


def bedroom_glimpse(w: float, h: float) -> str:
    return group(
        rect(0, 0, w, h, "#e9cf9a", sw=None),
        rect(w * 0.15, h * 0.62, w * 0.8, h * 0.2, "#f3e3c3", sw=LW_SMALL),
        rect(w * 0.15, h * 0.55, w * 0.25, h * 0.1, C["white"], rx=8, sw=LW_SMALL),
        rect(w * 0.15, h * 0.82, w * 0.8, h * 0.18, C["wood"], sw=LW_SMALL),
        rect(w * 0.15, h * 0.35, w * 0.08, h * 0.47, C["wood_dark"], sw=LW_SMALL),
    )


def sewing_corner(scale: float = 1.0) -> tuple[str, Box]:
    """Table with sewing machine, fabrics and stool against a wall. Origin bottom-centre of table. Returns (svg, local box)."""
    items = [
        group(P.table(360, 170), transform="translate(0 0)"),
        group(P.sewing_machine(0.95), transform="translate(-10 -200)"),
        group(P.fabric_stack(130), transform="translate(150 -200)"),
        group(P.fabric_stack(120, [C["teal"], "#e0a83a", C["maroon"]]), transform="translate(-120 -200)"),
    ]
    box = Box(-230, -420, 460, 430)
    return group(*items, transform=f"scale({scale:.3f})"), Box(box.x * scale, box.y * scale, box.w * scale, box.h * scale)


def veranda(variant: str) -> Scene:
    if variant == "land":
        W, H = 1920, 1080
        bw = Box(470, 200, 980, 500)     # back wall
        vp = (960, 430)
    else:
        W, H = 1080, 2400
        bw = Box(300, 720, 680, 600)
        vp = (640, 1000)
    s = Scene("veranda", variant, W, H, background=C["ochre_wall"])
    bx0, by0, bx1, by1 = bw.x, bw.y, bw.x + bw.w, bw.y + bw.h
    # ceiling (wooden planks with beams)
    s.add(poly([(0, 0), (W, 0), (bx1, by0), (bx0, by0)], "#7a4a2c", LW_MID))
    for k in range(1, 12):
        t = k / 12
        s.add(line(bx0 + (bx1 - bx0) * t, by0, W * t, 0, sw=1.6, stroke=C["wood_dark"]))
    for k, t in enumerate((0.25, 0.55, 0.8)):
        y = by0 * (1 - t)
        x0 = bx0 * (1 - t)
        x1 = bx1 + (W - bx1) * t
        s.add(poly([(x0, y - 10 * (1 + t)), (x1, y - 10 * (1 + t)), (x1, y + 10 * (1 + t)), (x0, y + 10 * (1 + t))], "url(#gWoodDark)", LW_MID))
    # left wall
    s.add(poly([(0, 0), (bx0, by0), (bx0, by1), (0, H)], "url(#gWallOchre)", LW_MID))
    s.add(shade([(0, 0), (bx0, by0), (bx0, by1), (0, H)], 0.12))
    # back wall
    s.add(rect(bx0, by0, bw.w, bw.h, "url(#gWallOchre)", sw=LW_MID))
    s.add(rect(bx0, by1 - 60, bw.w, 60, C["terracotta"], sw=LW_SMALL, extra='opacity=".55"'))
    # right side: open to the courtyard garden, low parapet (thinnai)
    s.add(poly([(bx1, by0), (W, 0), (W, H), (bx1, by1)], "url(#gSky)", LW_MID))
    s.add(poly([(bx1, by0 + (by1 - by0) * 0.55), (W, (by1 - by0) * 0.4 + by0 * 0.2), (W, H), (bx1, by1)], "url(#gGrass)", sw=None))
    s.add(group(P.coconut_palm(380 if variant == "land" else 300, 30, seed=77), transform=f"translate({f(bx1 + (W - bx1) * 0.55)} {f(by1 - 40)})"))
    garden = []
    rnd = random.Random(21)
    for i in range(10):
        gx = bx1 + rnd.uniform(0.1, 0.95) * (W - bx1)
        gy = by0 + rnd.uniform(0.25, 0.75) * (by1 - by0) + (gx - bx1) * 0.25
        garden.append(circle(gx, gy, rnd.uniform(40, 90), C["leaf_light"] if i % 2 else C["leaf"], sw=LW_SMALL))
    s.add(group(*garden, extra='opacity=".95"'))
    s.add(poly([(bx1, by1 - 70), (W, by1 - 70 + (H - by1) * 0.55), (W, H), (bx1, by1)], "url(#gTerracotta)", LW_MID))
    s.add(poly([(bx1, by1 - 80), (W, by1 - 80 + (H - by1) * 0.55), (W, by1 - 50 + (H - by1) * 0.55), (bx1, by1 - 66)], C["terracotta"], LW_MID))
    # floor
    floor = [(bx0, by1), (bx1, by1), (W, H), (0, H)]
    s.add(floor_tiles(floor, vp, H, -1600 if variant == "land" else -1400, W + 1600, 160 if variant == "land" else 130, 7))
    s.add(ellipse(W / 2, by1 + (H - by1) * 0.25, bw.w * 0.6, (H - by1) * 0.35, "url(#gWarmLight)", sw=None, extra='opacity=".6"'))

    # ---- doors on the back wall
    if variant == "land":
        bed_door = Box(560, 300, 160, 290)
        kit_door = Box(796, 300, 170, 290)
    else:
        bed_door = Box(350, 900, 170, 320)
        kit_door = Box(640, 900, 180, 320)
    # bedroom doorway: open, showing a glimpse of the bed
    s.add(rect(bed_door.x - 16, bed_door.y - 16, bed_door.w + 32, bed_door.h + 16, "url(#gWoodDark)", sw=LW_MID))
    s.add(group(bedroom_glimpse(bed_door.w, bed_door.h), transform=f"translate({f(bed_door.x)} {f(bed_door.y)})"))
    s.add(poly([(bed_door.x, bed_door.y), (bed_door.x + bed_door.w * 0.32, bed_door.y + 10), (bed_door.x + bed_door.w * 0.32, bed_door.y + bed_door.h - 6),
                (bed_door.x, bed_door.y + bed_door.h)], "url(#gWood)", LW_MID))
    s.add(rect(bed_door.x, bed_door.y, bed_door.w, bed_door.h, "none", sw=LW_MID))
    s.add(group(P.potted_plant("tall", 0.8, seed=31), transform=f"translate({f(bed_door.x - 60)} {f(by1)})"))
    # kitchen doorway: arch with curtain strip and warm light
    s.add(rect(kit_door.x - 16, kit_door.y - 16, kit_door.w + 32, kit_door.h + 16, "url(#gWoodDark)", sw=LW_MID))
    s.add(group(kitchen_glimpse(kit_door.w, kit_door.h), transform=f"translate({f(kit_door.x)} {f(kit_door.y)})"))
    s.add(path(f"M{f(kit_door.x)},{f(kit_door.y)} L{f(kit_door.x + kit_door.w)},{f(kit_door.y)} L{f(kit_door.x + kit_door.w)},{f(kit_door.y + 40)} "
               f"Q{f(kit_door.x + kit_door.w / 2)},{f(kit_door.y + 70)} {f(kit_door.x)},{f(kit_door.y + 40)} Z", C["teal"], LW_SMALL))
    s.add(rect(kit_door.x, kit_door.y, kit_door.w, kit_door.h, "none", sw=LW_MID))
    # brass lamp between doors
    lamp_x = (bed_door.x + bed_door.w + kit_door.x) / 2
    s.add(group(P.brass_lamp(130 if variant == "land" else 150), transform=f"translate({f(lamp_x)} {f(by1 - 4)})"))
    # framed pictures
    if variant == "land":
        s.add(group(P.picture_frame(90, 64, "hills"), transform=f"translate({f(lamp_x - 45)} 240)"))

    # ---- sewing corner
    if variant == "land":
        win = Box(1120, 260, 210, 150)
        s.add(group(P.window(win.w, win.h, shutters=C["indigo_light"]), transform=f"translate({f(win.x)} {f(win.y)})"))
        sc_svg, sc_box = sewing_corner(0.95)
        sc_place = Placement(1225, by1 + 20, 1.0)
        s.add(group(sc_svg, transform=f"translate({f(sc_place.tx)} {f(sc_place.ty)})"))
        sew_box = sc_place.box(sc_box)
    else:
        sc_svg, sc_box = sewing_corner(1.2)
        sc_place = Placement(790, 2060, 1.0)
        s.add(group(sc_svg, transform=f"translate({f(sc_place.tx)} {f(sc_place.ty)})"))
        sew_box = sc_place.box(sc_box)

    # ---- front door on the left wall (perspective quad), ajar with daylight
    if variant == "land":
        fx0, fx1 = 150, 360
        def wall_y(x, y):  # map a back-wall-relative height onto the left wall at screen x
            t = x / bx0
            return y * t
        top0, top1 = 230, 300
        bot0, bot1 = 900, 780
    else:
        fx0, fx1 = 50, 220
        top0, top1 = 1060, 1140
        bot0, bot1 = 2000, 1780
    frame = [(fx0 - 14, top0 - 18), (fx1 + 12, top1 - 14), (fx1 + 12, bot1 + 4), (fx0 - 14, bot0 + 6)]
    s.add(poly(frame, "url(#gWoodDark)", LW_MID))
    s.add(poly([(fx0, top0), (fx1, top1), (fx1, bot1), (fx0, bot0)], "#fff4d6", LW_SMALL))
    s.add(poly([(fx0, top0 + (bot0 - top0) * 0.55), (fx1, top1 + (bot1 - top1) * 0.6), (fx1, bot1), (fx0, bot0)], C["leaf_light"], sw=None, extra='opacity=".85"'))
    leaf_w = (fx1 - fx0) * 0.55
    s.add(poly([(fx1, top1), (fx1 + leaf_w, top1 - 30), (fx1 + leaf_w, bot1 + 40), (fx1, bot1)], "url(#gWood)", LW_MID))
    for k in range(3):
        t0, t1 = 0.08 + k * 0.31, 0.33 + k * 0.31
        s.add(poly([(fx1 + 10, top1 + (bot1 - top1) * t0 - 4), (fx1 + leaf_w - 10, top1 - 30 + (bot1 + 70 - top1) * t0),
                    (fx1 + leaf_w - 10, top1 - 30 + (bot1 + 70 - top1) * t1 - 6), (fx1 + 10, top1 + (bot1 - top1) * t1 - 8)], C["wood"], sw=1.4))
    s.add(circle(fx1 + leaf_w - 18, (top1 + bot1) / 2, 7, "url(#gBrass)", sw=1.2))
    s.add(ellipse((fx0 + fx1) / 2 + 40, bot0 + 30, 160, 40, "url(#gWarmLight)", sw=None))
    # door mat
    s.add(poly([(fx0 + 20, bot0 + 20), (fx1 + 60, bot1 + 30), (fx1 + 120, bot1 + 80), (fx0 + 60, bot0 + 80)], C["maroon"], LW_SMALL))
    # The tap area is the door itself (frame top to the inner edge of the open leaf), not the mat.
    front_box = Box(fx0 - 14, top1 - 20, fx1 + leaf_w - fx0 + 24, bot1 - top1 + 50)

    # ---- foreground decor
    if variant == "land":
        s.add(group(P.pillar(1080 - 0, 46), transform=f"translate(1560 {H + 10})"))
        s.add(group(P.hanging_plant(150, seed=5), transform="translate(1420 0)"))
        s.add(group(P.hanging_plant(110, seed=8), transform="translate(560 0)"))
        s.add(group(P.potted_plant("broad", 1.25, seed=41), transform=f"translate(1830 {H - 30})"))
        s.add(group(P.kolam(150), transform=f"translate(940 {f(by1 + 170)}) scale(1 0.3)"))
    else:
        s.add(group(P.hanging_plant(170, seed=5), transform="translate(870 0)"))
        s.add(group(P.potted_plant("broad", 1.3, seed=41), transform=f"translate(90 {H - 40})"))

    s.hotspot("bedroom_door", bed_door.grow(18))
    s.hotspot("kitchen_door", kit_door.grow(18))
    s.hotspot("sewing_corner", sew_box.grow(6))
    s.hotspot("front_door", front_box)
    return s


# ================================================================ bedroom

def bed(variant: str) -> str:
    """Wooden cot with patterned sheet seen at an angle. Local coordinates, origin at headboard bottom-left."""
    items = [
        shadow(250, 330, 330, 40),
        # headboard
        path("M0,0 L0,-170 Q0,-210 40,-210 L380,-210 Q420,-210 420,-170 L420,0 Z", "url(#gWood)", LW_BIG),
        path("M30,-20 L30,-160 Q30,-185 55,-185 L365,-185 Q390,-185 390,-160 L390,-20 Z", C["wood"], LW_SMALL),
        path("M120,-180 q90,-40 180,0", sw=2, stroke=C["brass"]),
        # mattress top (perspective toward viewer, lower-left)
        poly([(-10, 0), (430, 0), (300, 300), (-260, 300)], "#f3e3c3", LW_MID),
        # sheet pattern
    ]
    rnd = random.Random(14)
    pattern = []
    for i in range(40):
        u, v = rnd.random(), rnd.random()
        x = (-10 + 440 * u) * (1 - v) + (-260 + 560 * u) * v
        y = 300 * v
        pattern.append(circle(x, y, 7 + 5 * v, C["terracotta"] if i % 3 else C["indigo_light"], sw=None, extra='opacity=".75"'))
    items.append(group(*pattern))
    items += [
        poly([(-260, 300), (300, 300), (300, 360), (-260, 360)], "#e6cfa2", LW_MID),
        poly([(300, 300), (430, 0), (430, 60), (300, 360)], "#d7bb88", LW_MID),
        # pillows
        path("M30,-10 Q20,-70 70,-70 L170,-70 Q210,-70 200,-10 Q120,10 30,-10 Z", C["white"], LW_MID),
        path("M230,-10 Q220,-70 270,-70 L370,-70 Q410,-70 400,-10 Q320,10 230,-10 Z", C["white"], LW_MID),
        # folded blanket at foot
        poly([(-200, 220), (260, 220), (290, 290), (-240, 290)], C["indigo"], LW_MID),
        line(-200, 245, 270, 245, sw=2, stroke="#e0a83a"),
        # legs
        rect(-262, 360, 22, 50, C["wood_dark"], sw=LW_MID), rect(278, 360, 22, 50, C["wood_dark"], sw=LW_MID),
    ]
    return group(*items)


def bedroom(variant: str) -> Scene:
    if variant == "land":
        W, H = 1920, 1080
        bw = Box(200, 90, 1520, 600)
    else:
        W, H = 1080, 2400
        bw = Box(70, 380, 940, 900)
    s = Scene("bedroom", variant, W, H, background=C["ochre_wall"])
    bx0, by0, bx1, by1 = bw.x, bw.y, bw.x + bw.w, bw.y + bw.h
    # walls
    s.add(poly([(0, 0), (W, 0), (bx1, by0), (bx0, by0)], "#f3dfb6", LW_MID))
    s.add(poly([(0, 0), (bx0, by0), (bx0, by1), (0, H)], "url(#gWallOchre)", LW_MID))
    s.add(shade([(0, 0), (bx0, by0), (bx0, by1), (0, H)], 0.14))
    s.add(poly([(W, 0), (bx1, by0), (bx1, by1), (W, H)], "url(#gWallOchre)", LW_MID))
    s.add(shade([(W, 0), (bx1, by0), (bx1, by1), (W, H)], 0.2))
    s.add(rect(bx0, by0, bw.w, bw.h, "url(#gWallOchre)", sw=LW_MID))
    s.add(rect(bx0, by1 - 70, bw.w, 70, "#d6a86a", sw=LW_SMALL, extra='opacity=".7"'))
    # floor: polished red oxide
    s.add(poly([(bx0, by1), (bx1, by1), (W, H), (0, H)], "#b4532f", LW_MID))
    s.add(poly([(bx0 + 200, by1 + 10), (bx1 - 200, by1 + 10), (bx1 + 100, H - 80), (bx0 - 100, H - 80)], "#ffffff", sw=None, extra='opacity=".08"'))

    if variant == "land":
        win = Box(780, 180, 300, 230)
        bed_at = (300, 640, 1.0)
        desk = Placement(1330, 930, 1.0)
        desk_w, desk_h = 640, 250
        cup = (1800, by1 + 140)
    else:
        win = Box(560, 540, 240, 200)
        bed_at = (210, 1240, 0.85)
        desk = Placement(560, 1880, 1.0)
        desk_w, desk_h = 860, 260
        cup = None
    # window with light
    s.add(ellipse(win.x + win.w / 2, win.y + win.h / 2 + 120, win.w * 1.6, win.h * 1.4, "url(#gWarmLight)", sw=None))
    s.add(group(P.window(win.w, win.h, shutters=None, curtains=True), transform=f"translate({f(win.x)} {f(win.y)})"))
    # pictures
    if variant == "land":
        s.add(group(P.picture_frame(150, 105, "sea"), transform="translate(1220 200)"))
        s.add(group(P.picture_frame(110, 140, "flowers"), transform="translate(1440 180)"))
        s.add(group(P.picture_frame(120, 85, "hills"), transform="translate(440 250)"))
    else:
        s.add(group(P.picture_frame(150, 105, "sea"), transform="translate(200 560)"))
        s.add(group(P.picture_frame(100, 125, "flowers"), transform="translate(860 860)"))
    if cup:
        s.add(group(P.cupboard(230, 470), transform=f"translate({cup[0]} {cup[1]})"))
    # bed
    bx, byy, bs = bed_at
    s.add(group(bed(variant), transform=f"translate({bx} {byy}) scale({bs})"))
    # rug
    if variant == "land":
        s.add(group(P.rug(520, 120), transform=f"translate(1000 {H - 90})"))
    else:
        s.add(group(P.rug(560, 140), transform=f"translate(540 {H - 130})"))
    # desk + memory book + radio + lamp
    s.add(group(P.table(desk_w, desk_h, top_depth=46), transform=desk.transform))
    top_y = desk.ty - desk_h - 16
    if variant == "land":
        book_c, book_s = (desk.tx - 150, top_y), 0.9
        radio_c, radio_s = (desk.tx + 200, top_y - 6), 0.72
        lamp_c = None
        s.add(group(P.potted_plant("tall", 1.0, seed=17), transform=f"translate({f(desk.tx - 400)} {f(by1 + 50)})"))
        s.add(group(P.chair(1.0), transform=f"translate({f(desk.tx + 420)} {H - 40})"))
    else:
        book_c, book_s = (desk.tx - 190, top_y), 1.1
        radio_c, radio_s = (desk.tx + 220, top_y - 6), 1.0
        lamp_c = None
    s.add(group(P.radio(radio_s), transform=f"translate({f(radio_c[0])} {f(radio_c[1])})"))
    s.add(group(P.memory_book_open(book_s), transform=f"translate({f(book_c[0])} {f(book_c[1])})"))
    book_box = Box(book_c[0] - 175 * book_s, book_c[1] - 118 * book_s, 350 * book_s, 140 * book_s)
    radio_box = Box(radio_c[0] - 155 * radio_s, radio_c[1] - 214 * radio_s, 310 * radio_s, 224 * radio_s)
    s.hotspot("memory_book", book_box.grow(22))
    s.hotspot("radio", radio_box.grow(22))
    if variant == "land":
        s.safe = Box(560, 300, 1340, 560)
    else:
        s.safe = Box(30, 1180, 1020, 940)
    return s
