# Star Puzzle — Third-Party Licenses Audit

Star Puzzle is a GPL-3.0 project (a fork of 1010! Klooni by LonamiWebs,
https://github.com/LonamiWebs/Klooni10), rewritten in Kotlin + Jetpack Compose.
Full project license: COPYING / GPL-3.0.

## Bundled audio assets (native/app/src/main/res/raw + assets/audio)
Sound effects: Kenney "Interface Sounds", https://kenney.nl/assets/interface-sounds
(Mirror used: https://github.com/Calinou/kenney-interface-sounds)
License: Creative Commons Zero (CC0 1.0). Author: Kenney (Kenney.nl).
Commercial use: YES. Attribution required: NO.
Per-file mapping: see native/app/src/main/assets/audio/LICENSES.md.
Background music bgm_main.ogg: "Going Up" by ansimuz, source:
https://opengameart.org/content/going-up-adventure-chiptune
License: CC0 1.0. Commercial use: YES. Attribution required: NO.
Replaced the old ambient space loop (bgm_space.ogg) per owner direction (Oct 2026).

## Bundled code dependencies (Gradle)
AndroidX (Compose, Activity, Lifecycle, Material3): Apache-2.0
— Copyright The Android Open Source Project.
Kotlin stdlib / Compose compiler: Apache-2.0 — Copyright JetBrains.
No Firebase, no analytics, no networking libraries — the game is offline-first.

## Not used (and why)
No code, artwork, or audio copied from Block Blast, Toon Blast, Royal Match,
or any commercial game. Reference repositories listed in the research brief
were studied for engineering patterns only; permissive-licensed code
(Apache-2.0/MIT) may be adapted with notice, GPL code only in compliance
with GPL-3.0, and unclear-license material is never used.
