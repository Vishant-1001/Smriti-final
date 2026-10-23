"""Regenerates docs/ASSET_MANIFEST.csv from the files actually in the repository.

Usage (repo root): python -I art_source/generator/asset_manifest.py
"""
import csv
import glob
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
os.chdir(ROOT)
ASSETS = "app/src/main/assets"
ORIGINAL = "Original; drawn as code in art_source/generator (SVG rendered with resvg)"

USE = {
    "scenes": "Scene background",
    "characters": "SMRITI Guide sprite frame",
    "props": "Radio screen illustration",
    "memory": "Memory Book sample page picture",
}

rows = []
for p in sorted(glob.glob(ASSETS + "/**/*", recursive=True)):
    if os.path.isdir(p):
        continue
    rel = p.replace(os.sep, "/")
    name, ext = os.path.splitext(os.path.basename(p))
    folder = rel.split("/")[-2]
    if ext == ".webp":
        im = Image.open(p)
        alpha = "yes" if im.mode in ("RGBA", "LA") else "no (opaque)"
        use = USE[folder]
        if folder == "scenes":
            use += " (portrait)" if name.endswith("_port") else " (landscape)"
        rows.append([name, rel, folder, use, ORIGINAL, f"{im.width}x{im.height}", alpha, "approved_for_prototype",
                     "Text-free; simplified vector style compared with the painted references; not final production art"])
    elif ext == ".json":
        rows.append([name, rel, "layout", "Hotspot rectangles and safe area", "Generated with the scene art by build.py",
                     "-", "-", "approved_for_prototype", "Checked by SceneAssetsTest"])
    elif ext == ".ogg":
        rows.append([name, rel, "audio", "Radio demo tune (labelled 'test sound' in the UI)",
                     "Original; synthesised by art_source/generator/demo_audio.py", "mono 22.05 kHz OGG Vorbis", "-",
                     "approved_for_prototype", "Test audio only; real songs are added by caregivers"])

for p in sorted(glob.glob("app/src/main/res/font/*.ttf")):
    rows.append([os.path.basename(p)[:-4], p.replace(os.sep, "/"), "font", "UI text (Tamil / Latin)",
                 "Google Noto fonts, SIL OFL 1.1 (art_source/third_party/fonts/OFL.txt)", "-", "-",
                 "approved_for_release", "Unmodified upstream font files"])

rows.append(["ic_launcher_foreground", "app/src/main/res/drawable/ic_launcher_foreground.xml", "icon",
             "Launcher icon (kolam motif)", "Original vector drawable", "108dp", "yes", "approved_for_prototype", ""])

for p in sorted(glob.glob("art_source/reference_only/SMRITI_ASSET_REFERENCE_PACK_v2.0/01_Concept_Atlases/*.png")):
    im = Image.open(p)
    rows.append([os.path.basename(p)[:-4], p.replace(os.sep, "/"), "reference", "Art direction only",
                 "SMRITI_ASSET_REFERENCE_PACK_v2.0 generated concept atlas from the build kit; licence/provenance not verified",
                 f"{im.width}x{im.height}", "no", "reference",
                 "Multi-panel with baked labels; never shipped (SceneAssetsTest guards runtime assets)"])
rows.append(["SMRITI_ASSET_REFERENCE_PACK_v2.0", "art_source/reference_only/SMRITI_ASSET_REFERENCE_PACK_v2.0.zip",
             "reference", "Original archive as supplied", "Build kit v1.2", "-", "-", "reference", "Preserved unmodified"])

with open("docs/ASSET_MANIFEST.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["asset_id", "path", "category", "intended_use", "source_provenance", "dimensions", "alpha", "status", "review_notes"])
    w.writerows(rows)
print(f"{len(rows)} rows")
