# SMRITI Asset Reference Pack v2.0

## Included
Generated concept atlases covering the current SMRITI scope: South Indian house exterior and veranda; bedroom with Memory Book and radio; kitchen cooking and dishwashing; sewing corner; bus stop/interior/conductor/ticket interface; market entrance; fruit and vegetable, spice, flower, and cloth stalls; NPC character concepts; UI icons; and example props.

## Brutally honest status
This is a **concept/reference pack, not a production-ready sprite library**. The images are multi-panel atlases with text and borders baked into them. They are useful for art direction and for giving a coding agent visual references. They are NOT clean individual transparent sprites, layered backgrounds, rigged characters, animation sheets, seamless textures, vector UI assets, or interactive game scenes.

Do not put an entire atlas into the game as a background. Do not crop random objects and claim they are production-ready without checking edge bleed, resolution, transparency, text, panel borders, consistency, and licensing.

## Correct production workflow
1. Choose one master style reference and approve the design.
2. Generate each environment as a separate text-free background/scene illustration.
3. Generate each NPC as a separate consistent character sheet with required expressions/poses and transparent background where supported.
4. Generate individual props or controlled sprite sheets with uniform cell sizes, no labels, and clean spacing.
5. Remove backgrounds, correct edges, export individual assets, name consistently, and check visual scale.
6. Build actual UI in the engine; never bake interface text into the art.
7. Test in the running game on phone portrait, landscape, and tablet.

## Intended product scope
Preset-object navigation, not open-world exploration. House hub with veranda, bedroom (Memory Book + caregiver-loaded radio), kitchen (cook + wash dishes), sewing corner (Stitch & Create), front-door destination selection, bus stop/interior/conductor and payment interaction, and four market stalls: produce, spices/pantry, flowers/garlands, cloth/textiles.

## Safety/design notes
Keep assistance optional but immediately available. No failure feedback. Keep music playback user-controlled and easy to stop. Record interaction events only with transparent caregiver/user consent and never infer clinical decline from raw mistakes.
