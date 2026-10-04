package com.mohamedelhajri.starpuzzle.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.mohamedelhajri.starpuzzle.audio.SoundManager

// ── The approved visual language: no flat progress bars ──────────────
// Objective progress is shown as three glowing star gems that light up
// one by one (game-screen concept the owner approved).

private val GemGold = Color(0xFFFFD54F)
private val GemGoldSoft = Color(0xFFFFE082)
private val GemDim = Color(0xFF3A4468)

/** Draws a five-point star path inside (0,0,size,size). */
private fun starPath(size: Float): Path {
    val cx = size / 2f
    val cy = size / 2f
    val outer = size * 0.48f
    val inner = outer * 0.42f
    val p = Path()
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) outer else inner
        val a = -Math.PI / 2 + k * Math.PI / 10
        val x = cx + (r * kotlin.math.cos(a)).toFloat()
        val y = cy + (r * kotlin.math.sin(a)).toFloat()
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

@Composable
private fun StarGem(lit: Float, size: Int) {
    val dpSize = size.dp
    Box(modifier = Modifier.size(dpSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.minDimension()
            if (lit > 0f) {
                // soft halo behind the lit gem
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(GemGoldSoft.copy(alpha = 0.45f * lit), Color.Transparent),
                        center = Offset(s / 2f, s / 2f),
                        radius = s * 0.65f
                    ),
                    radius = s * 0.65f,
                    center = Offset(s / 2f, s / 2f)
                )
            }
            val path = starPath(s)
            if (lit > 0f) {
                drawPath(
                    path,
                    Brush.verticalGradient(
                        listOf(
                            GemGoldSoft.copy(alpha = 0.35f + 0.65f * lit),
                            Color(0xFFE6A817).copy(alpha = 0.35f + 0.65f * lit)
                        )
                    )
                )
            } else {
                drawPath(path, GemDim, style = Stroke(width = s * 0.05f))
            }
        }
    }
}

/**
 * Three star gems showing [progress] (0..1): gem 1 lights at 0.34, gem 2
 * at 0.67, gem 3 at 1.0 — the third gem glows progressively so the
 * player always feels the distance to the goal.
 */
@Composable
fun StarMilestoneStrip(progress: Float, modifier: Modifier = Modifier) {
    val frac = progress.coerceIn(0f, 1f)
    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement
            .spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        StarGem(lit = (frac / 0.34f).coerceIn(0f, 1f), 30)
        StarGem(lit = ((frac - 0.34f) / 0.33f).coerceIn(0f, 1f), 30)
        StarGem(lit = ((frac - 0.67f) / 0.33f).coerceIn(0f, 1f), 30)
        // the connecting light thread of the constellation
    }
}

// ── Booster chips (store inventory buttons on the game HUD) ───────────

@Composable
fun BoosterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    sound: SoundManager
) {
    val border = if (selected) GemGold
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable {
                if (enabled) onClick() else sound.play(SoundManager.Sfx.INVALID)
            }
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp, 34.dp)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .background(
                    if (selected) Color(0x2EFFD54F)
                    else Color(0x14101830),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val s = size.minDimension()
                when (label) {
                    "BOMB" -> {
                        drawCircle(Color(0xFF8D6E63), radius = s * 0.4f,
                            center = Offset(s / 2f, s * 0.6f))
                        drawCircle(GemGold, radius = s * 0.12f,
                            center = Offset(s * 0.62f, s * 0.28f))
                    }
                    "ZAP" -> {
                        val p = Path().apply {
                            moveTo(s * 0.7f, 0f); lineTo(s * 0.2f, s * 0.55f)
                            lineTo(s * 0.5f, s * 0.55f); lineTo(s * 0.3f, s)
                            lineTo(s * 0.8f, s * 0.45f); lineTo(s * 0.5f, s * 0.45f)
                            close()
                        }
                        drawPath(p, Color(0xFF40C4FF))
                    }
                    "STAR" -> drawPath(starPath(s), GemGold)
                    else -> { // MOVE: circular arrow
                        val arc = Path().apply {
                            arcTo(
                                androidx.compose.ui.geometry.Rect(
                                    Offset(s * 0.15f, s * 0.15f), Size(s * 0.7f, s * 0.7f)
                                ),
                                0f, 300f, false
                            )
                        }
                        drawPath(arc, GemGold,
                            style = Stroke(width = s * 0.12f, cap = StrokeCap.Round))
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        androidx.compose.material3.Text(
            "$label ×$count",
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFFF5F7FF).copy(alpha = 0.85f)
                else Color(0xFF9FA8CC).copy(alpha = 0.4f)
            )
        )
    }
}

// ── Confetti burst behind the win card ───────────────────────────────

@Composable
fun ConfettiBurst() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(3200, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
        label = "confetti-t"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // deterministic pseudo-random per index
        fun prand(seed: Int) =
            ((seed * 2654435761) % 10007) / 10007f
        for (i in 0 until 26) {
            val x0 = prand(i + 1) * w
            val speed = 0.35f + prand(i + 40) * 0.5f
            val phase = ((t * speed + prand(i + 80)) % 1f)
            val y = h * (1.15f - phase * 1.3f)
            val sway = kotlin.math.sin((phase * 4f + i) * 1.3f) * w * 0.03f
            val alpha = (1f - phase).coerceIn(0f, 1f) * 0.8f
            val r = 3.dp.toPx() * (0.5f + prand(i + 120))
            val c = if (i % 3 == 0) GemGold
            else if (i % 3 == 1) GemGoldSoft
            else Color(0xFFAB47BC)
            drawCircle(c.copy(alpha = alpha), radius = r,
                center = Offset(x0 + sway, y))
        }
    }
}
