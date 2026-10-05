package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.DailyMissions
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog

// ── The approved "Candy Cosmos" main menu ──────────────────────────────
// Bright candy gradient, glossy star mascot, kid-and-teen energy.
// Structure and navigation identical to the previous menu — only the
// look was rebuilt, exactly like the approved mock-up.

private val CandyPink = Color(0xFFFF6EC7)
private val CandyViolet = Color(0xFF8B5CF6)
private val CandyCyan = Color(0xFF22D3EE)
private val MenuGold = Color(0xFFFFD54F)
private val MenuGoldSoft = Color(0xFFFFE082)
private val MenuInk = Color(0xFF2A1B5E)
private val White = Color(0xFFFFFFFF)
private val BlockMint = Color(0xFF4DD9A8)
private val BlockCoral = Color(0xFFFF7A6B)
private val BlockLemon = Color(0xFFFFE066)
private val BlockSky = Color(0xFF5CC8FF)

/** The candy sky: gradient, twinkles, bokeh glow, sparkles. */
@Composable
private fun CandySky() {
    val transition = rememberInfiniteTransition(label = "candySky")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "candyT"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
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

        // ── soft nebula clouds: big, blurred, layered colour glow (depth) ──
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
            // soft glow halo behind the core (what makes a star read as luminous, not a flat dot)
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

        // ── hero stars: a few bright cross-flare stars (the "premium" accent) ──
        for (i in 0 until 6) {
            val x = prand(i + 1130) * w
            val y = prand(i + 1160) * h * 0.85f
            val pulse = 0.6f + 0.4f * kotlin.math.abs(kotlin.math.sin((t * 1.8f + prand(i) * 4f) * 3.1416f))
            val s = (7f + prand(i + 1190) * 7f).dp.toPx() * pulse
            val alpha = (0.55f + 0.45f * pulse).coerceIn(0.2f, 1f)
            // glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MenuGoldSoft.copy(alpha = alpha * 0.35f), MenuGoldSoft.copy(alpha = 0f)),
                    center = Offset(x, y),
                    radius = s * 3.2f
                ),
                radius = s * 3.2f,
                center = Offset(x, y)
            )
            // 4-point cross flare
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

        // ── a single slow shooting star streak (premium touch, very cheap) ──
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

/** A small glossy candy block that gently floats. */
@Composable
private fun CandyBlock(color: Color, phase: Float, sizeDp: Int) {
    val transition = rememberInfiniteTransition(label = "blockFloat")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "bt"
    )
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    0,
                    (kotlin.math.sin(t * 6.283f + phase) * 9f).dp.roundToPx()
                )
            }
            .graphicsLayer {
                rotationZ = kotlin.math.sin(t * 6.283f + phase) * 8f
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.72f))
                )
            )
            .border(1.5.dp, White.copy(alpha = 0.55f), shape)
    ) {
        // glossy top band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((sizeDp / 2).dp)
                .background(
                    Brush.verticalGradient(
                        listOf(White.copy(alpha = 0.35f), White.copy(alpha = 0f))
                    )
                )
        )
    }
}

/** The mascot: a glossy smiling star — the hero of the menu. */
@Composable
private fun StarMascot() {
    val transition = rememberInfiniteTransition(label = "mascot")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "mt"
    )
    Box(
        modifier = Modifier
            .size(170.dp)
            .graphicsLayer {
                translationY = kotlin.math.sin(t * 6.283f) * 10f
                rotationZ = kotlin.math.sin(t * 6.283f) * 2.5f
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val outer = size.minDimension * 0.46f
            val inner = outer * 0.5f
            // glow halo
            drawCircle(
                Brush.radialGradient(
                    listOf(MenuGoldSoft.copy(alpha = 0.55f), Color.Transparent),
                    center = c, radius = outer * 1.9f
                ),
                radius = outer * 1.9f, center = c
            )
            // star body
            val star = Path()
            var ang = -Math.PI / 2.0
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outer else inner
                val x = c.x + (r * kotlin.math.cos(ang)).toFloat()
                val y = c.y + (r * kotlin.math.sin(ang)).toFloat()
                if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                ang += Math.PI / 5.0
            }
            star.close()
            val bodyBrush = Brush.radialGradient(
                listOf(
                    Color(0xFFFFF176),
                    MenuGoldSoft,
                    Color(0xFFF9A825)
                ),
                center = Offset(c.x - outer * 0.25f, c.y - outer * 0.3f),
                radius = outer * 2.1f
            )
            drawPath(star, bodyBrush)
            // glossy crescent highlight top-left
            val hi = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset(c.x - outer * 0.52f, c.y - outer * 0.62f),
                        Size(outer * 0.62f, outer * 0.42f)
                    )
                )
            }
            clipPath(star) {
                drawPath(hi, White.copy(alpha = 0.30f))
            }
            // the face: eyes + smile + blush
            val eyeY = c.y - outer * 0.10f
            val eyeDX = outer * 0.26f
            for (s in intArrayOf(-1, 1)) {
                drawCircle(MenuInk, radius = outer * 0.085f,
                    center = Offset(c.x + s * eyeDX, eyeY))
                drawCircle(White, radius = outer * 0.03f,
                    center = Offset(c.x + s * eyeDX + outer * 0.03f, eyeY - outer * 0.03f))
            }
            drawArc(
                color = MenuInk,
                startAngle = -30f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(c.x - outer * 0.24f, eyeY + outer * 0.10f),
                size = Size(outer * 0.48f, outer * 0.40f),
                style = Stroke(width = outer * 0.05f, cap = StrokeCap.Round)
            )
            for (s in intArrayOf(-1, 1)) {
                drawCircle(
                    Color(0xFFFF8A95).copy(alpha = 0.55f),
                    radius = outer * 0.07f,
                    center = Offset(c.x + s * (eyeDX + outer * 0.22f), eyeY + outer * 0.08f)
                )
            }
        }
    }
}

/** A translucent candy-glass chip. */
@Composable
private fun CandyChip(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .background(White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .border(1.dp, White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) { content() }
}

/** The giant glossy gold PLAY button from the mock-up. */
@Composable
private fun HeroPlay(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(120),
        label = "heroScale"
    )
    val shape = RoundedCornerShape(38.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .scale(scale)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(MenuGoldSoft, MenuGold, Color(0xFFF9A825))
                )
            )
            .border(2.5.dp, White.copy(alpha = 0.65f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // glossy band across the top half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(White.copy(alpha = 0.42f), White.copy(alpha = 0f))
                    )
                )
        )
        Text(
            "PLAY",
            color = Color(0xFF5D3A00),
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            letterSpacing = 3.sp
        )
    }
}

/** A circular glossy icon button (shop / trophy / gear row). */
@Composable
private fun CandyIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector,
                            label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(White.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, White.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = White,
                modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = White.copy(alpha = 0.85f))
    }
}

@Composable
fun MainMenuScreen(
    progress: GameProgress,
    sound: SoundManager,
    soundOn: Boolean,
    musicOn: Boolean,
    hapticsOn: Boolean,
    motionOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleMotion: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onWorldMap: () -> Unit,
    onDaily: () -> Unit,
    onOpenStore: () -> Unit
) {
    val totalStars = progress.totalStars()
    val coins = progress.coins
    val nextLevel = progress.firstUnfinished()
    val level = remember(nextLevel) { LevelCatalog.getLevel(nextLevel) }
    var showSettings by remember { mutableStateOf(false) }
    var showAwards by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CandySky()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── top bar: live counters + gear ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CandyChip { Text("★ $totalStars", color = White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                Spacer(Modifier.width(8.dp))
                CandyChip { Text("● $coins", color = White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(White.copy(alpha = 0.18f), CircleShape)
                        .border(1.5.dp, White.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            sound.play(SoundManager.Sfx.CONFIRM)
                            showSettings = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings",
                        tint = White, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "STAR",
                style = MaterialTheme.typography.displayLarge,
                color = White,
                fontWeight = FontWeight.Black
            )
            Text(
                "PUZZLE",
                style = MaterialTheme.typography.displayLarge,
                color = MenuGoldSoft,
                fontWeight = FontWeight.Black
            )

            // ── mascot stage: the smiling star + floating candy blocks ──
            Box(contentAlignment = Alignment.Center) {
                CandyBlock(BlockMint, 0f, 34)
                Box(Modifier.offset(x = (-96).dp, y = 26.dp)) { CandyBlock(BlockCoral, 1.7f, 30) }
                Box(Modifier.offset(x = 96.dp, y = 26.dp)) { CandyBlock(BlockLemon, 3.4f, 30) }
                Box(Modifier.offset(x = (-64).dp, y = (-46).dp)) { CandyBlock(BlockSky, 5.1f, 26) }
                StarMascot()
            }

            Spacer(Modifier.height(18.dp))
            HeroPlay {
                sound.play(SoundManager.Sfx.START)
                onPlay()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Level $nextLevel · ${LevelCatalog.worldName(level.world)}",
                style = MaterialTheme.typography.labelLarge,
                color = White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CandyChip {
                    Text("🌍 WORLDS", color = White, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            sound.play(SoundManager.Sfx.CONFIRM); onWorldMap()
                        })
                }
                CandyChip {
                    Text("🔥 DAILY", color = White, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            sound.play(SoundManager.Sfx.CONFIRM); onDaily()
                        })
                }
            }

            Spacer(Modifier.height(26.dp))

            // ── daily streak: 7 glowing dots, day 7 is the gold star ──
            val streak = remember { progress.streakCount() }
            val streakDay = if (streak == 0) 0 else ((streak - 1) % 7) + 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (d in 1..7) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(30.dp)
                            .background(
                                if (d <= streakDay) MenuGold
                                else White.copy(alpha = 0.20f),
                                CircleShape
                            )
                            .border(
                                if (d == 7) 2.dp else 0.dp,
                                if (d == 7) White else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (d == 7) "★" else "$d",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (d <= streakDay) Color(0xFF5D3A00)
                            else White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (streakDay == 0) "Play today to start your streak"
                else "Day $streakDay — +100 coins every 7 days",
                style = MaterialTheme.typography.labelSmall,
                color = White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(16.dp))

            // ── today's missions, candy-glass card ──
            var missionsRefresh by remember { mutableIntStateOf(0) }
            val missionState = remember(missionsRefresh) { progress.dailyMissionState() }
            val missions = DailyMissions.forDay(missionState.day)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                    .border(1.dp, White.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "TODAY'S MISSIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                missions.forEachIndexed { i, m ->
                    val value = progress.missionValue(missionState, i)
                    val done = value >= m.target
                    val claimed = missionState.claimed[i]
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                m.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = White
                            )
                            Text(
                                "$value / ${m.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (done) Color(0xFFFFF176)
                                else White.copy(alpha = 0.75f)
                            )
                        }
                        if (!claimed) {
                            TextButton(
                                onClick = {
                                    if (progress.claimDailyMission(i)) {
                                        sound.play(SoundManager.Sfx.COIN)
                                        missionsRefresh++
                                    } else sound.play(SoundManager.Sfx.INVALID)
                                },
                                enabled = done,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "+${m.reward} ●",
                                    color = if (done) Color(0xFFFFF176)
                                    else White.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Text(
                                "CLAIMED",
                                style = MaterialTheme.typography.labelSmall,
                                color = White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── the mock-up bottom row: shop / trophy / gear ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CandyIconButton(Icons.Filled.ShoppingBag, "SHOP") {
                    sound.play(SoundManager.Sfx.CONFIRM); onOpenStore()
                }
                CandyIconButton(Icons.Filled.EmojiEvents, "AWARDS") {
                    sound.play(SoundManager.Sfx.CONFIRM); showAwards = true
                }
                CandyIconButton(Icons.Filled.Settings, "SETUP") {
                    sound.play(SoundManager.Sfx.CONFIRM); showSettings = true
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "v5.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAwards) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAwards = false }) {
            AchievementsDialog(progress) { showAwards = false }
        }
    }

    if (showSettings) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSettings = false }) {
            SettingsDialog(
                soundOn, musicOn, hapticsOn, motionOn,
                onToggleSound, onToggleMusic, onToggleHaptics, onToggleMotion
            ) { showSettings = false }
        }
    }
}

/** The achievements wall — progress visible, nothing to guess. */
@Composable
private fun AchievementsDialog(progress: GameProgress, onDismiss: () -> Unit) {
    data class Ach(val name: String, val desc: String, val done: Boolean, val prog: String)
    val t3 = progress.threeStarLevels()
    val stars = progress.totalStars()
    val bc = progress.bestCombo()
    val dd = progress.dailyDoneCount()
    val sk = progress.ownedSkins().size
    val bu = progress.boostersUsed()
    val achs = listOf(
        Ach("First Clear", "Win your first level", stars > 0, "${stars.coerceAtMost(1)}/1"),
        Ach("Perfect Run", "Finish a level with 3 stars", t3 >= 1, "$t3/1"),
        Ach("Combo Master", "Reach a x5 combo", bc >= 5, "$bc/5"),
        Ach("High Scorer", "Collect 100 stars", stars >= 100, "$stars/100"),
        Ach("Daily Player", "Complete 3 daily challenges", dd >= 3, "$dd/3"),
        Ach("Theme Collector", "Own 5 skins", sk >= 5, "$sk/5"),
        Ach("Booster Expert", "Use 25 boosters", bu >= 25, "$bu/25")
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MenuInk
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("ACHIEVEMENTS", style = MaterialTheme.typography.titleLarge,
                color = White)
            Spacer(Modifier.height(12.dp))
            achs.forEach { a ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (a.done) "★" else "○",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (a.done) MenuGold else Color(0xFF8F86C2)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            a.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (a.done) FontWeight.Bold else FontWeight.Normal,
                            color = White
                        )
                        Text(
                            a.desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC9C3E8)
                        )
                    }
                    Text(
                        if (a.done) "DONE" else a.prog,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (a.done) Color(0xFF66BB6A) else Color(0xFFC9C3E8)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    soundOn: Boolean,
    musicOn: Boolean,
    hapticsOn: Boolean,
    motionOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleMotion: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MenuInk
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("SETTINGS", style = MaterialTheme.typography.titleLarge,
                color = White)
            Spacer(Modifier.height(12.dp))
            SettingRow("Sound effects", soundOn, onToggleSound)
            SettingRow("Background music", musicOn, onToggleMusic)
            SettingRow("Haptic feedback", hapticsOn, onToggleHaptics)
            SettingRow("Motion effects", motionOn, onToggleMotion)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MenuGold,
                        contentColor = Color(0xFF5D3A00)
                    )
                ) { Text("OK") }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge,
            color = White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MenuGold,
                checkedTrackColor = MenuGold.copy(alpha = 0.3f)
            )
        )
    }
}
