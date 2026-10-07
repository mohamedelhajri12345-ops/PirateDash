package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Main Menu — matches the owner's reference screenshot 1:1 in layout
 * and features (top status bar with level/coins/gems, puzzle-piece
 * logo, title + subtitle, 4 full-width action buttons, daily-bonus
 * banner, version footer). Brand & colors are Star Puzzle's own.
 */
val BbMenuBgTop = Color(0xFFBFEAF0)
val BbMenuBgBot = Color(0xFFD9C7F0)
val BbMenuNavy = Color(0xFF2B2255)
val BbMenuPlay = Color(0xFFFF6B4A)
val BbMenuShop = Color(0xFF1FBFB2)
val BbMenuMissions = Color(0xFF8B5CF6)
val BbMenuSettings = Color(0xFF3B82F6)
val BbMenuGold = Color(0xFFFFC93C)

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
    onClaimDailyBonus: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BbMenuBgTop, BbMenuBgBot)))
    ) {
        // decorative floating puzzle-piece confetti, like the reference art
        Canvas(modifier = Modifier.fillMaxSize()) { drawMenuConfetti() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            // ---- top status bar: LEVEL * / coins / gems ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2B50", fontSize = 16.sp)
                    Text(
                        "  LEVEL $level",
                        color = BbMenuNavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                StatPill(icon = "\uD83E\uDE99", value = coins, color = Color(0xFFFF9F1C))
                Spacer(Modifier.width(8.dp))
                StatPill(icon = "\uD83D\uDC8E", value = gems, color = Color(0xFF3DA9FC))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(26.dp))

                // ---- puzzle-piece logo: 3x3 colorful jigsaw grid ----
                Canvas(modifier = Modifier.size(150.dp)) { drawJigsawLogo() }

                Spacer(Modifier.height(14.dp))
                Text(
                    "MAIN MENU",
                    color = BbMenuNavy,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
                Text(
                    "Star Puzzle \u2022 Ready to Play!",
                    color = BbMenuNavy.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(26.dp))

                MenuButton("Play", "\u25B6", BbMenuPlay) {
                    soundManager.play(SoundManager.Sfx.START)
                    onPlay()
                }
                Spacer(Modifier.height(12.dp))
                MenuButton("Shop", "\uD83D\uDED2", BbMenuShop) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onShop()
                }
                Spacer(Modifier.height(12.dp))
                MenuButton("Missions", "\u2705", BbMenuMissions) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onMissions()
                }
                Spacer(Modifier.height(12.dp))
                MenuButton("Settings", "\u2699", BbMenuSettings) {
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    onSettings()
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "BEST $best",
                    color = BbMenuNavy.copy(alpha = 0.55f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            // ---- daily bonus banner ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(listOf(BbMenuGold, Color(0xFFFFA726))),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = dailyBonusAvailable) {
                        soundManager.play(SoundManager.Sfx.COMPLETE)
                        onClaimDailyBonus()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83C\uDF81  ", fontSize = 16.sp)
                Text(
                    if (dailyBonusAvailable) "Daily Bonus Ready! Claim now" else "Daily Bonus Claimed \u2014 come back tomorrow",
                    color = Color(0xFF5A3A00),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Star Puzzle v8.4.0 \u2022 No internet required",
                color = BbMenuNavy.copy(alpha = 0.45f),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatPill(icon: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Text(
            "  $value",
            color = BbMenuNavy,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MenuButton(label: String, icon: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(color.copy(alpha = 0.95f), color)),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Color.White, fontSize = 20.sp)
        Text(
            "  $label",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
    }
}

/** 3x3 colorful jigsaw-piece logo, matching the reference art's puzzle icon. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJigsawLogo() {
    val colors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4FC3F7), Color(0xFFFFD54F),
        Color(0xFF66BB6A), Color(0xFF9575CD), Color(0xFFFF8A65),
        Color(0xFF4DB6AC), Color(0xFFBA68C8), Color(0xFFFFB74D)
    )
    val cell = size.minDimension / 3.4f
    val gap = cell * 0.08f
    val startX = (size.width - cell * 3f - gap * 2f) / 2f
    val startY = (size.height - cell * 3f - gap * 2f) / 2f
    val tab = cell * 0.22f
    for (row in 0 until 3) {
        for (col in 0 until 3) {
            val x = startX + col * (cell + gap)
            val y = startY + row * (cell + gap)
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        x, y, x + cell, y + cell,
                        androidx.compose.ui.geometry.CornerRadius(cell * 0.18f)
                    )
                )
            }
            drawPath(path, colors[(row * 3 + col) % colors.size], style = Fill)
            // small jigsaw tab nub between pieces for the puzzle look
            if (col < 2) {
                drawCircle(
                    color = colors[(row * 3 + col) % colors.size],
                    radius = tab / 2f,
                    center = Offset(x + cell, y + cell / 2f)
                )
            }
            if (row < 2) {
                drawCircle(
                    color = colors[(row * 3 + col) % colors.size],
                    radius = tab / 2f,
                    center = Offset(x + cell / 2f, y + cell)
                )
            }
        }
    }
}

/** Soft floating puzzle-piece confetti in the background, like the reference art. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMenuConfetti() {
    val pieces = listOf(
        Triple(0.08f, 0.12f, Color(0xFFFF8A80)),
        Triple(0.88f, 0.08f, Color(0xFF80DEEA)),
        Triple(0.90f, 0.30f, Color(0xFFFFD54F)),
        Triple(0.06f, 0.55f, Color(0xFF4FC3F7)),
        Triple(0.92f, 0.62f, Color(0xFF81C784)),
        Triple(0.10f, 0.85f, Color(0xFFBA68C8))
    )
    pieces.forEach { (fx, fy, color) ->
        val cx = size.width * fx
        val cy = size.height * fy
        val s = size.minDimension * 0.045f
        drawRoundRect(
            color = color.copy(alpha = 0.35f),
            topLeft = Offset(cx - s / 2f, cy - s / 2f),
            size = androidx.compose.ui.geometry.Size(s, s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.3f)
        )
    }
}

/**
 * Game over screen in the real game's style: GAME OVER title,
 * final score, BEST line, coin recap, PLAY AGAIN + MENU buttons.
 */
@Composable
fun BlockBlastGameOverScreen(
    score: Int,
    best: Int,
    coins: Int,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "GAME OVER",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(18.dp))
            Text(
                score.toString(),
                color = BbGold,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp
            )
            Text("SCORE", color = BbTextSoft, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
                        Brush.verticalGradient(listOf(Color(0xFFFFD34E), Color(0xFFFF9F1C))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onPlayAgain() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("PLAY AGAIN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
                Text("MENU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
