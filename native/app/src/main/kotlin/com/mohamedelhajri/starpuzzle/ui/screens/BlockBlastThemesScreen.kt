package com.mohamedelhajri.starpuzzle.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.StarThemes

/**
 * THEMES (master prompt §32–§40): 8 data-driven themes. Selection
 * persists via the callback; all free during the testing phase.
 */
@Composable
fun BlockBlastThemesScreen(
    soundManager: SoundManager,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val theme = StarThemes.active
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable { soundManager.play(SoundManager.Sfx.BACK); onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("\u2190", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Text(
                "  THEMES",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        StarThemes.ALL.forEachIndexed { idx, t ->
            val selected = idx == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .shadow(if (selected) 8.dp else 3.dp, RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (selected) 0.16f else 0.08f), RoundedCornerShape(18.dp))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) t.accent else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        soundManager.play(SoundManager.Sfx.CONFIRM)
                        onSelect(idx)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // theme swatch: bg gradient + accent dot
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(30.dp)
                            .background(
                                Brush.horizontalGradient(listOf(t.bgTop, t.bgBottom, t.board)),
                                RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        t.name,
                        color = if (selected) t.accent else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    if (selected) "\u2713 ACTIVE" else "Tap to use",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "All themes are free during the testing phase.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp
        )
    }
}
