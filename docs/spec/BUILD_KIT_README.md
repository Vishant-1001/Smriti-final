# SMRITI From-Scratch Build Kit v1.2 — Kotlin / Jetpack Compose

## Current technology decision

SMRITI is being built as a **native Android app using Kotlin and Jetpack Compose**. It is not a Godot/GDScript project. The current design uses preset illustrated scenes, tappable objects, compact UI, dialogue, voice assistance, and small stateful activities; it does not need a free-roaming 3D game engine.

## Target repository

`https://github.com/Vishant-1001/Smriti-final.git`

Build from scratch in this separate repository. Do not import code from the old prototype. The project and Gradle files belong at the repository root, not inside a nested app folder beyond the standard `app/` Android module.

## Contents

- `MASTER_BUILD_PROMPT.md` — complete Kotlin/Android implementation prompt with GitHub workflow, milestones, visual asset rules, product path, accessibility, privacy and QA acceptance gates.
- `SMRITI_ASSET_REFERENCE_PACK_v2.0.zip` — generated scene and character concept atlases to guide the art pipeline.

## How to use

1. Clone `Smriti-final` and open the repository root in your coding agent.
2. Give the agent both files from this kit.
3. Tell it to follow `MASTER_BUILD_PROMPT.md` as the controlling specification.
4. Require it to inspect and verify the Git repository and Android toolchain, build a Kotlin/Jetpack Compose project at the root, and implement the exterior → veranda → bedroom vertical slice first.
5. Require a successful build and relevant tests before each commit/push. Push only to `origin` at the exact target URL and never force-push.

## Honest limitations

The art ZIP contains generated concept atlases and references, not a full set of isolated, transparent, animation-ready production assets. The prompt requires the coding agent to keep these outside runtime assets and to create/clean scene illustrations, characters, and props with a status manifest. This kit does not itself create code in GitHub and does not prove that the app has been built, tested, or pushed.
