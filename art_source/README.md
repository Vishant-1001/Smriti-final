# art_source

| Folder | Contents | Shipped in the app? |
|---|---|---|
| `generator/` | `svgkit.py` (drawing helpers and palette), `props.py` (furniture, plants, architecture), `scenes.py` (exterior, veranda, bedroom in landscape and portrait), `characters.py` (Guide frames, radio, Memory Book samples), `build.py` (renders everything into `app/src/main/assets`), `demo_audio.py` (synthesises the two demo radio tunes), `asset_manifest.py` (regenerates `docs/ASSET_MANIFEST.csv`) | Its *outputs* are |
| `tools/` | `render.mjs` (SVG → PNG with resvg; `npm ci` first), `export_screenshots.py` | No |
| `reference_only/` | The supplied `SMRITI_ASSET_REFERENCE_PACK_v2.0.zip` and its extracted concept atlases, unmodified | **Never** |
| `third_party/fonts/` | Noto Sans and Noto Sans Tamil TTFs and the SIL OFL 1.1 licence | Copies are in `app/src/main/res/font/` |
| `build/` | Intermediate SVG and PNG files (git-ignored) | No |

Rebuild the art with `python -I art_source/generator/build.py`. Then run `./gradlew testDebugUnitTest`, which checks hotspots against the code.
