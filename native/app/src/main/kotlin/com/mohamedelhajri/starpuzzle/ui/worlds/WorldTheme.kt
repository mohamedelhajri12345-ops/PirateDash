package com.mohamedelhajri.starpuzzle.ui.worlds

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.sin

// ─── Phase B: every world's visual identity ───
//
// One gradient + one quiet animated element layer per world. Everything is
// vector-drawn on a single canvas — no bitmaps, no per-frame allocations,
// and always subtle enough that the board stays perfectly readable.
// Halal standard: nature, light and craftsmanship only, no occult symbols.

data class WorldTheme(
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color
)

private val WorldThemes = listOf(
    WorldTheme(Color(0xFF173B2A), Color(0xFF2E7D4F), Color(0xFF8BC34A)), // 1 Green Valley
    WorldTheme(Color(0xFF7A4A10), Color(0xFFE0952F), Color(0xFFFFD54F)), // 2 Golden Desert
    WorldTheme(Color(0xFF0A2E4D), Color(0xFF10688C), Color(0xFF4DD0E1)), // 3 Coral Ocean
    WorldTheme(Color(0xFF274B69), Color(0xFF9CC3DE), Color(0xFFB3E5FC)), // 4 Frozen Peaks
    WorldTheme(Color(0xFF1C0A08), Color(0xFF5C1B0E), Color(0xFFFF7043)), // 5 Ember Volcano
    WorldTheme(Color(0xFF0D2B12), Color(0xFF2F5D2A), Color(0xFF9CCC65)), // 6 Dino Jungle
    WorldTheme(Color(0xFF150726), Color(0xFF3A0D45), Color(0xFFE040FB)), // 7 Neon City
    WorldTheme(Color(0xFF5A2A6E), Color(0xFFC36FA0), Color(0xFFF48FB1)), // 8 Candy Wonderland
    WorldTheme(Color(0xFF070B24), Color(0xFF241B52), Color(0xFF7C4DFF)), // 9 Cosmic Space
    WorldTheme(Color(0xFF04202F), Color(0xFF0A4A5C), Color(0xFF26C6DA))  // 10 Sunken Treasure
)

fun worldTheme(world: Int): WorldTheme =
    WorldThemes[(world - 1).coerceIn(0, WorldThemes.size - 1)]

/** Deterministic per-index pseudo random in 0..1 (stable across frames). */
private fun DrawScope.rnd(k: Int, salt: Int): Float =
    (((k * 73856093) xor (salt * 19349663)) and 0xFFFF) / 65535f

/** Full-screen animated backdrop that carries [world]'s identity. */
@Composable
fun WorldBackdrop(world: Int, modifier: Modifier = Modifier) {
    val theme = remember(world) { worldTheme(world) }
    val t by rememberInfiniteTransition(label = "worldClock").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(16000, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "worldT"
    )
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawWorldElements(world, t)
        }
    }
}

private fun DrawScope.drawWorldElements(world: Int, t: Float) {
    val w = size.width
    val h = size.height
    when (world) {
        1 -> { // Green Valley: rolling hills + drifting leaves
            drawCircle(Color(0xFF123524).copy(alpha = 0.55f), w * 0.65f, Offset(w * 0.15f, h * 1.45f))
            drawCircle(Color(0xFF1B4D2E).copy(alpha = 0.55f), w * 0.75f, Offset(w * 0.95f, h * 1.5f))
            for (k in 0 until 12) {
                val rx = (rnd(k, 1) + t * (0.15f + rnd(k, 3) * 0.25f)) % 1f
                val ry = (rnd(k, 2) + t * 0.5f) % 1f
                drawCircle(
                    Color(0xFF9CCC65).copy(alpha = 0.35f), w * 0.012f,
                    Offset(rx * w, ry * h)
                )
            }
        }

        2 -> { // Golden Desert: sun, dunes, drifting sand
            drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.85f), w * 0.13f, Offset(w * 0.8f, h * 0.16f))
            drawCircle(Color(0xFFFFD54F).copy(alpha = 0.25f), w * 0.30f, Offset(w * 0.8f, h * 0.16f))
            val dune = Path().apply {
                moveTo(0f, h)
                cubicTo(w * 0.25f, h * 0.82f, w * 0.55f, h * 1.02f, w, h * 0.88f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(dune, Color(0xFF9C5A1B).copy(alpha = 0.5f))
            val dune2 = Path().apply {
                moveTo(0f, h * 0.95f)
                cubicTo(w * 0.35f, h * 0.78f, w * 0.7f, h * 0.98f, w, h * 0.8f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(dune2, Color(0xFF7A4A10).copy(alpha = 0.6f))
            for (k in 0 until 10) {
                val sx = (rnd(k, 4) + t * (0.2f + rnd(k, 5) * 0.3f)) % 1f
                drawCircle(
                    Color(0xFFFFE082).copy(alpha = 0.3f), w * 0.008f,
                    Offset(sx * w, h * (0.55f + rnd(k, 6) * 0.4f))
                )
            }
        }

        3 -> { // Coral Ocean: rising bubbles
            for (k in 0 until 14) {
                val by = 1f - ((rnd(k, 1) + t * (0.3f + rnd(k, 2) * 0.4f)) % 1f)
                val bx = (rnd(k, 3) + sin(t * 6.2832f + k) * 0.01f)
                drawCircle(
                    Color(0xFFB2EBF2).copy(alpha = 0.28f),
                    w * (0.008f + rnd(k, 4) * 0.012f),
                    Offset((bx % 1f + 1f) % 1f * w, by * h)
                )
            }
        }

        4 -> { // Frozen Peaks: snowfall + aurora glow + peaks
            drawCircle(Color(0xFF80DEEA).copy(alpha = 0.10f), w * 0.9f, Offset(w * 0.5f, -h * 0.4f))
            val m1 = Path().apply {
                moveTo(0f, h); lineTo(w * 0.18f, h * 0.72f); lineTo(w * 0.36f, h); close()
            }
            drawPath(m1, Color(0xFF37474F).copy(alpha = 0.45f))
            val m2 = Path().apply {
                moveTo(w * 0.5f, h); lineTo(w * 0.72f, h * 0.66f); lineTo(w * 0.95f, h); close()
            }
            drawPath(m2, Color(0xFF455A64).copy(alpha = 0.45f))
            for (k in 0 until 24) {
                val sy = (rnd(k, 1) + t * (0.25f + rnd(k, 2) * 0.35f)) % 1f
                val sx = (rnd(k, 3) + sin(t * 12.566f + k) * 0.008f) % 1f
                drawCircle(Color.White.copy(alpha = 0.6f), w * 0.006f, Offset(sx * w, sy * h))
            }
        }

        5 -> { // Ember Volcano: ridge + glow + rising embers
            drawCircle(Color(0xFFFF5722).copy(alpha = 0.22f), w * 0.7f, Offset(w * 0.5f, h * 1.25f))
            val ridge = Path().apply {
                moveTo(0f, h); lineTo(w * 0.2f, h * 0.78f)
                lineTo(w * 0.45f, h * 0.92f); lineTo(w * 0.62f, h * 0.70f)
                lineTo(w * 0.85f, h * 0.90f); lineTo(w, h * 0.80f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(ridge, Color(0xFF3E1111).copy(alpha = 0.7f))
            for (k in 0 until 12) {
                val ey = 1f - ((rnd(k, 1) + t * (0.25f + rnd(k, 2) * 0.35f)) % 1f)
                val ex = (rnd(k, 3) + sin(t * 9f + k * 2f) * 0.012f) % 1f
                drawCircle(
                    Color(0xFFFFAB40).copy(alpha = 0.5f * (0.4f + 0.6f * ey)),
                    w * 0.006f, Offset(ex * w, ey * h)
                )
            }
        }

        6 -> { // Dino Jungle: dino silhouettes + fireflies
            drawDino(Offset(w * 0.06f, h * 0.88f), w * 0.34f, Color(0xFF0A1F0C).copy(alpha = 0.75f))
            drawDino(Offset(w * 0.60f, h * 0.94f), w * 0.22f, Color(0xFF0F2E12).copy(alpha = 0.55f))
            for (k in 0 until 10) {
                val fx = (rnd(k, 1) + t * 0.05f) % 1f
                val fy = ((rnd(k, 2) + sin(t * 6.2832f + k) * 0.03f) % 1f + 1f) % 1f
                val a = 0.35f + 0.55f * abs(sin(t * 6.2832f * (1f + rnd(k, 3)) + k))
                drawCircle(Color(0xFFFFFF8D).copy(alpha = a), w * 0.005f, Offset(fx * w, fy * h))
            }
        }

        7 -> { // Neon City: skyline + blinking windows + signs
            val base = h * 0.999f
            for (k in 0 until 8) {
                val bw = w * (0.06f + rnd(k, 1) * 0.07f)
                val bh = h * (0.18f + rnd(k, 2) * 0.30f)
                val bx = w * k / 8f + w * 0.012f
                drawRect(Color(0xFF0A0413).copy(alpha = 0.8f), Offset(bx, base - bh), Size(bw, bh))
                for (r in 0 until 4) for (c in 0 until 3) {
                    if (sin(t * 12.566f + k * 3f + r + c) > 0f) {
                        drawRect(
                            Color(0xFF80DEEA).copy(alpha = 0.5f),
                            Offset(
                                bx + bw * (0.18f + c * 0.28f),
                                base - bh + bh * (0.12f + r * 0.22f)
                            ),
                            Size(bw * 0.12f, bh * 0.06f)
                        )
                    }
                }
            }
            drawRect(Color(0xFFE040FB).copy(alpha = 0.5f), Offset(w * 0.12f, h * 0.16f), Size(w * 0.07f, h * 0.015f))
            drawRect(Color(0xFF40C4FF).copy(alpha = 0.5f), Offset(w * 0.78f, h * 0.10f), Size(w * 0.09f, h * 0.012f))
        }

        8 -> { // Candy Wonderland: soft candy dots
            drawCircle(Color.White.copy(alpha = 0.05f), w * 0.50f, Offset(w * 0.20f, h * 0.20f))
            drawCircle(Color.White.copy(alpha = 0.05f), w * 0.35f, Offset(w * 0.85f, h * 0.80f))
            for (k in 0 until 12) {
                val cx = (rnd(k, 1) + t * 0.03f) % 1f
                val cy = (rnd(k, 2) + t * 0.05f) % 1f
                val r = w * (0.008f + rnd(k, 3) * 0.014f)
                val col = when (k % 3) {
                    0 -> Color(0xFFF8BBD0).copy(alpha = 0.40f)
                    1 -> Color(0xFFE1BEE7).copy(alpha = 0.40f)
                    else -> Color(0xFFFFF176).copy(alpha = 0.35f)
                }
                drawCircle(col, r, Offset(cx * w, cy * h))
            }
        }

        9 -> { // Cosmic Space: twinkling stars, planets, one comet
            for (k in 0 until 40) {
                val a = 0.25f + 0.65f * abs(sin(t * 6.2832f * (0.5f + rnd(k, 3)) + k))
                drawCircle(
                    Color.White.copy(alpha = a * 0.8f),
                    w * 0.004f * (1f + rnd(k, 1)),
                    Offset(rnd(k, 2) * w, rnd(k, 4) * h)
                )
            }
            drawCircle(Color(0xFF7986CB).copy(alpha = 0.75f), w * 0.06f, Offset(w * 0.15f, h * 0.22f))
            drawCircle(Color(0xFF4527A0).copy(alpha = 0.80f), w * 0.09f, Offset(w * 0.85f, h * 0.30f))
            drawOval(
                Color(0xFFFFD54F).copy(alpha = 0.50f),
                Offset(w * 0.72f, h * 0.27f),
                Size(w * 0.26f, w * 0.018f)
            )
            if (t < 0.35f) {
                val ct = t / 0.35f
                val cx = w * ct * 1.1f
                val cy = h * (0.05f + ct * 0.45f)
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.006f, Offset(cx, cy))
                drawLine(
                    Color.White.copy(alpha = 0.25f),
                    Offset(cx, cy),
                    Offset(cx - w * 0.06f, cy - h * 0.03f),
                    strokeWidth = w * 0.004f
                )
            }
        }

        else -> { // Sunken Treasure: light rays + bubbles + gold glints
            for (k in 0 until 3) {
                val rayX = w * (0.2f + k * 0.3f)
                val sway = sin(t * 6.2832f + k * 2f) * w * 0.03f
                val ray = Path().apply {
                    moveTo(rayX - w * 0.05f, 0f)
                    lineTo(rayX + w * 0.05f, 0f)
                    lineTo(rayX + w * 0.14f + sway, h)
                    lineTo(rayX - w * 0.14f + sway, h)
                    close()
                }
                drawPath(ray, Color(0xFF4DD0E1).copy(alpha = 0.05f))
            }
            for (k in 0 until 10) {
                val by = 1f - ((rnd(k, 1) + t * (0.2f + rnd(k, 2) * 0.3f)) % 1f)
                drawCircle(
                    Color(0xFFB2EBF2).copy(alpha = 0.25f), w * 0.010f,
                    Offset(rnd(k, 3) * w, by * h)
                )
            }
            for (k in 0 until 5) {
                val a = 0.30f + 0.50f * abs(sin(t * 6.2832f + k * 1.3f))
                drawCircle(
                    Color(0xFFFFD54F).copy(alpha = a), w * 0.008f,
                    Offset(rnd(k, 5) * w, h * (0.88f + rnd(k, 6) * 0.08f))
                )
            }
        }
    }
}

/** A gentle brontosaurus silhouette built from simple volumes. */
private fun DrawScope.drawDino(at: Offset, s: Float, color: Color) {
    // tail
    val tail = Path().apply {
        moveTo(at.x + s * 0.1f, at.y - s * 0.55f)
        lineTo(at.x - s * 0.35f, at.y - s * 0.72f)
        lineTo(at.x + s * 0.1f, at.y - s * 0.35f)
        close()
    }
    drawPath(tail, color)
    // body
    drawRoundRect(
        color,
        Offset(at.x, at.y - s * 0.80f),
        Size(s * 1.0f, s * 0.5f),
        CornerRadius(s * 0.25f)
    )
    // legs
    drawRoundRect(
        color,
        Offset(at.x + s * 0.12f, at.y - s * 0.35f),
        Size(s * 0.14f, s * 0.38f),
        CornerRadius(s * 0.06f)
    )
    drawRoundRect(
        color,
        Offset(at.x + s * 0.62f, at.y - s * 0.35f),
        Size(s * 0.14f, s * 0.38f),
        CornerRadius(s * 0.06f)
    )
    // neck
    val neck = Path().apply {
        moveTo(at.x + s * 0.80f, at.y - s * 0.62f)
        cubicTo(
            at.x + s * 1.05f, at.y - s * 0.90f,
            at.x + s * 1.00f, at.y - s * 1.30f,
            at.x + s * 0.85f, at.y - s * 1.45f
        )
        lineTo(at.x + s * 0.95f, at.y - s * 1.45f)
        cubicTo(
            at.x + s * 1.15f, at.y - s * 1.20f,
            at.x + s * 1.10f, at.y - s * 0.80f,
            at.x + s * 0.85f, at.y - s * 0.45f
        )
        close()
    }
    drawPath(neck, color)
    // head
    drawRoundRect(
        color,
        Offset(at.x + s * 0.72f, at.y - s * 1.55f),
        Size(s * 0.30f, s * 0.16f),
        CornerRadius(s * 0.07f)
    )
}
