package com.mohamedelhajri.starpuzzle.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager

/** Settings screen matching the Main Menu's light pastel style. */
@Composable
fun BlockBlastSettingsScreen(
    soundInitial: Boolean,
    hapticsInitial: Boolean,
    soundManager: SoundManager,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var soundOn by remember { mutableStateOf(soundInitial) }
    var hapticsOn by remember { mutableStateOf(hapticsInitial) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BbMenuBgTop, BbMenuBgBot)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2190", color = BbMenuNavy, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Text(
                    "  SETTINGS",
                    color = BbMenuNavy,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            SettingRow("\uD83D\uDD0A", "Sound Effects", soundOn) {
                soundOn = it
                soundManager.enabled = it
                onSoundChanged(it)
                if (it) soundManager.play(SoundManager.Sfx.CONFIRM)
            }
            Spacer(Modifier.height(14.dp))
            SettingRow("\uD83D\uDCF3", "Vibration", hapticsOn) {
                hapticsOn = it
                onHapticsChanged(it)
                if (it) soundManager.play(SoundManager.Sfx.CONFIRM)
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Star Puzzle v8.4.0",
                color = BbMenuNavy.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingRow(icon: String, label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Text("  $label", color = BbMenuNavy, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(28.dp)
                .background(
                    if (value) BbMenuShop else Color(0xFFE0E0E0),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onToggle(!value) },
            contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
