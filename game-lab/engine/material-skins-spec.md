# Material Skin Framework Specification
**Project:** Star Puzzle (Kotlin + Jetpack Compose)  
**Squad:** Material Skin Framework Squad  
**Document:** `game-lab/engine/material-skins-spec.md`  
**Status:** Approved Engineering Foundation Architecture  

---

## 1. Executive Summary & Context

The legacy visual engine relied on 20 basic color palettes (`PieceSkins.kt`), which produced flat color swaps across identical rounded squares. The game owner rejected mere color swaps and requested tactile, rich **Material Skins**—including **Ice blocks**, **Fire blocks**, **Gems**, **Wood blocks**, **Candy**, and **Metal blocks**.

This specification defines the design-independent, video-independent **MaterialSkin Engineering Engine**. Instead of relying on static bitmap textures or heavy asset pipelines, the system renders tactile 3D materials **procedurally using Jetpack Compose Canvas primitives** (`DrawScope`). This approach guarantees:
- **Zero Asset Overhead:** 100% vector/gradient procedural rendering with zero bitmap files.
- **Infinite Resolution:** Flawless scaling from small 72dp tray items to high-density 120Hz displays.
- **Backwards Compatibility:** Legacy 20 color palettes are adapted as classic color themes, keeping saved player data (`ownedSkins` CSV) fully intact.
- **Premium Shop Upgrades:** New material skins integrate seamlessly into the shop catalog as premium tier unlockables.

---

## 2. Core Data Architecture

The material engine is built around three fundamental types: `OverlayPattern`, `MaterialSpec`, and `MaterialSkin`.

### 2.1 `OverlayPattern` Enum
Defines procedural texture effects rendered directly over the block surface:
```kotlin
enum class OverlayPattern {
    NONE,      // Clean metallic or minimal finish
    GLOSS,     // High specular spot gloss (ideal for Gems and Candy)
    MATTE,     // Diffuse surface texture / wood grain arcs
    SPARKLE,   // Four-pointed star embers / radiant sparkles (Fire/Magma)
    FROST      // Fractured ice crystal lines and frost dots
}
```

### 2.2 `MaterialSpec` Data Class
Encapsulates all physical and optical parameters required to draw a single cell block:
```kotlin
data class MaterialSpec(
    val baseColor: Color,
    val edgeColor: Color,
    val glossColor: Color = Color.White,
    val glossAlpha: Float = 0.25f,
    val cornerRadiusFraction: Float = 0.22f,
    val innerHighlightTopFraction: Float = 0.45f,
    val innerHighlightBottomFraction: Float = 0.10f,
    val shadowAlpha: Float = 0.35f,
    val overlayPattern: OverlayPattern = OverlayPattern.GLOSS,
    val specularSizeFraction: Float = 0.15f,
    val specularOffsetFraction: Offset = Offset(0.25f, 0.25f)
)
```

#### Parameter Contract:
- **`baseColor`**: Primary gradient color at the top/center of the block.
- **`edgeColor`**: Secondary gradient color toward the bottom/edges of the block.
- **`glossColor`**: Tint color for specular highlights and top bevel reflections.
- **`glossAlpha`**: Opacity multiplier for top gloss reflections `(0.0f..1.0f)`.
- **`cornerRadiusFraction`**: Corner rounding relative to cell size `(0.05f = sharp metal, 0.35f = pillowy candy)`.
- **`innerHighlightTopFraction`**: Vertical height fraction of the top light bevel `(0.0f..0.8f)`.
- **`innerHighlightBottomFraction`**: Vertical height fraction of the bottom depth shadow bevel `(0.0f..0.5f)`.
- **`shadowAlpha`**: Drop shadow opacity projected below the block `(0.0f..1.0f)`.
- **`overlayPattern`**: Procedural surface texture pattern enum.
- **`specularSizeFraction`**: Radius of specular highlight spot relative to cell width `(0.05f..0.5f)`.
- **`specularOffsetFraction`**: Normalized center offset `(x, y)` for the specular highlight center spot `(0.0..1.0)`.

### 2.3 `MaterialSkin` Data Class
A complete Material Skin contains an array of `MaterialSpec` objects corresponding to the `PIECE_COLOR_COUNT` (9 piece shapes/colors):
```kotlin
data class MaterialSkin(
    val id: Int,
    val name: String,
    val isPremium: Boolean,
    val price: Int,
    val materials: List<MaterialSpec>
) {
    init {
        require(materials.size == com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT) {
            "MaterialSkin '$name' must hold exactly PIECE_COLOR_COUNT (${Piece.PIECE_COLOR_COUNT}) material specs"
        }
    }
}
```

---

## 3. Pure Compose Canvas Rendering Pipeline

Rendering is performed by `DrawScope.drawMaterial(...)`, executing a multi-pass pipeline:

```
┌──────────────────────────────────────────────────────────┐
│ Pass 1: Drop Shadow (Offset black round rect with alpha)  │
├──────────────────────────────────────────────────────────┤
│ Pass 2: Base Body Gradient (Linear gradient top to bottom)│
├──────────────────────────────────────────────────────────┤
│ Pass 3: Procedural Overlay Pattern (Wood, Frost, Sparkle) │
├──────────────────────────────────────────────────────────┤
│ Pass 4: Top Light Bevel (Vertical white/gloss gradient)  │
├──────────────────────────────────────────────────────────┤
│ Pass 5: Bottom Shadow Bevel (Vertical black gradient)    │
├──────────────────────────────────────────────────────────┤
│ Pass 6: Specular Highlight Spot (Radial gloss gradient)  │
├──────────────────────────────────────────────────────────┤
│ Pass 7: Outer Edge / Volume Highlight (Stroke border)    │
└──────────────────────────────────────────────────────────┘
```

### Signature:
```kotlin
fun DrawScope.drawMaterial(
    topLeft: Offset,
    size: Size,
    spec: MaterialSpec,
    alpha: Float = 1.0f
)
```

---

## 4. Backwards Compatibility & Migration Strategy

1. **Legacy Catalog Integration:**
   The 20 legacy palettes in `PieceSkins.kt` (IDs 0..19) are preserved as **Color Themes** (`isPremium = false`). Each legacy `PieceSkin` is automatically converted into a classic `MaterialSkin` via helper adapter:
   ```kotlin
   fun PieceSkin.toClassicMaterialSkin(id: Int): MaterialSkin
   ```
2. **Premium Material Skins:**
   New Material Skins occupy IDs starting at 20+:
   - ID 20: `ICE` (Glacial Ice)
   - ID 21: `FIRE` (Hellfire Lava)
   - ID 22: `GEM` (Crystal Shard)
   - ID 23: `WOOD` (Ancient Oak)
   - ID 24: `CANDY` (Sweet Gummy)
   - ID 25: `METAL` (Chrome Steel)

3. **Store & Persistence Compatibility:**
   - Saved progress (`owned_skins` CSV in `PrefsSaveStore.kt`) continues to read integer IDs.
   - `GameProgress.ownedSkins()` and `GameProgress.buySkin()` work seamlessly with both legacy IDs (0..19) and new material IDs (20+).
   - Active skin selection (`SkinState.active`) maps directly to `MaterialSkinCatalog.getSkin(id)`.

---

## 5. Starter Material Specs (6 Demo Catalog)

Each starter material defines a distinct tactile profile across all 9 piece color indices:

1. **ICE (ID 20 - Glacial Ice):**
   - *Physical feel:* Cold, translucent, crystalline refraction.
   - *Gradients:* Light cyan to deep azure linear gradients.
   - *Parameters:* `cornerRadiusFraction = 0.20f`, `glossAlpha = 0.45f`, `overlayPattern = FROST`.
   - *Overlay:* Procedural icy cross-fracture lines and frost dots.

2. **FIRE (ID 21 - Hellfire Lava):**
   - *Physical feel:* Smoldering heat, molten lava core glow.
   - *Gradients:* Glowing orange-yellow to dark smoldering crimson red.
   - *Parameters:* `cornerRadiusFraction = 0.22f`, `glossAlpha = 0.50f`, `overlayPattern = SPARKLE`.
   - *Overlay:* Hot yellow core glow and radiant ember star sparkles.

3. **GEM (ID 22 - Crystal Shard):**
   - *Physical feel:* Highly saturated jewel, sharp faceted crystal edges.
   - *Gradients:* Deep rich jewel tones (Ruby, Sapphire, Emerald, Amethyst) to dark facets.
   - *Parameters:* `cornerRadiusFraction = 0.12f`, `glossAlpha = 0.70f`, `overlayPattern = GLOSS`.
   - *Overlay:* Hard specular glare dot and deep shadow facet bevels.

4. **WOOD (ID 23 - Ancient Oak):**
   - *Physical feel:* Organic warm timber, carved wooden block feel.
   - *Gradients:* Amber/brown wood hues to dark bark edge tints.
   - *Parameters:* `cornerRadiusFraction = 0.18f`, `glossAlpha = 0.10f`, `overlayPattern = MATTE`.
   - *Overlay:* Procedural curved wood-grain rings.

5. **CANDY (ID 24 - Sweet Gummy):**
   - *Physical feel:* Pillowy soft gummy candy, sugary gloss.
   - *Gradients:* Bright pastel pinks, sky blues, mint greens, lemon yellow.
   - *Parameters:* `cornerRadiusFraction = 0.35f`, `glossAlpha = 0.60f`, `overlayPattern = GLOSS`.
   - *Overlay:* Soft large radial bubble gloss highlight.

6. **METAL (ID 25 - Chrome Steel):**
   - *Physical feel:* Machined industrial steel, polished silver/aluminum finish.
   - *Gradients:* High-contrast grayscale metallic linear gradients.
   - *Parameters:* `cornerRadiusFraction = 0.08f`, `glossAlpha = 0.35f`, `overlayPattern = NONE`.
   - *Overlay:* Sharp linear bevel bands and crisp edge highlights.

---

## 6. Integration Roadmap (Zero-Risk GameScreen Swap)

To integrate `MaterialSkin` into `GameScreen.kt`:
1. In `drawCellShape`:
   Replace current manual `drawRoundRect` calls with:
   ```kotlin
   val currentMaterial = MaterialSkinCatalog.getSkin(SkinState.active).materials[colorIndex]
   drawMaterial(topLeft = topLeft, size = Size(cellSize, cellSize), spec = currentMaterial)
   ```
2. In `drawPiece`:
   Pass `currentMaterial` into `drawMaterial` for each piece cell, respecting `alpha` for drag overlays and tray slot previews.

This decoupled architecture guarantees zero changes to core game session logic, collision detection, level rules, or user saves.
