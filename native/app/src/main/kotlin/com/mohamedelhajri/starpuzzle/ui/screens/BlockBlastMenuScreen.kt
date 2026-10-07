package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.StarThemes
import kotlin.math.sin

/**
 * MAIN MENU — Star Puzzle (by RENDER). Original identity (master
 * prompt §27–§28): star logo + wordmark, big glowing PLAY, best score,
 * cards for Daily Challenge / Themes / Missions / Shop, settings gear,
 * floating-star background, visible currencies (owner requirement).
 * Palette follows the active theme.
 */
@Composable
fun BlockBlastMenuScreen(
    level: Int,
    best: Int,
    coins: Int,
    gems: Int,
    dailyBonusAvailable: Boolean,
    soundManager: SoundManager,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onMissions: () -> Unit,
    onSettings: () -> Unit,
    onClaimDailyBonus: () -> Unit,
    onThemes: () -> Unit = {},
    dailyChallengeDone: Boolean = false,
    onDailyChallenge: () -> Unit = {}
) {
    val theme = StarThemes.active
    val accent = theme.accent

    // slow drifting background stars (subtle movement, §27)
    val drift = rememberInfiniteTransition(label = "stars")
    val starPhase by drift.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000), RepeatMode.Restart),
        label = "phase"
    )
    // idle pulse for the PLAY glow (§28)
    val pulse by drift.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )
    val playInteraction = remember { MutableInteractionSource() }
    val pressed by playInteraction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMenuStars(starPhase)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // ---- top bar: LEVEL + currencies (always visible, owner rule) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\u2B50  LEVEL $level",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MenuChip("\uD83E\uDE99 $coins", gold = true)
                    Spacer(Modifier.width(6.dp))
                    MenuChip("\uD83D\uDC8E $gems", gold = false, accent = accent)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // ---- STAR PUZZLE logo: five-point star + wordmark ----
                Canvas(modifier = Modifier.size(64.dp)) {
                    val r = size.minDimension / 2f
                    val c = Offset(size.width / 2f, size.height / 2f + r * 0.06f)
                    val path = Path()
                    for (i in 0 until 10) {
                        val rad = if (i % 2 == 0) r else r * 0.45f
                        val a = -Math.PI / 2f + i * Math.PI / 5f
                        val x = c.x + rad * kotlin.math.cos(a).toFloat()
                        val y = c.y + rad * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, Color(0xFFFFD34E), style = Fill)
                }
                Text(
                    "STAR PUZZLE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    "by RENDER",
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(22.dp))

                // ---- big glowing PLAY (§28) ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .scale(if (pressed) 0.95f else pulse)
                        .shadow(10.dp, RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent, Color(0xFF4C7DF0), Color(0xFF8B5CF6))
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .clickable(interactionSource = playInteraction, indication = null) {
                            soundManager.play(SoundManager.Sfx.START)
                            onPlay()
                        }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "\u25B6  PLAY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "BEST $best",
                    color = BbGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(22.dp))

                // ---- feature cards ----
                MenuCard("\uD83C\uDFAE", if (dailyChallengeDone) "Daily Challenge \u2713" else "Daily Challenge", accent) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onDailyChallenge()
                }
                Spacer(Modifier.height(10.dp))
                MenuCard("\uD83C\uDFA8", "Themes", accent) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onThemes()
                }
                Spacer(Modifier.height(10.dp))
                MenuCard("\uD83D\uDEC9", "Missions", accent) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onMissions()
                }
                Spacer(Modifier.height(10.dp))
                MenuCard("\uD83D\uDED2", "Shop", accent) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onShop()
                }
            }

            // ---- daily bonus + settings ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFD34E), Color(0xFFFFA726))),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = dailyBonusAvailable) {
                            soundManager.play(SoundManager.Sfx.COMPLETE)
                            onClaimDailyBonus()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (dailyBonusAvailable) "\uD83C\uDF81 Daily Bonus \u2014 tap to claim"
                        else "\uD83C\uDF81 Bonus claimed \u2014 come back tomorrow",
                        color = Color(0xFF5A3A00),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            soundManager.play(SoundManager.Sfx.CONFIRM)
                            onSettings()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2699", color = Color.White, fontSize = 22.sp)
                }
            }
            Text(
                "STAR PUZZLE \u2022 by RENDER \u2022 v8.5.0 \u2022 offline",
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuChip(label: String, gold: Boolean, accent: Color = Color.White) {
    Text(
        label,
        color = if (gold) Color(0xFFFFD34E) else accent,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun MenuCard(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .shadow(5.dp, RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Text(
                "  $label",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
        Text("\u203A", color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}

/** Soft drifting stars in the theme background (calm, non-distracting). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMenuStars(phase: Float) {
    val stars = listOf(
        0.10f to 0.18f, 0.85f to 0.10f, 0.70f to 0.32f, 0.20f to 0.44f,
        0.92f to 0.55f, 0.35f to 0.62f, 0.55f to 0.16f, 0.05f to 0.80f
    )
    stars.forEachIndexed { i, (fx, fy) ->
        val driftX = sin((phase * 2f * Math.PI).toFloat() + i) * 12f
        val twinkle = 0.25f + 0.20f * (0.5f + 0.5f * sin((phase * 4f * Math.PI).toFloat() + i * 1.7f))
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = size.minDimension * 0.006f * (1f + (i % 3) * 0.4f),
            center = Offset(size.width * fx + driftX, size.height * fy)
        )
    }
}

/**
 * Game Over (§20): STAR PUZZLE branding, score, best, NEW BEST badge,
 * RETRY + HOME. Feels like "one more attempt", not an error.
 */
@Composable
fun BlockBlastGameOverScreen(
    score: Int,
    best: Int,
    coins: Int,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
    newBest: Boolean = false
) {
    val theme = StarThemes.active
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("STAR PUZZLE", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "GAME OVER",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp
            )
            if (newBest) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "\u2B50 NEW BEST \u2B50",
                    color = BbGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                score.toString(),
                color = BbGold,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp
            )
            Text("SCORE", color = Color(0xFF9AA5CE), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "BEST $best",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            BbCoinChip(coins, big = true)

            Spacer(Modifier.height(26.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .background(
                        Brush.horizontalGradient(listOf(theme.accent, Color(0xFF8B5CF6))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onPlayAgain() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RETRY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .clickable { onMenu() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("HOME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
