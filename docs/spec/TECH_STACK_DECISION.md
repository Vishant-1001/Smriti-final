# SMRITI technical stack decision

**Decision:** Kotlin + Jetpack Compose, native Android.

**Reason:** the confirmed game direction uses preset 2D-comic scenes, room/destination selection, tappable object hotspots, character dialogue, audio playback, a radio, a Memory Book, shopping/inventory state, and short minigames. These fit native Android UI and app lifecycle APIs well. There is no free-roaming world, real-time 3D camera, or physics system that justifies introducing a game engine.

**UI/rendering pattern:** scene illustrations are background artwork; Compose renders all labels, dialogue, lists, controls, shopping status, ticket/payment interface and accessible alternatives dynamically. Hotspots are positioned relative to the actual displayed image bounds so they remain aligned across portrait, landscape, tablets and resizing.

**Not part of this build:** Godot/GDScript, Unity, Unreal, 3D open-world mechanics, cloud-only media services, mandatory speech recognition, and live model-based adaptation.

**Build verification:** the coding agent must document actual JDK, Gradle wrapper, Android Gradle Plugin, Compose and Android SDK versions, run the build/test commands it can execute, and report any device/emulator checks it could not perform. Do not claim voice works offline unless the required Tamil/English voice or bundled audio has been verified on target devices.
