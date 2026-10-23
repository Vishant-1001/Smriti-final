"""Small SVG authoring kit for SMRITI's hand-authored 2D-comic art.

Everything here is plain string building so the art is fully reproducible from source.
Style rules (see docs/ART_BIBLE.md): one warm dark-brown outline colour, rounded joins,
three line weights, soft gradient shading, blurred contact shadows, no text in art.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

OUT = "#4a2e22"          # the single outline colour
LW_BIG, LW_MID, LW_SMALL = 4.0, 3.0, 2.0

PALETTE = {
    "cream": "#f6e7c8",
    "cream_dark": "#e8d2a6",
    "ochre_wall": "#efd29a",
    "ochre_wall_dark": "#d9b574",
    "terracotta": "#c8643b",
    "terracotta_dark": "#9e4527",
    "roof": "#b8502f",
    "roof_dark": "#8c3a22",
    "leaf": "#5e8c4a",
    "leaf_light": "#86b25e",
    "leaf_dark": "#3f6b3a",
    "indigo": "#3c4c8c",
    "indigo_light": "#5c6db0",
    "wood": "#8a5230",
    "wood_light": "#a96a3f",
    "wood_dark": "#5e3520",
    "brass": "#e2b04a",
    "brass_dark": "#a87a24",
    "teal": "#2f8a85",
    "teal_dark": "#1f6461",
    "maroon": "#8e2f3c",
    "sand": "#e8cc96",
    "sand_dark": "#cfae72",
    "grass": "#9cbf6b",
    "grass_dark": "#7aa152",
    "sky_top": "#8fcfe3",
    "sky_bottom": "#f4e9cc",
    "skin": "#b8794f",
    "skin_shadow": "#9a603d",
    "hair": "#2b1d18",
    "white": "#fffaf0",
    "red": "#c23b33",
    "steel": "#c9cfd3",
    "steel_dark": "#8e989f",
}
C = PALETTE


def f(v: float) -> str:
    """Compact number formatting."""
    s = f"{v:.1f}"
    return s[:-2] if s.endswith(".0") else s


def stroke_attrs(sw: float | None, stroke: str = OUT) -> str:
    if sw is None or sw <= 0:
        return 'stroke="none"'
    return f'stroke="{stroke}" stroke-width="{f(sw)}" stroke-linejoin="round" stroke-linecap="round"'


def path(d: str, fill: str = "none", sw: float | None = LW_MID, stroke: str = OUT, extra: str = "") -> str:
    return f'<path d="{d}" fill="{fill}" {stroke_attrs(sw, stroke)} {extra}/>'


def rect(x, y, w, h, fill, rx=0, sw=LW_MID, stroke=OUT, extra="") -> str:
    r = f' rx="{f(rx)}"' if rx else ""
    return f'<rect x="{f(x)}" y="{f(y)}" width="{f(w)}" height="{f(h)}"{r} fill="{fill}" {stroke_attrs(sw, stroke)} {extra}/>'


def ellipse(cx, cy, rx, ry, fill, sw=LW_MID, stroke=OUT, extra="") -> str:
    return f'<ellipse cx="{f(cx)}" cy="{f(cy)}" rx="{f(rx)}" ry="{f(ry)}" fill="{fill}" {stroke_attrs(sw, stroke)} {extra}/>'


def circle(cx, cy, r, fill, sw=LW_MID, stroke=OUT, extra="") -> str:
    return ellipse(cx, cy, r, r, fill, sw, stroke, extra)


def poly(points, fill, sw=LW_MID, stroke=OUT, extra="") -> str:
    pts = " ".join(f"{f(x)},{f(y)}" for x, y in points)
    return f'<polygon points="{pts}" fill="{fill}" {stroke_attrs(sw, stroke)} {extra}/>'


def line(x1, y1, x2, y2, sw=LW_SMALL, stroke=OUT, extra="") -> str:
    return f'<line x1="{f(x1)}" y1="{f(y1)}" x2="{f(x2)}" y2="{f(y2)}" {stroke_attrs(sw, stroke)} {extra}/>'


def group(*items: str, transform: str = "", extra: str = "") -> str:
    t = f' transform="{transform}"' if transform else ""
    return f"<g{t} {extra}>" + "".join(items) + "</g>"


def shadow(cx, cy, rx, ry, opacity=0.28) -> str:
    """Soft blurred contact shadow."""
    return f'<ellipse cx="{f(cx)}" cy="{f(cy)}" rx="{f(rx)}" ry="{f(ry)}" fill="#3a2214" opacity="{opacity}" filter="url(#soft)"/>'


def shade(points, opacity=0.18, color="#3a2214") -> str:
    """Flat shading overlay polygon (no outline)."""
    return poly(points, color, sw=None, extra=f'opacity="{opacity}"')


@dataclass
class Box:
    x: float
    y: float
    w: float
    h: float

    def grow(self, d: float) -> "Box":
        return Box(self.x - d, self.y - d, self.w + 2 * d, self.h + 2 * d)

    def union(self, o: "Box") -> "Box":
        x0, y0 = min(self.x, o.x), min(self.y, o.y)
        x1, y1 = max(self.x + self.w, o.x + o.w), max(self.y + self.h, o.y + o.h)
        return Box(x0, y0, x1 - x0, y1 - y0)


@dataclass
class Placement:
    """Translate+uniform scale used to place a locally drawn element; maps local boxes to scene boxes."""
    tx: float
    ty: float
    s: float = 1.0

    @property
    def transform(self) -> str:
        return f"translate({f(self.tx)} {f(self.ty)}) scale({self.s:.4f})"

    def box(self, b: Box) -> Box:
        return Box(self.tx + b.x * self.s, self.ty + b.y * self.s, b.w * self.s, b.h * self.s)


@dataclass
class Scene:
    scene_id: str
    variant: str
    width: int
    height: int
    background: str = C["cream"]
    defs: list[str] = field(default_factory=list)
    layers: list[str] = field(default_factory=list)
    hotspots: dict[str, Box] = field(default_factory=dict)
    safe: Box | None = None

    def add(self, *items: str) -> None:
        self.layers.extend(items)

    def hotspot(self, hotspot_id: str, box: Box) -> None:
        self.hotspots[hotspot_id] = box

    def svg(self) -> str:
        return svg_doc(self.width, self.height, self.defs, self.layers, self.background)

    def layout_json(self) -> dict:
        def norm(b: Box) -> dict:
            return {
                "left": round(b.x / self.width, 4),
                "top": round(b.y / self.height, 4),
                "right": round((b.x + b.w) / self.width, 4),
                "bottom": round((b.y + b.h) / self.height, 4),
            }
        safe = self.safe
        if safe is None:
            boxes = list(self.hotspots.values())
            safe = boxes[0]
            for b in boxes[1:]:
                safe = safe.union(b)
            safe = safe.grow(40)
        # Never extend the protected area beyond the picture itself.
        x0, y0 = max(0.0, safe.x), max(0.0, safe.y)
        x1, y1 = min(float(self.width), safe.x + safe.w), min(float(self.height), safe.y + safe.h)
        safe = Box(x0, y0, x1 - x0, y1 - y0)
        return {
            "scene": self.scene_id,
            "variant": self.variant,
            "image": f"scenes/{self.scene_id}_{self.variant}.webp",
            "width": self.width,
            "height": self.height,
            "safeArea": norm(safe),
            "hotspots": [{"id": k, "rect": norm(v)} for k, v in self.hotspots.items()],
        }


COMMON_DEFS = f"""
<filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="10"/></filter>
<filter id="softer" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="28"/></filter>
<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="40"/></filter>
<linearGradient id="gWood" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{C['wood_light']}"/><stop offset="1" stop-color="{C['wood']}"/></linearGradient>
<linearGradient id="gWoodV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{C['wood_light']}"/><stop offset="1" stop-color="{C['wood']}"/></linearGradient>
<linearGradient id="gWoodDark" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{C['wood']}"/><stop offset="1" stop-color="{C['wood_dark']}"/></linearGradient>
<linearGradient id="gBrass" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#f3d27a"/><stop offset=".55" stop-color="{C['brass']}"/><stop offset="1" stop-color="{C['brass_dark']}"/></linearGradient>
<linearGradient id="gTerracotta" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#d77a4c"/><stop offset="1" stop-color="{C['terracotta_dark']}"/></linearGradient>
<linearGradient id="gRoof" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#cf6a3e"/><stop offset="1" stop-color="{C['roof_dark']}"/></linearGradient>
<linearGradient id="gSky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{C['sky_top']}"/><stop offset="1" stop-color="{C['sky_bottom']}"/></linearGradient>
<linearGradient id="gWallCream" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fbf0d8"/><stop offset="1" stop-color="{C['cream_dark']}"/></linearGradient>
<linearGradient id="gWallOchre" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f6dfaf"/><stop offset="1" stop-color="{C['ochre_wall_dark']}"/></linearGradient>
<linearGradient id="gLeaf" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{C['leaf_light']}"/><stop offset="1" stop-color="{C['leaf']}"/></linearGradient>
<linearGradient id="gSteel" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#eef1f2"/><stop offset=".5" stop-color="{C['steel']}"/><stop offset="1" stop-color="{C['steel_dark']}"/></linearGradient>
<linearGradient id="gGrass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{C['grass']}"/><stop offset="1" stop-color="{C['grass_dark']}"/></linearGradient>
<linearGradient id="gSand" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f0d9a8"/><stop offset="1" stop-color="{C['sand_dark']}"/></linearGradient>
<linearGradient id="gTeal" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3ea39d"/><stop offset="1" stop-color="{C['teal_dark']}"/></linearGradient>
<radialGradient id="gWarmLight" cx=".5" cy=".5" r=".5"><stop offset="0" stop-color="#fff3c9" stop-opacity=".75"/><stop offset="1" stop-color="#fff3c9" stop-opacity="0"/></radialGradient>
<radialGradient id="gVignette" cx=".5" cy=".5" r=".75"><stop offset=".6" stop-color="#3a2214" stop-opacity="0"/><stop offset="1" stop-color="#3a2214" stop-opacity=".28"/></radialGradient>
"""


def svg_doc(w: int, h: int, defs: list[str], layers: list[str], background: str) -> str:
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
        f"<defs>{COMMON_DEFS}{''.join(defs)}</defs>"
        f'<rect width="{w}" height="{h}" fill="{background}"/>'
        + "".join(layers)
        + f'<rect width="{w}" height="{h}" fill="url(#gVignette)"/>'
        + "</svg>"
    )


def sprite_doc(w: int, h: int, defs: list[str], layers: list[str]) -> str:
    """Transparent sprite: no background, no vignette."""
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
        f"<defs>{COMMON_DEFS}{''.join(defs)}</defs>" + "".join(layers) + "</svg>"
    )


def leaf_path(length: float, width: float) -> str:
    return f"M0,0 Q{f(width / 2)},{f(-length * 0.45)} 0,{f(-length)} Q{f(-width / 2)},{f(-length * 0.45)} 0,0 Z"


def leaf(x, y, length, width, angle, fill="url(#gLeaf)", sw=LW_SMALL) -> str:
    return group(
        path(leaf_path(length, width), fill, sw),
        line(0, -length * 0.08, 0, -length * 0.85, sw=1.4, stroke=C["leaf_dark"]),
        transform=f"translate({f(x)} {f(y)}) rotate({f(angle)})",
    )


def arc_points(cx, cy, r, a0, a1, n):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / (n - 1))),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / (n - 1)))) for i in range(n)]
