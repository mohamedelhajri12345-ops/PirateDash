package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.core.BlockBlastSpec

/**
 * Block Blast home screen structure, built natively in Compose:
 * avatar + coin chip top row, glossy letter-tile STAR logo + PUZZLE
 * wordmark, star mascot, wide gold 3D PLAY button, SHOP / MISSIONS
 * pills, bottom icon row.
 */
@Composable
fun BlockBlastMenuScreen(
    best: Int,
    coins: Int,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground)
    ) {
        // ---- top row: avatar + coins ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF5876CA), Color(0xFF3B4E9E))),
                        CircleShape
                    )
                    .border(3.dp, BbGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            BbCoinChip(coins, big = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- glossy letter-tile logo: STAR ----
            val tiles = remember {
                listOf(
                    "S" to BlockBlastSpec.COLORS[0],
                    "T" to BlockBlastSpec.COLORS[2],
                    "A" to BlockBlastSpec.COLORS[4],
                    "R" to BlockBlastSpec.COLORS[1]
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                tiles.forEach { (letter, color) ->
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(color.toLong() or 0xFF000000L),
                                        Color(color.toLong() or 0xFF000000L)
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .shadow(6.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            letter,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        )
                    }
                }
            }
            Text(
                "PUZZLE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                letterSpacing = 12.sp,
                modifier = Modifier.padding(top = 10.dp)
            )

            // ---- star mascot ----
            Canvas(modifier = Modifier.size(120.dp).padding(top = 8.dp)) {
                drawStarMascot()
            }

            // ---- wide gold PLAY button ----
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(top = 18.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFD34E), Color(0xFFFFB32E), Color(0xFFFF9F1C))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onPlay() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PLAY",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp
                )
            }

            // ---- secondary pills ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                PillButton("SHOP")
                PillButton("MISSIONS")
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "BEST $best",
                color = BbTextSoft,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )

            // ---- bottom icon row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val icons = listOf("\uD83C\uDF82", "\uD83C\uDFAF", "\uD83D\uDCAE", "\u2699")
                icons.forEach { icon ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 9.dp)
                            .size(46.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 21.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String) {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .border(2.dp, BbGold, RoundedCornerShape(24.dp))
            .clickable { }
            .padding(horizontal = 22.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = BbGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )
    }
}

/** Star mascot with a face, matching the home-screen mood of the real game. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarMascot() {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val outer = w * 0.46f
    val inner = outer * 0.42f

    val path = Path()
    for (i in 0 until 10) {
        val angle = Math.toRadians((-90 + i * 36).toDouble())
        val radius = if (i % 2 == 0) outer else inner
        val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
        val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    // golden body
    drawPath(path, Color(0xFFFFC93C), style = Fill)
    // outline
    drawPath(path, Color(0xFFB8860B), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
    // face
    drawCircle(Color(0xFF3A3A55), radius = w * 0.05f, center = Offset(cx - w * 0.10f, cy - h * 0.04f))
    drawCircle(Color(0xFF3A3A55), radius = w * 0.05f, center = Offset(cx + w * 0.10f, cy - h * 0.04f))
    drawCircle(Color.White, radius = w * 0.017f, center = Offset(cx - w * 0.085f, cy - h * 0.055f))
    drawCircle(Color.White, radius = w * 0.017f, center = Offset(cx + w * 0.115f, cy - h * 0.055f))
    // smile
    val smile = Path()
    smile.moveTo(cx - w * 0.09f, cy + h * 0.10f)
    smile.quadraticBezierTo(cx, cy + h * 0.17f, cx + w * 0.09f, cy + h * 0.10f)
    drawPath(smile, Color(0xFF3A3A55), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
}

/**
 * Game over screen in the real game's style: GAME OVER title,
 * final score, BEST line, coin chip, gold PLAY AGAIN button.
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
                color = BbGold,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                score.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 58.sp
            )
            Text(
                "BEST $best",
                color = BbTextSoft,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            BbCoinChip(coins, big = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .padding(top = 18.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFD34E), Color(0xFFFFB32E), Color(0xFFFF9F1C))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onPlayAgain() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PLAY AGAIN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
            }
            Text(
                "MENU",
                color = BbTextSoft,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable { onMenu() }
            )
        }
    }
}
