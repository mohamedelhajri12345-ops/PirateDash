package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main menu — the primary visual statement of the game: deep night sky,
 * twinkling stars, a big warm PLAY button and quiet secondary actions.
 */
@Composable
fun MainMenuScreen(
    progress: GameProgress,
    sound: SoundManager,
    soundOn: Boolean,
    hapticsOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onWorldMap: () -> Unit,
    onDaily: () -> Unit
) {
    val totalStars = progress.totalStars()
    val nextLevel = progress.firstUnfinished()
    val level = remember(nextLevel) { LevelCatalog.getLevel(nextLevel) }
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1026), Color(0xFF1B2C5C), Color(0xFF0B1026))
                )
            )
    ) {
        TwinklingStars()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "STAR",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "PUZZLE",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "★ $totalStars",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))

            BigButton(
                text = "PLAY",
                onClick = {
                    sound.play(SoundManager.Sfx.PLACE)
                    onPlay()
                }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Level $nextLevel · ${LevelCatalog.worldName(level.world)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallButton("WORLDS") { onWorldMap() }
                SmallButton("DAILY") { onDaily() }

                Text(
                    "v2.1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(soundOn, hapticsOn, onToggleSound, onToggleHaptics) {
            showSettings = false
        }
    }
}

@Composable
private fun TwinklingStars() {
    val transition = rememberInfiniteTransition(label = "twinkle")
    val time by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkleTime"
    )
    val stars = remember {
        List(90) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        for ((fx, fy, phase) in stars) {
            val tw = 0.5f + 0.5f * sin((time * 6.28f + phase * 6.28f).toDouble()).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.25f + 0.55f * tw),
                radius = 1.2f + 1.8f * tw,
                center = Offset(fx * w, fy * h)
            )
        }
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.82f))))
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium, color = fg)
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsDialog(
    soundOn: Boolean,
    hapticsOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (soundOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("  Sound", style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f))
                    Switch(
                        checked = soundOn,
                        onCheckedChange = onToggleSound,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Vibration, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("  Haptics", style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f))
                    Switch(
                        checked = hapticsOn,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}
