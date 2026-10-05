package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// ── The unified "Cosmos" identity ─────────────────────────────────────
// One premium animated sky shared by every screen (menu, world map, game)
// so the whole app reads as a single product, like the reference video.
// Fully procedural: no bitmaps, no large blur passes, stable 60fps.

private val CandyPink = Color(0xFFFF6EC7)
private val CandyViolet = Color(0xFF8B5CF6)
private val CandyCyan = Color(0xFF22D3EE)
private val MenuGoldSoft = Color(0xFFFFE082)
private val White = Color(0xFFFFFFFF)

@Composable
fun CosmosBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cosmos")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmosT"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(
                    CandyPink.copy(alpha = 0.95f),
                    CandyViolet,
                    CandyViolet,
                    CandyCyan.copy(alpha = 0.85f)
                ),
                startY = 0f, endY = h
            )
        )
        fun prand(seed: Int) = ((seed * 2654435761) % 10007) / 10007f

        // ── soft nebula clouds: big, layered colour glow (depth) ──
        val nebulaColors = listOf(
            Color(0xFFFF9FE0), Color(0xFFB79BFF), Color(0xFF8FE9FF), Color(0xFFFFE38F)
        )
        for (i in 0 until 5) {
            val cx = prand(i + 11) * w
            val cy = prand(i + 40) * h * 0.75f
            val r = (0.32f + prand(i + 65) * 0.26f) * w
            val drift = kotlin.math.sin((t + prand(i)) * 6.283f) * r * 0.10f
            val col = nebulaColors[i % nebulaColors.size]
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(col.copy(alpha = 0.16f), col.copy(alpha = 0f)),
                    center = Offset(cx + drift, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx + drift, cy)
            )
        }

        // ── distant star layer: tiny, dim, dense (parallax depth) ──
        for (i in 0 until 70) {
            val x = prand(i + 500) * w
            val y = prand(i + 540) * h
            val base = 0.15f + prand(i + 571) * 0.35f
            val alpha = base * (0.6f + 0.4f * kotlin.math.sin((t * 0.6f + prand(i)) * 6.283f))
            val r = (0.6f + prand(i + 590) * 0.8f).dp.toPx()
            drawCircle(White.copy(alpha = alpha.coerceIn(0.03f, 0.5f)), radius = r, center = Offset(x, y))
        }

        // ── mid star layer: glowing twinkling stars with soft halo ──
        for (i in 0 until 34) {
            val x = prand(i + 3) * w
            val y = prand(i + 37) * h
            val base = 0.35f + prand(i + 71) * 0.55f
            val twinkle = 0.55f + 0.45f * kotlin.math.sin((t * 1.4f + prand(i) * 3f) * 6.283f)
            val alpha = (base * twinkle).coerceIn(0.08f, 1f)
            val coreR = (1.1f + prand(i + 90) * 1.5f).dp.toPx()
            val warm = prand(i + 920) > 0.5f
            val tint = if (warm) Color(0xFFFFF3D6) else Color(0xFFE4F3FF)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = alpha * 0.55f), tint.copy(alpha = 0f)),
                    center = Offset(x, y),
                    radius = coreR * 5.5f
                ),
                radius = coreR * 5.5f,
                center = Offset(x, y)
            )
            drawCircle(tint.copy(alpha = alpha), radius = coreR, center = Offset(x, y))
        }

        // ── hero stars: bright cross-flare accents ──
        for (i in 0 until 6) {
            val x = prand(i + 1130) * w
            val y = prand(i + 1160) * h * 0.85f
            val pulse = 0.6f + 0.4f * kotlin.math.abs(kotlin.math.sin((t * 1.8f + prand(i) * 4f) * 3.1416f))
            val s = (7f + prand(i + 1190) * 7f).dp.toPx() * pulse
            val alpha = (0.55f + 0.45f * pulse).coerceIn(0.2f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MenuGoldSoft.copy(alpha = alpha * 0.35f), MenuGoldSoft.copy(alpha = 0f)),
                    center = Offset(x, y),
                    radius = s * 3.2f
                ),
                radius = s * 3.2f,
                center = Offset(x, y)
            )
            val p = Path().apply {
                moveTo(x, y - s)
                lineTo(x + s * 0.18f, y - s * 0.18f)
                lineTo(x + s, y)
                lineTo(x + s * 0.18f, y + s * 0.18f)
                lineTo(x, y + s)
                lineTo(x - s * 0.18f, y + s * 0.18f)
                lineTo(x - s, y)
                lineTo(x - s * 0.18f, y - s * 0.18f)
                close()
            }
            drawPath(p, White.copy(alpha = alpha))
            drawCircle(White.copy(alpha = alpha), radius = s * 0.16f, center = Offset(x, y))
        }

        // ── a single slow shooting star streak ──
        val shootPhase = (t * 1f) % 1f
        if (shootPhase < 0.22f) {
            val progress = shootPhase / 0.22f
            val sx0 = w * 0.08f; val sy0 = h * 0.10f
            val sx1 = w * 0.62f; val sy1 = h * 0.42f
            val hx = sx0 + (sx1 - sx0) * progress
            val hy = sy0 + (sy1 - sy0) * progress
            val fade = kotlin.math.sin(progress * 3.1416f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(White.copy(alpha = 0f), White.copy(alpha = (0.85f * fade).coerceIn(0f, 1f))),
                    start = Offset(hx - w * 0.09f, hy - h * 0.05f),
                    end = Offset(hx, hy)
                ),
                start = Offset(hx - w * 0.09f, hy - h * 0.05f),
                end = Offset(hx, hy),
                strokeWidth = 2.2f.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
