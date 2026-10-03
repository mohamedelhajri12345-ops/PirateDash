# Star Puzzle — UX Copy & Game-Feel Specification

**Document Version:** 1.0.0  
**Target Platform:** Android (Native Kotlin + Jetpack Compose)  
**Theme & Identity:** Premium Casual Night-Sky Block Puzzle  
**Author:** Senior UX Writer & Game-Feel Designer  

---

## 1. HUD & In-Game UI Microcopy

All text follows Title Case for headlines/buttons and UPPERCASE for technical HUD badges. Language is warm, concise, and aligned with the night-sky stargazing theme.

### 1.1 HUD Labels & Counters
| Element | Badge Copy | Description / Context |
| :--- | :--- | :--- |
| **Score Label** | `SCORE` | Upper HUD left label above live score counter |
| **Goal Label** | `GOAL` | Upper HUD right label above level target value |
| **Moves Left** | `MOVES` | Counter label for turn-limited levels |
| **Time Left** | `TIME` | Timer label for time-attack levels (formatted as `0:30`) |
| **Level Badge** | `LEVEL {ID}` | Header bar text (e.g., `LEVEL 42`) |
| **Daily Badge** | `DAILY CHALLENGE` | Header bar text for daily puzzle mode |

---

### 1.2 Objective Strings (`LevelDefinition.kt` Microcopy)
Short, high-readability objective cards presented at level start and on HUD:

* **Score Target:** `SCORE {targetScore}` (e.g., `SCORE 60`)
* **Line Clear Target:** `CLEAR {targetLines} LINES` (e.g., `CLEAR 2 LINES`)
* **Time Challenge Target:** `SCORE {targetScore} IN {timeLimit}s` (e.g., `SCORE 60 IN 30s`)
* **Combo Target:** `COMBO x{targetCombo}` (e.g., `COMBO x3`)
* **Survival / Boss Target:** `SURVIVE`

---

### 1.3 Win Screen Microcopy
* **Headline:** `Level Complete!`
* **Sub-Headline / Cheer:** `Starlight Restored!`
* **Star Rank Labels:**
  * **1 Star (40% allowance left):** `Cleared!`
  * **2 Stars (20% allowance left):** `Brilliant!`
  * **3 Stars (Mastery):** `Starlight Master!`
* **Coin Reward Badge:** `+50 Coins`
* **Primary Action:** `Next Level`
* **Secondary Action:** `World Map`

---

### 1.4 Lose Screen Microcopy (Warmer Sky-Themed Alternative)
* **Headline Proposel (Replaces cold "No space left!"):**  
  * **Primary:** `The Sky Is Full!`
  * **Alternative Option A:** `Constellation Filled`
  * **Alternative Option B:** `No Space in the Sky`
* **Body / Encouragement:** `No valid positions remain for your tray pieces.`
* **Buttons:**
  * **Primary Button:** `Retry` *(Glossy primary gold CTA)*
  * **Secondary Button:** `World Map` *(Outlined glass card CTA)*
  * **Tertiary Action:** `Main Menu`

---

### 1.5 Dialogs & Overlay Microcopy
* **Quit Level Confirmation:**
  * Title: `Leave Level?`
  * Body: `Your current progress in this constellation will be lost.`
  * Actions: `Keep Playing` (Cancel) | `Quit Level` (Confirm)
* **Restart Level Confirmation:**
  * Title: `Restart Level?`
  * Body: `Reset board and restart from the beginning?`
  * Actions: `Cancel` | `Restart`
* **Booster Purchase Dialog:**
  * Title: `Get Booster`
  * Body: `Activate booster for 50 Coins?`
  * Actions: `Not Now` | `Use (50 Coins)`

---

## 2. Color-Theme (Skin) System Specification

6 curated palettes designed for dark-mode night-sky gameplay. Each skin includes a background gradient tint, primary UI accent color, and exactly 9 piece colors corresponding to the 9 board shape indices (0 through 8).

All palettes are **Halal-safe** (no occult symbols, demonic motifs, or gambling aesthetic) and maintain WCAG AAA contrast against dark board tiles (`#16204A` / `#1F2C5E`).

---

### Palette 1: Starlight (Default Night Sky)
*Description: The canonical Star Puzzle palette featuring vivid cosmic gems on deep indigo night skies.*
* **Background Gradient Tint:** Top `#0B1026` | Bottom `#1B2C5C`
* **Primary Accent:** `#FFD54F` (Starlight Gold)
* **Surface Card:** `#16204A`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#EF5350` (Ruby Red)
  2. Shape 1 (2x2): `#FFA726` (Amber Orange)
  3. Shape 2 (3x3): `#FFEE58` (Solar Yellow)
  4. Shape 3 (1x2): `#66BB6A` (Emerald Green)
  5. Shape 4 (1x3): `#42A5F5` (Sapphire Blue)
  6. Shape 5 (1x4): `#7E57C2` (Amethyst Purple)
  7. Shape 6 (1x5): `#EC407A` (Cosmic Pink)
  8. Shape 7 (L 2x3): `#26C6DA` (Nebula Cyan)
  9. Shape 8 (L 3x3): `#AB47BC` (Deep Magenta)

---

### Palette 2: Rose (Dusty Pink & Sunset Bloom)
*Description: Soft, elegant pinks and warm magenta hues reminiscent of twilight rosy skies.*
* **Background Gradient Tint:** Top `#1A0F1A` | Bottom `#2D122B`
* **Primary Accent:** `#F48FB1` (Rose Gold)
* **Surface Card:** `#2A1829`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#FF80AB` (Soft Rose)
  2. Shape 1 (2x2): `#FF4081` (Bright Rose)
  3. Shape 2 (3x3): `#F8BBD0` (Blush Pink)
  4. Shape 3 (1x2): `#F48FB1` (Pastel Rose)
  5. Shape 4 (1x3): `#F06292` (Coral Pink)
  6. Shape 5 (1x4): `#E91E63` (Deep Rose)
  7. Shape 6 (1x5): `#D81B60` (Crimson Pink)
  8. Shape 7 (L 2x3): `#C2185B` (Dark Magenta)
  9. Shape 8 (L 3x3): `#AD1457` (Plum Velvet)

---

### Palette 3: Ice (Arctic Frost & Glacial Aurora)
*Description: Cool, crisp icy blues and mint cyans reflecting arctic midnight skies.*
* **Background Gradient Tint:** Top `#0A192F` | Bottom `#112240`
* **Primary Accent:** `#80DEEA` (Polar Ice)
* **Surface Card:** `#172A45`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#E0F7FA` (Frost White)
  2. Shape 1 (2x2): `#B2EBF2` (Ice Aqua)
  3. Shape 2 (3x3): `#80DEEA` (Glacial Cyan)
  4. Shape 3 (1x2): `#4DD0E1` (Bright Cyan)
  5. Shape 4 (1x3): `#26C6DA` (Deep Aqua)
  6. Shape 5 (1x4): `#00BCD4` (Ocean Blue)
  7. Shape 6 (1x5): `#00ACC1` (Deep Cyan)
  8. Shape 7 (L 2x3): `#0097A7` (Arctic Blue)
  9. Shape 8 (L 3x3): `#00838F` (Frost Sapphire)

---

### Palette 4: Neon (Cyber Night Spectrum)
*Description: High-contrast electric hues engineered for ultra-vivid readability.*
* **Background Gradient Tint:** Top `#080811` | Bottom `#121224`
* **Primary Accent:** `#00E676` (Neon Emerald)
* **Surface Card:** `#18182E`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#FF007F` (Neon Pink)
  2. Shape 1 (2x2): `#00E5FF` (Electric Cyan)
  3. Shape 2 (3x3): `#76FF03` (Lime Neon)
  4. Shape 3 (1x2): `#FFEA00` (Cyber Yellow)
  5. Shape 4 (1x3): `#FF6D00` (Neon Amber)
  6. Shape 5 (1x4): `#D500F9` (Electric Violet)
  7. Shape 6 (1x5): `#1DE9B6` (Mint Neon)
  8. Shape 7 (L 2x3): `#FF1744` (Laser Red)
  9. Shape 8 (L 3x3): `#651FFF` (Deep Indigo Neon)

---

### Palette 5: Mono (Platinum & Obsidian Luxury)
*Description: A refined monochrome theme featuring sleek silver, platinum, and charcoal shades.*
* **Background Gradient Tint:** Top `#121212` | Bottom `#212121`
* **Primary Accent:** `#E0E0E0` (Platinum White)
* **Surface Card:** `#2C2C2C`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#FFFFFF` (Pure White)
  2. Shape 1 (2x2): `#F5F5F5` (Bright Platinum)
  3. Shape 2 (3x3): `#EEEEEE` (Silver Fog)
  4. Shape 3 (1x2): `#E0E0E0` (Metallic Grey)
  5. Shape 4 (1x3): `#BDBDBD` (Slate Grey)
  6. Shape 5 (1x4): `#9E9E9E` (Ash Grey)
  7. Shape 6 (1x5): `#757575` (Steel Grey)
  8. Shape 7 (L 2x3): `#616161` (Charcoal)
  9. Shape 8 (L 3x3): `#424242` (Dark Obsidian)

---

### Palette 6: Gold (Celestial Royal Amber)
*Description: Warm golden stars and honeyed light inspired by royal astronomy.*
* **Background Gradient Tint:** Top `#1C1608` | Bottom `#33270D`
* **Primary Accent:** `#FFD700` (Celestial Gold)
* **Surface Card:** `#2D2310`
* **9 Piece Hex Colors:**
  1. Shape 0 (1x1): `#FFF59D` (Pale Gold)
  2. Shape 1 (2x2): `#FFE082` (Warm Amber)
  3. Shape 2 (3x3): `#FFD54F` (Sun Gold)
  4. Shape 3 (1x2): `#FFCA28` (Honey Gold)
  5. Shape 4 (1x3): `#FFC107` (Royal Amber)
  6. Shape 5 (1x4): `#FFB300` (Deep Amber)
  7. Shape 6 (1x5): `#FFA000` (Dark Honey)
  8. Shape 7 (L 2x3): `#FF8F00` (Bronze Amber)
  9. Shape 8 (L 3x3): `#FF6F00` (Burnt Gold)

---

## 3. Booster Icon & Interaction Specification

Flat vector booster controls located adjacent to the piece tray at the bottom of the game HUD.

### 3.1 Move Booster (`MOVE`)
* **Function:** Undoes the last piece placement or returns the last placed piece to the tray.
* **Vector Icon Concept:** A sleek curved counter-clockwise arrow surrounding a subtle 2x2 grid cell line icon.
* **Microcopy:** `Undo Move`
* **In-Game Tooltip:** `Return your last piece to the tray`

### 3.2 Swap Booster (`SWAP`)
* **Function:** Rerolls the 3 tray pieces with 3 brand-new random pieces.
* **Vector Icon Concept:** Two circular arrows forming a dynamic ring around a piece block shape.
* **Microcopy:** `Swap Tray`
* **In-Game Tooltip:** `Refresh all 3 tray pieces`

### 3.3 Counter Badge Rules
1. **Positioning:** Top-right corner offset of the 48dp circular booster icon button.
2. **Visual Hierarchy:**
   * **Available (>0):** Solid primary accent background (`#FFD54F`), dark bold numeral font (10sp).
   * **Empty (0):** Small circular `+` badge in primary gold with surface background. Tapping opens booster refill prompt (`50 Coins`).
3. **Touch Target:** Minimum 48dp x 48dp accessible hit box with subtle haptic tap feedback.

---

## 4. Retention Beats for Global Audience (Halal-Safe & Ethical)

Designed with zero dark patterns, no gambling mechanics (no loot boxes, no mystery wheels), and no fake urgency timers.

### 4.1 Daily Streak Mechanics (`Starlight Journey`)
* **Naming:** `Starlight Journey`
* **Progression Rewards (7-Day Cycle):**
  * Day 1: `20 Coins` — *"Beginning your voyage"*
  * Day 2: `1 MOVE Booster` — *"Keen observation"*
  * Day 3: `40 Coins` — *"Steadfast focus"*
  * Day 4: `1 SWAP Booster` — *"Cosmic flexibility"*
  * Day 5: `60 Coins` — *"Starlight harmony"*
  * Day 6: `2 Boosters (1 Move + 1 Swap)` — *"Master's vision"*
  * Day 7: `Starlight Gift: 150 Coins + 2 Boosters` — *"Constellation Complete!"*

### 4.2 Mission Phrasing Examples
Honest, goal-oriented tasks framed with positive celestial language:
* **Daily Mission 1:** `Clear 10 Lines Today` — *"Clear 10 horizontal or vertical lines."*
* **Daily Mission 2:** `Achieve x3 Combo` — *"Trigger a triple line clear."*
* **Daily Mission 3:** `Complete Daily Challenge` — *"Finish today's featured puzzle."*
* **Weekly Mission:** `Gather 30 Stars` — *"Earn 30 stars across any world level."*

### 4.3 Milestone Celebration Texts
* **World Unlock:** `World {N} Unlocked!` — *"New constellations await in {WorldName}."*
* **Star Gate Reached:** `{N} Stars Collected!` — *"The pathway to the next galaxy is open."*
* **Century Milestone (Level 100/200/etc.):** `Level {N} Reached!` — *"A true master of the night sky."*
