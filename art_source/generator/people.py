"""Canonical character busts for SMRITI, all drawn from one parameterised template so faces,
line weight and proportions stay consistent. Every character has a `neutral` and a `speaking`
frame that differ only in the mouth.

Canvas: 512x512, transparent. The same function also draws the characters into scenes
(scaled), so the in-scene vendor and the dialogue portrait always match.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from svgkit import C, LW_BIG, LW_MID, LW_SMALL, circle, ellipse, group, line, path


@dataclass(frozen=True)
class Person:
    skin: str = C["skin"]
    skin_shadow: str = C["skin_shadow"]
    hair: str = C["hair"]
    hair_style: str = "bun"          # bun | short | short_grey | bald_grey
    outfit: str = "saree"            # saree | shirt | uniform
    main: str = C["teal"]            # saree / shirt colour
    accent: str = C["brass"]         # saree border / pocket trim
    blouse: str = C["maroon"]
    apron: str | None = None
    mustache: bool = False
    bindi: bool = False
    jasmine: bool = False
    glasses: bool = False
    cap: str | None = None           # cap colour (conductor)
    earrings: bool = False
    extras: tuple = field(default_factory=tuple)


GUIDE = Person(bindi=True, jasmine=True, earrings=True)
CONDUCTOR = Person(skin="#a86b45", skin_shadow="#8a5536", hair_style="short", outfit="uniform", main="#b89a66",
                   accent="#6b5636", mustache=True, cap="#8a7448")
VEG_VENDOR = Person(skin="#9c6340", skin_shadow="#7e4c2f", hair_style="short", outfit="shirt", main="#f4efe2",
                    accent=C["leaf_dark"], apron=C["leaf_dark"], mustache=True)
SPICE_VENDOR = Person(skin="#b07a52", skin_shadow="#8f5f3d", hair="#9a948c", hair_style="short_grey", outfit="shirt",
                      main="#efe3c8", accent=C["terracotta"], apron="#c8643b", mustache=True, glasses=True)
FLOWER_VENDOR = Person(skin="#a46a45", skin_shadow="#85512f", outfit="saree", main="#d0607a", accent="#e0a83a",
                       blouse=C["leaf_dark"], bindi=True, jasmine=True, earrings=True)
CLOTH_VENDOR = Person(skin="#b98058", skin_shadow="#99623e", hair_style="short", outfit="shirt", main="#9cc3e0",
                      accent=C["indigo"], mustache=True)

CHARACTERS = {
    "guide": GUIDE,
    "conductor": CONDUCTOR,
    "vendor_produce": VEG_VENDOR,
    "vendor_spice": SPICE_VENDOR,
    "vendor_flower": FLOWER_VENDOR,
    "vendor_cloth": CLOTH_VENDOR,
}


def bust(p: Person, frame: str = "neutral") -> str:
    skin, skin_sh = p.skin, p.skin_shadow
    items = []
    # behind the head
    if p.hair_style == "bun":
        items.append(circle(180, 205, 46, p.hair, LW_MID))
        if p.jasmine:
            items += [circle(150 + i * 9, 175 + (i % 2) * 8, 6, C["white"], sw=1.2) for i in range(7)]
    # body
    if p.outfit == "saree":
        items += [
            path("M80,512 Q90,380 190,350 L322,350 Q422,380 432,512 Z", p.blouse, LW_BIG),
            path("M170,352 Q250,420 300,512 L432,512 Q425,420 340,360 L300,352 Z", p.main, LW_MID),
            path("M300,352 Q368,400 412,512", sw=10, stroke=p.accent), path("M300,352 Q368,400 412,512", sw=LW_SMALL),
            path("M170,352 Q236,410 284,512", sw=8, stroke=p.accent), path("M170,352 Q236,410 284,512", sw=LW_SMALL),
        ]
    else:
        items += [
            path("M70,512 Q80,382 190,350 L322,350 Q432,382 442,512 Z", p.main, LW_BIG),
            # collar
            path("M206,350 L256,402 L230,420 L196,372 Z", p.main, LW_MID),
            path("M306,350 L256,402 L282,420 L316,372 Z", p.main, LW_MID),
            line(256, 402, 256, 512, sw=LW_SMALL),
            circle(256, 440, 4, p.accent, sw=1), circle(256, 480, 4, p.accent, sw=1),
        ]
        if p.outfit == "uniform":
            items += [
                path("M110,430 L170,430 L170,470 L110,470 Z", p.main, LW_SMALL),  # pocket
                path("M300,350 L120,512", sw=14, stroke=p.accent), path("M300,350 L120,512", sw=LW_SMALL),  # bag strap
                path("M330,440 L380,440 L380,476 L330,476 Z", "#d9d2c3", LW_SMALL),  # badge
            ]
        if p.apron:
            items += [path("M150,512 L170,420 Q256,440 342,420 L362,512 Z", p.apron, LW_MID),
                      path("M190,420 L214,352", sw=6, stroke=p.apron), path("M322,420 L298,352", sw=6, stroke=p.apron)]
    # neck
    items += [
        path("M222,300 L222,360 Q256,380 290,360 L290,300 Z", skin, LW_MID),
        path("M222,330 Q256,350 290,330 L290,300 L222,300 Z", skin_sh, sw=None, extra='opacity=".6"'),
    ]
    if p.outfit == "saree":
        items.append(path("M214,356 Q256,396 298,356", sw=3, stroke=C["brass"]))
    # ears
    items += [ellipse(168, 236, 14, 24, skin, LW_MID), ellipse(344, 236, 14, 24, skin, LW_MID)]
    if p.earrings:
        items += [circle(168, 262, 7, "url(#gBrass)", sw=1.4), circle(344, 262, 7, "url(#gBrass)", sw=1.4)]
    # face
    items.append(path("M172,200 Q170,120 256,112 Q342,120 340,200 Q342,290 256,322 Q170,290 172,200 Z", skin, LW_BIG))
    items.append(path("M300,150 Q338,190 330,250 Q320,295 270,318 Q320,270 318,210 Z", skin_sh, sw=None, extra='opacity=".35"'))
    # hair
    if p.hair_style == "bun":
        items += [path("M166,214 Q150,96 256,84 Q362,96 346,214 Q336,150 300,132 Q270,124 258,122 Q246,124 214,132 Q178,150 166,214 Z", p.hair, LW_MID),
                  line(256, 88, 256, 122, sw=2, stroke="#6b4b3e")]
    elif p.hair_style in ("short", "short_grey"):
        items.append(path("M168,206 Q150,100 256,86 Q362,100 344,206 Q340,160 318,142 Q290,150 256,140 Q214,148 196,138 Q172,160 168,206 Z", p.hair, LW_MID))
    # brows & eyes
    items += [
        path("M196,190 Q218,178 240,188", sw=5, stroke=p.hair if p.hair_style != "short_grey" else "#6f6a63"),
        path("M272,188 Q294,178 316,190", sw=5, stroke=p.hair if p.hair_style != "short_grey" else "#6f6a63"),
        path("M200,214 Q218,200 238,214 Q218,224 200,214 Z", C["white"], LW_SMALL),
        path("M274,214 Q294,200 312,214 Q294,224 274,214 Z", C["white"], LW_SMALL),
        circle(219, 213, 8, "#3b2318", sw=None), circle(293, 213, 8, "#3b2318", sw=None),
        circle(221, 210, 2.5, "#ffffff", sw=None), circle(295, 210, 2.5, "#ffffff", sw=None),
        path("M198,212 Q218,198 240,212", sw=3, stroke=C["hair"]),
        path("M272,212 Q294,198 314,212", sw=3, stroke=C["hair"]),
        path("M256,222 Q250,248 246,256 Q256,264 268,256", sw=LW_SMALL),
        ellipse(204, 256, 18, 10, "#d98a6a", sw=None, extra='opacity=".3"'),
        ellipse(308, 256, 18, 10, "#d98a6a", sw=None, extra='opacity=".3"'),
    ]
    if p.glasses:
        items += [circle(219, 214, 24, "none", sw=3, stroke="#3a3a3a"), circle(293, 214, 24, "none", sw=3, stroke="#3a3a3a"),
                  line(243, 212, 269, 212, sw=3, stroke="#3a3a3a")]
    if p.bindi:
        items.append(circle(256, 176, 6, C["red"], sw=1.2))
    # mouth
    if frame == "speaking":
        items += [path("M232,284 Q256,278 280,284 Q272,308 256,308 Q240,308 232,284 Z", "#6b2a28", LW_SMALL),
                  path("M240,298 Q256,290 272,298 Q264,306 256,306 Q248,306 240,298 Z", "#c96a63", sw=None)]
    else:
        items.append(path("M230,282 Q256,302 282,282", sw=LW_MID))
    if p.mustache:
        items.append(path("M222,276 Q240,262 256,272 Q272,262 290,276 Q274,282 256,278 Q238,282 222,276 Z", C["hair"] if p.hair_style != "short_grey" else "#7a746c", LW_SMALL))
    if p.cap:
        items += [path("M166,150 Q170,82 256,76 Q342,82 346,150 Z", p.cap, LW_MID),
                  path("M150,152 L362,152 Q380,160 362,172 L150,172 Q132,160 150,152 Z", "#5e4c2c", LW_MID),
                  circle(256, 112, 10, C["brass"], sw=1.4)]
    return group(*items)
