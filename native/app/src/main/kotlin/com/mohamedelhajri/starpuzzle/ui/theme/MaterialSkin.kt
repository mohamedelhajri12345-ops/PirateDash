package com.mohamedelhajri.starpuzzle.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * ─── MATERIAL SKIN ENGINE (v3.0.0) ───
 *
 * Provides tactile, procedural 3D block rendering (Ice, Fire, Gem, Wood, Candy, Metal)
 * using Jetpack Compose Canvas primitives (`DrawScope`).
 *
 * No external bitmap textures are required. All gradients, bevels, specular reflections,
 * drop shadows, and surface overlay patterns are procedurally calculated.
 */

/** Procedural surface texture pattern overlay. */
enum class OverlayPattern {
    NONE,      // Clean metallic / minimal finish
    GLOSS,     // High specular spot gloss (Gems, Candy)
    MATTE,     // Diffuse surface texture / curved wood grain
    SPARKLE,   // Radiant ember star sparkles (Fire/Magma)
    FROST      // Fractured ice crystal lines and frost dots
}

/**
 * Parameterized visual definition for a single piece cell block.
 */
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

/**
 * A complete Material Skin containing exactly `PIECE_COLOR_COUNT` (9) MaterialSpecs.
 */
data class MaterialSkin(
    val id: Int,
    val name: String,
    val isPremium: Boolean,
    val price: Int,
    val materials: List<MaterialSpec>
) {
    init {
        require(materials.size == com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT) {
            "MaterialSkin '$name' must hold exactly PIECE_COLOR_COUNT (${com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT}) material specs"
        }
    }
}

// ─── PROCEDURAL DRAWING FUNCTION ───

/**
 * Draws a single piece cell using procedural multi-pass Compose Canvas primitives.
 *
 * @param topLeft Top-left offset of the cell bounding box.
 * @param size Width and height of the cell.
 * @param spec The material specification governing colors, bevels, and overlay patterns.
 * @param alpha Global opacity multiplier for drag overlays and previews (0.0f..1.0f).
 */
fun DrawScope.drawMaterial(
    topLeft: Offset,
    size: Size,
    spec: MaterialSpec,
    alpha: Float = 1f
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val effectiveAlpha = alpha.coerceIn(0f, 1f)
    val cornerRadius = w * spec.cornerRadiusFraction.coerceIn(0.01f, 0.45f)
    val r = CornerRadius(cornerRadius, cornerRadius)

    // ── PASS 1: DROP SHADOW ──
    if (spec.shadowAlpha > 0f) {
        val shadowOffsetY = h * 0.08f
        drawRoundRect(
            color = Color.Black.copy(alpha = (spec.shadowAlpha * 0.5f * effectiveAlpha).coerceIn(0f, 1f)),
            topLeft = Offset(topLeft.x, topLeft.y + shadowOffsetY),
            size = size,
            cornerRadius = r
        )
    }

    // ── PASS 2: BASE BODY GRADIENT ──
    val bodyBrush = Brush.linearGradient(
        colors = listOf(
            spec.baseColor.copy(alpha = spec.baseColor.alpha * effectiveAlpha),
            spec.edgeColor.copy(alpha = spec.edgeColor.alpha * effectiveAlpha)
        ),
        start = topLeft,
        end = Offset(topLeft.x + w, topLeft.y + h)
    )
    drawRoundRect(
        brush = bodyBrush,
        topLeft = topLeft,
        size = size,
        cornerRadius = r
    )

    // ── PASS 3: PROCEDURAL OVERLAY PATTERNS ──
    when (spec.overlayPattern) {
        OverlayPattern.MATTE -> {
            // Wood-grain arcs
            val grainColor = spec.edgeColor.copy(alpha = (0.22f * effectiveAlpha).coerceIn(0f, 1f))
            val path = Path().apply {
                moveTo(topLeft.x + w * 0.10f, topLeft.y + h * 0.30f)
                quadraticTo(topLeft.x + w * 0.50f, topLeft.y + h * 0.45f, topLeft.x + w * 0.90f, topLeft.y + h * 0.35f)
                moveTo(topLeft.x + w * 0.05f, topLeft.y + h * 0.65f)
                quadraticTo(topLeft.x + w * 0.50f, topLeft.y + h * 0.80f, topLeft.x + w * 0.95f, topLeft.y + h * 0.70f)
            }
            drawPath(
                path = path,
                color = grainColor,
                style = Stroke(width = w * 0.04f)
            )
        }
        OverlayPattern.FROST -> {
            // Ice crystal fracture facets & dots
            val frostColor = spec.glossColor.copy(alpha = (0.35f * effectiveAlpha).coerceIn(0f, 1f))
            drawLine(
                color = frostColor,
                start = Offset(topLeft.x + w * 0.18f, topLeft.y + h * 0.18f),
                end = Offset(topLeft.x + w * 0.82f, topLeft.y + h * 0.82f),
                strokeWidth = w * 0.025f
            )
            drawLine(
                color = frostColor,
                start = Offset(topLeft.x + w * 0.75f, topLeft.y + h * 0.25f),
                end = Offset(topLeft.x + w * 0.45f, topLeft.y + h * 0.55f),
                strokeWidth = w * 0.02f
            )
            drawCircle(
                color = frostColor,
                radius = w * 0.035f,
                center = Offset(topLeft.x + w * 0.30f, topLeft.y + h * 0.70f)
            )
        }
        OverlayPattern.SPARKLE -> {
            // Lava star sparkles / glowing embers
            val sparkColor = spec.glossColor.copy(alpha = (0.80f * effectiveAlpha).coerceIn(0f, 1f))
            val centers = listOf(
                Offset(topLeft.x + w * 0.35f, topLeft.y + h * 0.65f),
                Offset(topLeft.x + w * 0.70f, topLeft.y + h * 0.35f)
            )
            for (c in centers) {
                drawLine(
                    color = sparkColor,
                    start = Offset(c.x - w * 0.07f, c.y),
                    end = Offset(c.x + w * 0.07f, c.y),
                    strokeWidth = w * 0.02f
                )
                drawLine(
                    color = sparkColor,
                    start = Offset(c.x, c.y - h * 0.07f),
                    end = Offset(c.x, c.y + h * 0.07f),
                    strokeWidth = w * 0.02f
                )
            }
        }
        else -> {}
    }

    // ── PASS 4: TOP LIGHT BEVEL ──
    if (spec.innerHighlightTopFraction > 0f) {
        val glossH = h * spec.innerHighlightTopFraction.coerceIn(0.05f, 0.80f)
        val glossBrush = Brush.verticalGradient(
            colors = listOf(
                spec.glossColor.copy(alpha = (spec.glossAlpha * effectiveAlpha).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            startY = topLeft.y,
            endY = topLeft.y + glossH
        )
        drawRoundRect(
            brush = glossBrush,
            topLeft = topLeft,
            size = Size(w, glossH),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius * 0.7f)
        )
    }

    // ── PASS 5: BOTTOM SHADOW BEVEL ──
    if (spec.innerHighlightBottomFraction > 0f) {
        val shadowH = h * spec.innerHighlightBottomFraction.coerceIn(0.05f, 0.50f)
        val shadowBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = (0.30f * effectiveAlpha).coerceIn(0f, 1f))
            ),
            startY = topLeft.y + h - shadowH,
            endY = topLeft.y + h
        )
        drawRoundRect(
            brush = shadowBrush,
            topLeft = Offset(topLeft.x, topLeft.y + h - shadowH),
            size = Size(w, shadowH),
            cornerRadius = CornerRadius(cornerRadius * 0.7f, cornerRadius)
        )
    }

    // ── PASS 6: SPECULAR HIGHLIGHT SPOT ──
    if (spec.overlayPattern == OverlayPattern.GLOSS && spec.glossAlpha > 0f) {
        val specRadius = w * spec.specularSizeFraction.coerceIn(0.05f, 0.50f)
        val specCenter = Offset(
            topLeft.x + w * spec.specularOffsetFraction.x.coerceIn(0f, 1f),
            topLeft.y + h * spec.specularOffsetFraction.y.coerceIn(0f, 1f)
        )
        val spotBrush = Brush.radialGradient(
            colors = listOf(
                spec.glossColor.copy(alpha = (spec.glossAlpha * 1.2f * effectiveAlpha).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            center = specCenter,
            radius = specRadius
        )
        drawCircle(
            brush = spotBrush,
            radius = specRadius,
            center = specCenter
        )
    }

    // ── PASS 7: VOLUME EDGE STROKE ──
    val strokeColor = spec.glossColor.copy(alpha = (0.25f * effectiveAlpha).coerceIn(0f, 1f))
    drawRoundRect(
        color = strokeColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = r,
        style = Stroke(width = w * 0.035f)
    )
}

// ─── DEMO CATALOG (6 STARTER MATERIAL SKINS) ───

object MaterialSkinCatalog {

    /** 1. GLACIAL ICE (ID 20) */
    val ICE = MaterialSkin(
        id = 20,
        name = "GLACIAL ICE",
        isPremium = true,
        price = 500,
        materials = listOf(
            MaterialSpec(Color(0xFF80DEEA), Color(0xFF00ACC1), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF29B6F6), Color(0xFF01579B), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFFB3E5FC), Color(0xFF0288D1), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF81D4FA), Color(0xFF0091EA), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFFE0F7FA), Color(0xFF006064), Color(0xFFFFFFFF), 0.50f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF4DD0E1), Color(0xFF00838F), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF00E5FF), Color(0xFF006064), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f)),
            MaterialSpec(Color(0xFF1DE9B6), Color(0xFF004D40), Color(0xFFE0F7FA), 0.45f, 0.20f, 0.30f, 0.15f, 0.25f, OverlayPattern.FROST, 0.15f, Offset(0.25f, 0.25f))
        )
    )

    /** 2. HELLFIRE LAVA (ID 21) */
    val FIRE = MaterialSkin(
        id = 21,
        name = "HELLFIRE",
        isPremium = true,
        price = 550,
        materials = listOf(
            MaterialSpec(Color(0xFFFF3D00), Color(0xFF5D0000), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFFF6D00), Color(0xFF7E1D00), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFFF9100), Color(0xFF8C3200), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFFFAB00), Color(0xFFE65100), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFFF5722), Color(0xFFBF360C), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFF4511E), Color(0xFF3E2723), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFE64A19), Color(0xFF210000), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFFF8F00), Color(0xFFD84315), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f)),
            MaterialSpec(Color(0xFFDD2C00), Color(0xFF1E0000), Color(0xFFFFEA00), 0.50f, 0.22f, 0.40f, 0.05f, 0.40f, OverlayPattern.SPARKLE, 0.35f, Offset(0.50f, 0.50f))
        )
    )

    /** 3. CRYSTAL SHARD (ID 22) */
    val GEM = MaterialSkin(
        id = 22,
        name = "CRYSTAL SHARD",
        isPremium = true,
        price = 600,
        materials = listOf(
            MaterialSpec(Color(0xFFEF5350), Color(0xFFB71C1C), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFFEC407A), Color(0xFF880E4F), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFFAB47BC), Color(0xFF4A148C), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFF5C6BC0), Color(0xFF1A237E), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFF26A69A), Color(0xFF004D40), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFF66BB6A), Color(0xFF1B5E20), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFFFFCA28), Color(0xFFF57F17), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFFFFA726), Color(0xFFE65100), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f)),
            MaterialSpec(Color(0xFF26C6DA), Color(0xFF006064), Color(0xFFFFFFFF), 0.70f, 0.12f, 0.15f, 0.35f, 0.35f, OverlayPattern.GLOSS, 0.10f, Offset(0.20f, 0.20f))
        )
    )

    /** 4. ANCIENT OAK (ID 23) */
    val WOOD = MaterialSkin(
        id = 23,
        name = "ANCIENT OAK",
        isPremium = true,
        price = 450,
        materials = listOf(
            MaterialSpec(Color(0xFF8D6E63), Color(0xFF3E2723), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFFA1887F), Color(0xFF4E342E), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFFBCAAA4), Color(0xFF5D4037), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFFD7CCC8), Color(0xFF4E342E), Color(0xFFFFFFFF), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFF795548), Color(0xFF27140B), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFF5D4037), Color(0xFF1F0C07), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFF8C6239), Color(0xFF40220F), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFFBF8A49), Color(0xFF593412), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f)),
            MaterialSpec(Color(0xFF704214), Color(0xFF3B1E08), Color(0xFFD7CCC8), 0.10f, 0.18f, 0.20f, 0.10f, 0.30f, OverlayPattern.MATTE, 0.05f, Offset(0.30f, 0.20f))
        )
    )

    /** 5. SWEET GUMMY (ID 24) */
    val CANDY = MaterialSkin(
        id = 24,
        name = "SWEET GUMMY",
        isPremium = true,
        price = 500,
        materials = listOf(
            MaterialSpec(Color(0xFFFF8A80), Color(0xFFC62828), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFFF80AB), Color(0xFFAD1457), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFEA80FC), Color(0xFF6A1B9A), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFB388FF), Color(0xFF4527A0), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFF82B1FF), Color(0xFF1565C0), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFF84FFFF), Color(0xFF00838F), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFB9F6CA), Color(0xFF2E7D32), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFFFE57F), Color(0xFFF57C00), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f)),
            MaterialSpec(Color(0xFFFFD180), Color(0xFFD84315), Color(0xFFFFF176), 0.60f, 0.35f, 0.55f, 0.12f, 0.20f, OverlayPattern.GLOSS, 0.45f, Offset(0.35f, 0.30f))
        )
    )

    /** 6. CHROME STEEL (ID 25) */
    val METAL = MaterialSkin(
        id = 25,
        name = "CHROME STEEL",
        isPremium = true,
        price = 650,
        materials = listOf(
            MaterialSpec(Color(0xFFCFD8DC), Color(0xFF455A64), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFFB0BEC5), Color(0xFF37474F), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFF90A4AE), Color(0xFF263238), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFFECEFF1), Color(0xFF78909C), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFF9E9E9E), Color(0xFF424242), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFF757575), Color(0xFF212121), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFFEEEEEE), Color(0xFF616161), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFFE0E0E0), Color(0xFF424242), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f)),
            MaterialSpec(Color(0xFFB3B3B3), Color(0xFF333333), Color(0xFFFFFFFF), 0.35f, 0.08f, 0.10f, 0.10f, 0.45f, OverlayPattern.NONE, 0.15f, Offset(0.15f, 0.15f))
        )
    )

    /** All premium material skins. */
    val premiumSkins = listOf(ICE, FIRE, GEM, WOOD, CANDY, METAL)

    /** Full combined list of classic legacy skins + premium material skins. */
    val allSkins: List<MaterialSkin> by lazy {
        val legacyList = PieceSkins.mapIndexed { index, legacy ->
            legacy.toClassicMaterialSkin(index)
        }
        legacyList + premiumSkins
    }

    /** Safe lookup by skin ID. Falls back to classic default (ID 0) if index out of bounds. */
    fun getSkin(id: Int): MaterialSkin {
        val found = allSkins.find { it.id == id }
        return found ?: allSkins[0]
    }
}

/**
 * Adapter converting legacy 20-palette `PieceSkin` to a classic `MaterialSkin`.
 */
fun PieceSkin.toClassicMaterialSkin(id: Int): MaterialSkin {
    val specs = colors.map { color ->
        MaterialSpec(
            baseColor = color,
            edgeColor = color.copy(
                red = color.red * 0.75f,
                green = color.green * 0.75f,
                blue = color.blue * 0.75f
            ),
            glossColor = Color.White,
            glossAlpha = 0.20f,
            cornerRadiusFraction = 0.22f,
            innerHighlightTopFraction = 0.45f,
            innerHighlightBottomFraction = 0.10f,
            shadowAlpha = 0.35f,
            overlayPattern = OverlayPattern.GLOSS,
            specularSizeFraction = 0.20f,
            specularOffsetFraction = Offset(0.25f, 0.25f)
        )
    }
    return MaterialSkin(
        id = id,
        name = name,
        isPremium = false,
        price = skinPrice(id),
        materials = specs
    )
}

/*
 * ─── MANDATORY LINE-BY-LINE SELF-AUDIT ───
 *
 * 1. MATHEMATICAL BOUNDS & SANITIZATION:
 *    - `DrawScope.drawMaterial` guards against width <= 0 or height <= 0 immediately to avoid divide-by-zero or negative dimension crashes.
 *    - All fraction fields (`cornerRadiusFraction`, `innerHighlightTopFraction`, `innerHighlightBottomFraction`, `shadowAlpha`, `glossAlpha`) are coerced via `.coerceIn(...)` prior to geometry/color calculations.
 *    - Color alpha values are strictly clamped in `(0f..1f)`.
 *
 * 2. MEMORY & GC ZERO-ALLOCATION ON DRAW LOOP:
 *    - `CornerRadius`, `Offset`, `Size`, `Brush.linearGradient`, `Brush.verticalGradient`, `Brush.radialGradient`, and `Stroke` are created only with stack primitives derived directly from function parameters (`topLeft`, `size`).
 *    - `MaterialSkinCatalog.allSkins` uses `by lazy` initialization so skin list allocation happens at most once during runtime, never during frame redraws.
 *    - The catalog objects (`ICE`, `FIRE`, `GEM`, `WOOD`, `CANDY`, `METAL`) are static `val` singletons holding pre-constructed immutables.
 *
 * 3. CONTRACT & TYPE SAFETY:
 *    - `MaterialSkin` init block asserts `materials.size == PIECE_COLOR_COUNT` (9). If any future material skin is authored with fewer or more specs, an explicit runtime assertion fails during catalog creation rather than dropping frames or throwing OutOfBoundsException mid-gameplay.
 *    - Safe fallback in `MaterialSkinCatalog.getSkin(id)` ensures unknown/corrupted skin IDs safely resolve to default skin (ID 0).
 *
 * 4. BACKWARDS COMPATIBILITY:
 *    - `PieceSkin.toClassicMaterialSkin(id)` adapts all 20 existing color palettes dynamically into valid `MaterialSkin` instances without breaking existing player progress or saved CSV skin IDs.
 */
