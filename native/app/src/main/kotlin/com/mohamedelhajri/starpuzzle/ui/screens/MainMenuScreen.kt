package com.mohamedelhajri.starpuzzle.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.DailyMissions
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog

// ── The approved celestial main menu ─────────────────────────────────
// Moon, stars, one gold accent, extreme minimalism.

private val MenuGold = Color(0xFFFFD54F)
private val MenuGoldSoft = Color(0xFFFFE082)
private val NavyTop = Color(0xFF0B1026)
private val NavyMid = Color(0xFF1B2C5C)

@Composable
private fun CelestialSky() {
    val transition = rememberInfiniteTransition(label = "sky")
    val twinkle by transition.animateFloat(
        0f, 1f, tween(7000, easing = LinearEasing), RepeatMode.Reverse, label = "tw"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(NavyTop, NavyMid, NavyTop),
                startY = 0f, endY = h
            )
        )
        // stars: deterministic positions, brightness breathes with time
        fun prand(seed: Int) = ((seed * 2654435761) % 10007) / 10007f
        for (i in 0 until 60) {
            val x = prand(i + 3) * w
            val y = prand(i + 37) * h * 0.85f
            val base = 0.3f + prand(i + 71) * 0.5f
            val alpha = base * (0.6f + 0.4f * kotlin.math.sin((twinkle + prand(i)) * 6.28f))
            val r = (1f + prand(i + 90) * 1.8f).dp.toPx() * 0.4f
            drawCircle(Color(0xFFF5F7FF).copy(alpha = alpha.coerceIn(0.05f, 1f)),
                radius = r, center = Offset(x, y))
        }
        // the moon: glow halo + disc + crescent shadow
        val mx = w * 0.76f
        val my = h * 0.18f
        val mr = w * 0.13f
        drawCircle(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF3C4).copy(alpha = 0.5f),
                    Color(0xFFFFF3C4).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(mx, my),
                radius = mr * 2.4f
            ),
            radius = mr * 2.4f,
            center = Offset(mx, my)
        )
        drawCircle(Color(0xFFFFF8E1), radius = mr, center = Offset(mx, my))
        drawCircle(NavyTop.copy(alpha = 0.92f), radius = mr,
            center = Offset(mx - mr * 0.55f, my - mr * 0.28f))
        // two soft drifting cloud bands
        val drift = twinkle * w * 0.04f
        drawOval(
            Color(0xFF3B4E8C).copy(alpha = 0.16f),
            topLeft = Offset(w * 0.05f + drift, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.05f)
        )
        drawOval(
            Color(0xFF3B4E8C).copy(alpha = 0.12f),
            topLeft = Offset(w * 0.45f - drift, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.045f)
        )
    }
}

@Composable
fun MainMenuScreen(
    progress: GameProgress,
    sound: SoundManager,
    soundOn: Boolean,
    musicOn: Boolean,
    hapticsOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        CelestialSky()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "STAR",
                style = MaterialTheme.typography.displayLarge,
                color = MenuGold
            )
            Text(
                "PUZZLE",
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFFF5F7FF)
            )
            Spacer(Modifier.height(10.dp))
            // star counter chip
            Box(
                modifier = Modifier
                    .border(1.dp, MenuGold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    "★ $totalStars   ● $coins",
                    style = MaterialTheme.typography.titleMedium,
                    color = MenuGoldSoft
                )
            }

            Spacer(Modifier.height(44.dp))

            // ── the big round glossy PLAY button ──
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(MenuGoldSoft, MenuGold, Color(0xFFE6A817))
                        ),
                        CircleShape
                    )
                    .border(2.dp, Color(0xFFFFE082), CircleShape)
                    .clickable {
                        sound.play(SoundManager.Sfx.PLACE)
                        onPlay()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PLAY",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = NavyTop
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Level $nextLevel · ${LevelCatalog.worldName(level.world)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(30.dp))

            // quiet chip row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuChip("WORLDS") { onWorldMap() }
                MenuChip("DAILY") { onDaily() }
                MenuChip("SHOP") { onOpenStore() }
                Text(
                    "v3.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF5F7FF).copy(alpha = 0.35f),
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── today's missions (kept feature, quiet styling) ──
            var missionsRefresh by remember { mutableIntStateOf(0) }
            val missionState = remember(missionsRefresh) { progress.dailyMissionState() }
            val missions = DailyMissions.forDay(missionState.day)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF16204A).copy(alpha = 0.85f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "TODAY'S MISSIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = if (claimed) Color(0xFF9FA8CC)
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$value / ${m.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (done) MenuGold
                                else Color(0xFF9FA8CC)
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
                                    color = if (done) MenuGold
                                    else Color(0xFF9FA8CC).copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Text(
                                "CLAIMED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9FA8CC)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSettings) {
        SettingsDialog(
            soundOn, musicOn, hapticsOn,
            onToggleSound, onToggleMusic, onToggleHaptics
        ) { showSettings = false }
    }
}

@Composable
private fun MenuChip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) { Text(label) }
}

@Composable
private fun SettingsDialog(
    soundOn: Boolean,
    musicOn: Boolean,
    hapticsOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF16204A),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("SETTINGS", style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF5F7FF))
            Spacer(Modifier.height(12.dp))
            SettingRow("Sound effects", soundOn, onToggleSound)
            SettingRow("Background music", musicOn, onToggleMusic)
            SettingRow("Haptic feedback", hapticsOn, onToggleHaptics)
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
            color = Color(0xFFF5F7FF), modifier = Modifier.weight(1f))
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
