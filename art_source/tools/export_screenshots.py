"""Converts Roborazzi PNG recordings to compact WebP copies that are committed.

Usage (repo root), after ./gradlew recordRoborazziDebug:
    python -I art_source/tools/export_screenshots.py
Reads docs/screenshots/roborazzi/<device>/*.png (git-ignored) and writes docs/screenshots/jvm/<device>/*.webp.
"""
import glob
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "docs", "screenshots", "roborazzi")
DST = os.path.join(ROOT, "docs", "screenshots", "jvm")

count = 0
for png in sorted(glob.glob(os.path.join(SRC, "*", "*.png"))):
    device = os.path.basename(os.path.dirname(png))
    out_dir = os.path.join(DST, device)
    os.makedirs(out_dir, exist_ok=True)
    im = Image.open(png).convert("RGB")
    im.thumbnail((1400, 1400))
    im.save(os.path.join(out_dir, os.path.basename(png)[:-4] + ".webp"), quality=80)
    count += 1
print(f"exported {count} screenshots to {DST}")
