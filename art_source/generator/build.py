"""Build all SMRITI runtime art from source.

Usage (from the repository root):
    python -I art_source/generator/build.py

Writes SVG sources to art_source/build/svg/, renders them with art_source/tools/render.mjs
(resvg), converts to WebP and installs them under app/src/main/assets/. Scene hotspot layouts
are written next to the scene images as JSON.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import characters  # noqa: E402
import items  # noqa: E402
import people  # noqa: E402
import scenes  # noqa: E402
import scenes2  # noqa: E402
import props as props_mod  # noqa: E402
from svgkit import sprite_doc  # noqa: E402

ROOT = HERE.parent.parent
BUILD = ROOT / "art_source" / "build"
SVG_DIR = BUILD / "svg"
PNG_DIR = BUILD / "png"
ASSETS = ROOT / "app" / "src" / "main" / "assets"


def main() -> None:
    from PIL import Image

    SVG_DIR.mkdir(parents=True, exist_ok=True)
    PNG_DIR.mkdir(parents=True, exist_ok=True)
    jobs: list[tuple[str, str, str]] = []  # (svg name, asset relative path, format)
    layouts = []

    for builder in (scenes.exterior, scenes.veranda, scenes.bedroom, *scenes2.BUILDERS):
        for variant in ("land", "port"):
            sc = builder(variant)
            name = f"{sc.scene_id}_{variant}"
            (SVG_DIR / f"{name}.svg").write_text(sc.svg(), encoding="utf-8")
            jobs.append((name, f"scenes/{name}.webp", "scene"))
            layouts.append(sc.layout_json())

    for char_id, person in people.CHARACTERS.items():
        for frame in ("neutral", "speaking"):
            name = f"{char_id}_{frame}"
            (SVG_DIR / f"{name}.svg").write_text(sprite_doc(512, 512, [], [people.bust(person, frame)]), encoding="utf-8")
            jobs.append((name, f"characters/{name}.webp", "sprite"))

    # 320x320 sprites: shop items, kitchen utensils (one file per state), dishes, fabrics and creations
    def sprite(name: str, folder: str, svg: str, scale: float = 1.0) -> None:
        body = f'<g transform="translate(160 172) scale({scale})">{svg}</g>'
        (SVG_DIR / f"{name}.svg").write_text(sprite_doc(320, 320, [items.fabric_defs()], [body]), encoding="utf-8")
        jobs.append((name, f"{folder}/{name}.webp", "sprite"))

    for item_id, draw in items.ITEM_ICONS.items():
        sprite(item_id, "items", draw())
    for st in ("empty", "powder", "brewing"):
        sprite(f"filter_{st}", "kitchen", items.filter_vessel(st), 0.95)
    for st in ("empty", "decoction", "coffee"):
        sprite(f"tumbler_{st}", "kitchen", items.tumbler(st), 0.95)
    sprite("kettle", "kitchen", items.kettle(), 0.85)
    sprite("milk_pot", "kitchen", items.milk_pot(), 0.95)
    for kind, draw in (("plate", items.plate), ("cup", items.cup), ("bowl", items.bowl)):
        sprite(f"{kind}_dirty", "kitchen", draw(True), 1.15)
        sprite(f"{kind}_clean", "kitchen", draw(False), 1.15)
    for fid in items.FABRICS:
        sprite(f"swatch_{fid}", "sewing", items.swatch(fid), 1.2)
        for kind in ("bag", "pillow", "shirt"):
            sprite(f"{kind}_{fid}", "sewing", items.creation(kind, fid), 1.05)
    for kind in ("bag", "pillow", "shirt"):
        sprite(f"template_{kind}", "sewing", items.template_outline(kind), 1.05)

    sprite("sewing_machine", "props", props_mod.sewing_machine(1.0), 0.95)

    (SVG_DIR / "radio.svg").write_text(sprite_doc(800, 600, [], [characters.radio_sprite()]), encoding="utf-8")
    jobs.append(("radio", "props/radio.webp", "sprite"))

    for kind in ("kolam", "coffee", "jasmine"):
        name = f"memory_sample_{kind}"
        (SVG_DIR / f"{name}.svg").write_text(sprite_doc(600, 450, [], [characters.vignette(kind)]), encoding="utf-8")
        jobs.append((name, f"memory/{name}.webp", "scene"))

    render = ROOT / "art_source" / "tools" / "render.mjs"
    subprocess.run(["node", str(render), str(SVG_DIR), str(PNG_DIR)], check=True)

    for name, rel, kind in jobs:
        src = PNG_DIR / f"{name}.png"
        dst = ASSETS / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        im = Image.open(src)
        if kind == "scene":
            im = im.convert("RGB")
            im.save(dst, "WEBP", quality=88, method=6)
        else:
            im.save(dst, "WEBP", lossless=True, method=6)
        print(f"{rel}: {im.size[0]}x{im.size[1]} {im.mode} {os.path.getsize(dst) // 1024} KB")

    for lay in layouts:
        out = ASSETS / "scenes" / f"{lay['scene']}_{lay['variant']}.json"
        out.write_text(json.dumps(lay, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(layouts)} layouts")


if __name__ == "__main__":
    main()
