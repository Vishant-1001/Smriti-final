"""Canonical SMRITI Guide character (bust) and small standalone illustrations.

The Guide is a calm adult woman in a teal cotton saree with a gold border and maroon blouse,
centre-parted hair in a low bun with a jasmine strand, small red bindi, gold studs.
Frames share every coordinate except the mouth, so speaking alternates cleanly.
"""
from __future__ import annotations

import props as P
from svgkit import C, LW_BIG, LW_MID, LW_SMALL, circle, ellipse, group, line, path, poly, rect, shadow


def guide(frame: str = "neutral") -> str:
    """512x512 sprite, transparent background."""
    skin, skin_sh = C["skin"], C["skin_shadow"]
    items = []
    # bun + jasmine behind head (visible at left)
    items += [circle(180, 205, 46, C["hair"], LW_MID)]
    items += [circle(150 + i * 9, 175 + (i % 2) * 8, 6, C["white"], sw=1.2) for i in range(7)]
    # shoulders / blouse / saree
    items += [
        path("M80,512 Q90,380 190,350 L322,350 Q422,380 432,512 Z", C["maroon"], LW_BIG),
        # saree pallu across the chest from left shoulder
        path("M170,352 Q250,420 300,512 L432,512 Q425,420 340,360 L300,352 Z", "url(#gTeal)", LW_MID),
        path("M300,352 Q368,400 412,512", sw=10, stroke=C["brass"]),
        path("M300,352 Q368,400 412,512", sw=LW_SMALL),
        path("M170,352 Q236,410 284,512", sw=8, stroke=C["brass"]),
        path("M170,352 Q236,410 284,512", sw=LW_SMALL),
        # neck
        path("M222,300 L222,360 Q256,380 290,360 L290,300 Z", skin, LW_MID),
        path("M222,330 Q256,350 290,330 L290,300 L222,300 Z", skin_sh, sw=None, extra='opacity=".6"'),
        # thin gold chain
        path("M214,356 Q256,396 298,356", sw=3, stroke=C["brass"]),
    ]
    # ears + studs
    items += [ellipse(168, 236, 14, 24, skin, LW_MID), ellipse(344, 236, 14, 24, skin, LW_MID),
              circle(168, 262, 7, "url(#gBrass)", sw=1.4), circle(344, 262, 7, "url(#gBrass)", sw=1.4)]
    # face
    items += [path("M172,200 Q170,120 256,112 Q342,120 340,200 Q342,290 256,322 Q170,290 172,200 Z", skin, LW_BIG)]
    items += [path("M300,150 Q338,190 330,250 Q320,295 270,318 Q320,270 318,210 Z", skin_sh, sw=None, extra='opacity=".35"')]
    # hair (centre parting, framing the face)
    items += [
        path("M166,214 Q150,96 256,84 Q362,96 346,214 Q336,150 300,132 Q270,124 258,122 Q246,124 214,132 Q178,150 166,214 Z",
             C["hair"], LW_MID),
        line(256, 88, 256, 122, sw=2, stroke="#6b4b3e"),
    ]
    # brows, eyes
    items += [
        path("M196,190 Q218,178 240,188", sw=5, stroke=C["hair"]),
        path("M272,188 Q294,178 316,190", sw=5, stroke=C["hair"]),
        path("M200,214 Q218,200 238,214 Q218,224 200,214 Z", C["white"], LW_SMALL),
        path("M274,214 Q294,200 312,214 Q294,224 274,214 Z", C["white"], LW_SMALL),
        circle(219, 213, 8, "#3b2318", sw=None), circle(293, 213, 8, "#3b2318", sw=None),
        circle(221, 210, 2.5, "#ffffff", sw=None), circle(295, 210, 2.5, "#ffffff", sw=None),
        path("M198,212 Q218,198 240,212", sw=3, stroke=C["hair"]),
        path("M272,212 Q294,198 314,212", sw=3, stroke=C["hair"]),
        # bindi, nose, nose stud
        circle(256, 176, 6, C["red"], sw=1.2),
        path("M256,222 Q250,248 246,256 Q256,264 268,256", sw=LW_SMALL),
        circle(244, 254, 2.6, "#ffe9a8", sw=None),
        # cheeks
        ellipse(204, 256, 18, 10, "#d98a6a", sw=None, extra='opacity=".35"'),
        ellipse(308, 256, 18, 10, "#d98a6a", sw=None, extra='opacity=".35"'),
    ]
    if frame == "speaking":
        items += [path("M232,282 Q256,276 280,282 Q272,306 256,306 Q240,306 232,282 Z", "#6b2a28", LW_SMALL),
                  path("M240,296 Q256,288 272,296 Q264,304 256,304 Q248,304 240,296 Z", "#c96a63", sw=None)]
    else:
        items += [path("M230,280 Q256,300 282,280", sw=LW_MID)]
    return group(*items)


def radio_sprite() -> str:
    """Radio on a lace doily for the radio screen. 800x600 canvas."""
    return group(
        ellipse(400, 520, 300, 46, "#fbf3e2", LW_SMALL),
        ellipse(400, 520, 270, 36, "none", sw=1.6, stroke=C["cream_dark"], extra='stroke-dasharray="8 6"'),
        group(P.radio(1.9), transform="translate(400 520)"),
    )


def vignette(kind: str) -> str:
    """600x450 sample Memory Book illustrations. No people, no text."""
    W, H = 600, 450
    items = []
    if kind == "kolam":
        items += [rect(0, 0, W, H, "#b4532f", sw=None),
                  rect(0, 0, W, 90, "#d6a86a", sw=None),
                  rect(0, 90, W, 12, C["terracotta_dark"], sw=None),
                  group(P.kolam(150), transform="translate(300 290) scale(1 0.55)"),
                  group(P.brass_lamp(120), transform="translate(520 330)"),
                  group(P.potted_plant("flowering", 0.7, seed=3), transform="translate(80 380)")]
    elif kind == "coffee":
        items += [rect(0, 0, W, H, "#f2d9a6", sw=None),
                  rect(0, 300, W, 150, "url(#gWoodV)", sw=None),
                  line(0, 300, W, 300, sw=LW_MID),
                  shadow(300, 360, 150, 18),
                  # davara (wide bowl) and tumbler
                  path("M170,330 Q170,380 300,380 Q430,380 430,330 Z", "url(#gSteel)", LW_MID),
                  ellipse(300, 330, 130, 20, "#dfe4e7", LW_MID),
                  path("M250,330 L240,190 L360,190 L350,330 Z", "url(#gSteel)", LW_MID),
                  ellipse(300, 190, 60, 12, "#c79a6a", LW_MID),
                  ellipse(300, 190, 44, 7, "#e9d1b0", sw=None),
                  path("M282,170 q-14,-30 6,-56 q16,-24 0,-48", sw=4, stroke="#ffffff", extra='opacity=".75"'),
                  path("M316,172 q-12,-26 6,-48", sw=4, stroke="#ffffff", extra='opacity=".6"'),
                  group(P.potted_plant("tall", 0.6, seed=12), transform="translate(510 300)")]
    elif kind == "jasmine":
        items += [rect(0, 0, W, H, "#eaf2de", sw=None),
                  rect(0, 320, W, 130, "url(#gWoodV)", sw=None),
                  line(0, 320, W, 320, sw=LW_MID),
                  shadow(300, 380, 210, 20)]
        import math
        for i in range(36):
            a = math.radians(i * 10)
            x, y = 300 + math.cos(a) * 190, 365 + math.sin(a) * 50
            items.append(ellipse(x, y, 13, 9, C["white"], LW_SMALL))
            if i % 3 == 0:
                items.append(leaf(x, y - 4, 26, 12, i * 10 + 90))
        items += [ellipse(300, 365, 120, 26, "url(#gTerracotta)", LW_SMALL)]
    return group(*items)


def leaf(x, y, l, w, a):
    from svgkit import leaf as _leaf
    return _leaf(x, y, l, w, a)
