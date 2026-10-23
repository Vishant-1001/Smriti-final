# Art bible

## Direction chosen

All seven supplied concept atlases (`art_source/reference_only/`) share one look: warm soft 2D-comic illustration, clean dark-brown linework, gentle shading, terracotta roofs and floors, teal and indigo accents, and lush greenery. **Atlas 05** (labelled panels, terracotta veranda with an open side, Memory Book and radio on a wooden desk) and **atlas 07** (veranda layout and bedroom framing) were the main composition references. The atlases are generated concept boards with baked-in labels and panels of about 300×220 px. They were **not** cropped or reused: every runtime image is newly drawn.

## How the art is made

There was no image-generation tool in the build environment, so the art is hand-authored vector illustration written as code (`art_source/generator/*.py`). It is rendered to PNG with resvg and saved as WebP. This keeps the art reproducible and editable, and lets hotspot coordinates come from the same source. The trade-off: it is simpler and flatter than the painted atlases, with less texture and fewer details. Treat it as **approved for prototype**, not final.

## Palette (tokens shared by art and UI)

| Token | Hex | Use |
|---|---|---|
| outline | `#4A2E22` | The one line colour |
| cream / paper | `#F6E7C8` / `#FFF9EE` | Walls, panels |
| ochre wall | `#EFD29A` | Interiors |
| terracotta / dark | `#C8643B` / `#9E4527` | Floors, pots, roof |
| UI terracotta | `#A44A26` | Primary buttons (white text ≈5.6:1) |
| leaf / light / dark | `#5E8C4A` / `#86B25E` / `#3F6B3A` | Plants; UI secondary `#3F6B3A` (white text ≈6.3:1) |
| indigo | `#3C4C8C` | Accents, "sample" badges |
| teal | `#2F8A85` | Shutters, Guide's saree |
| wood / light / dark | `#8A5230` / `#A96A3F` / `#5E3520` | Furniture, doors |
| brass | `#E2B04A` | Lamps, knobs |
| ink | `#3A2418` | Body text on paper (≈13:1) |

## Line and shading rules

- One outline colour. Three weights at 1920 px scene width: 4 px (large forms), 3 px (objects), 2 px (detail). Joins and caps are rounded.
- Shading uses vertical or horizontal two-stop gradients plus flat 12–22 % dark overlays on the shadow side. Contact shadows are blurred ellipses.
- Light is warm and comes from windows or doorways (soft radial glow), with a gentle vignette on every scene.
- No blinking, flashing or busy patterns behind interactive objects. Kolam and sheet patterns sit away from hotspots.

## Composition rules

- Scenes have one-point perspective with the vanishing point near the centre, and the floor takes the lower 35–45 %.
- Interactive objects are large and well separated with clear silhouettes, and placed inside the scene's **safe area** (recorded in the layout JSON). Hotspots never overlap; a test enforces this.
- The top ~84 dp (136 dp on narrow phones) is reserved for the HUD. The runtime keeps hotspots below it.
- Portrait variants are re-composed, not cropped: the same props are rearranged for 1080×2400.

## Characters

**SMRITI Guide (canonical):** an adult Tamil woman, about 40s. Medium-brown skin `#B8794F`, centre-parted black hair in a low bun with a jasmine strand, small red bindi, gold ear studs and nose stud. She wears a teal cotton saree with a gold border and a maroon blouse. Her expression is calm and friendly, never exaggerated. There are two frames, `neutral` and `speaking` (mouth open), alternated every 190 ms only while speech is playing, and never with Reduced motion on. **Other characters** are drawn from the same parameterised bust template (`art_source/generator/people.py`). Every face therefore shares proportions, line weights and eyes. Each has the same two frames.

| ID | Appearance |
|---|---|
| `conductor` | Adult man, khaki uniform, cap with brass badge, satchel strap, moustache |
| `vendor_produce` | Adult man, white shirt, green apron, moustache |
| `vendor_spice` | Older man, grey hair, glasses, cream shirt, terracotta apron |
| `vendor_flower` | Adult woman, pink saree with gold border, green blouse, jasmine in her hair, bindi |
| `vendor_cloth` | Adult man, light-blue shirt, moustache |

Vendors and the conductor appear in their scenes from the waist up: the counter or the seat backs hide the lower body. The dialogue portrait is the same drawing.

## Cultural notes (to be reviewed by people from the community)

- **House:** single-storey South Indian house with a hipped Mangalore-tile roof, wooden pillars, thinnai (raised platforms), teal wooden shutters, a kolam at the threshold, coconut palms, banana plant, tulsi.
- **Interior:** red-oxide or terracotta floor, wooden ceiling beams, brass kuthu vilakku (unlit), steel and brass vessels, a black hand-crank sewing machine.
- **Memory Book samples:** a kolam, filter coffee in a davara-tumbler, a jasmine string. There are deliberately **no people** in sample pages and picture frames, so nothing implies someone's family.
- **Kitchen:** wall tiles, a two-burner stove with a South Indian coffee filter and a tumbler-davara, steel and brass vessels. There are no knives or flames.
- **Market:** striped awnings, cane baskets, a jasmine string, marigolds, turmeric and chilli in bowls, folded cotton. The four fabrics differ by *pattern* (stripes, dots, checks, leaves), not only by colour.
- **Bus:** a generic city bus with blank number boards; the app draws the numbers. No real transport company's branding is used.
- None of this has been reviewed by Tamil cultural or community reviewers yet. That is an open item in the risk register.

## Asset sizes and naming

| Kind | Size | Format | Path |
|---|---|---|---|
| Scene, landscape | 1920×1080 | WebP q88, opaque | `assets/scenes/<scene>_land.webp` |
| Scene, portrait | 1080×2400 | WebP q88, opaque | `assets/scenes/<scene>_port.webp` |
| Hotspot layout | — | JSON | `assets/scenes/<scene>_<variant>.json` |
| Character frame | 512×512 | WebP lossless, alpha | `assets/characters/<character>_<frame>.webp` |
| Prop | 800×600 | WebP lossless, alpha | `assets/props/<prop>.webp` |
| Memory Book sample | 600×450 | WebP q88 | `assets/memory/memory_sample_<key>.webp` |
| Shop item | 320×320 | WebP lossless, alpha | `assets/items/<item_id>.webp` (ID = catalog ID = stall hotspot ID) |
| Kitchen utensil/dish state | 320×320 | WebP lossless, alpha | `assets/kitchen/<thing>_<state>.webp` (e.g. `filter_powder`, `plate_dirty`) |
| Sewing swatch/template/creation | 320×320 | WebP lossless, alpha | `assets/sewing/swatch_<fabric>`, `template_<kind>`, `<kind>_<fabric>` |

IDs are lower_snake_case and stable. Code never uses file names as identity: scenes, hotspots, pages and tracks have their own IDs.

## Export rules

No text, labels, prices, buttons or UI in any image; Compose draws all text. No panel borders, watermarks or neighbouring assets. Sprites have transparent backgrounds. Reference atlases never go into `app/src/main/assets/`, and a test enforces this.
