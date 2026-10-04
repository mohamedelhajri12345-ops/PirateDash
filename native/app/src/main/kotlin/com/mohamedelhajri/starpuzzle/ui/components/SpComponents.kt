package com.mohamedelhajri.starpuzzle.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager

/**
 * ─── STARPUZZLE DESIGN SYSTEM v4.0 ───
 *
 * ONE visual language for every screen. Never pick a color or a corner
 * radius inside a screen — take it from here so the game reads as one
 * coherent product, not a patchwork of screens.
 */
object SpDesign {
    // ── COLOR TOKENS ──
    val BgDeep = Color(0xFF0B1026)      // night sky base
    val BgMid = Color(0xFF1B2C5C)       // gradient middle
    val Surface = Color(0xFF16204A)     // cards / dialogs
    val Gold = Color(0xFFFFD54F)        // primary reward accent
    val GoldSoft = Color(0xFFFFE082)    // highlight
    val GoldDark = Color(0xFFE6A817)    // pressed / deep gold
    val TextBright = Color(0xFFF5F7FF)  // main text
    val TextMuted = Color(0xFF9FA8CC)   // secondary text
    val Success = Color(0xFF66BB6A)    // owned / done
    val LockedGray = Color(0xFF6B7499)  // locked / disabled
    val AccentCyan = Color(0xFF40C4FF)  // secondary accent
    val EpicPurple = Color(0xFFB388FF)  // rarity: epic

    // ── SPACING TOKENS (dp) ──
    const val SpaceXs = 4
    const val SpaceS = 8
    const val SpaceM = 16
    const val SpaceL = 24
    const val SpaceXl = 32

    // ── RADIUS TOKENS (dp) ──
    const val RadiusCard = 20
    const val RadiusChip = 16
    const val RadiusButton = 26

    // ── ANIMATION TOKENS ──
    const val PressScale = 0.94f
    const val PressMs = 90
}

/**
 * The house button: glossy gold body, breathing border glow for the hero
 * CTA, press scale feedback, haptic tick. One button = one feel everywhere.
 */
@Composable
fun SpButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    glow: Boolean = false,
    sound: SoundManager? = null,
    sfx: SoundManager.Sfx = SoundManager.Sfx.CONFIRM
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) SpDesign.PressScale else 1f,
        animationSpec = tween(SpDesign.PressMs),
        label = "pressScale"
    )
    val glowPulse by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "gp"
    )
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(if (big) 150.dp else 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                Brush.verticalGradient(
                    listOf(SpDesign.GoldSoft, SpDesign.Gold, SpDesign.GoldDark)
                ),
                CircleShape
            )
            .border(
                3.dp,
                if (glow) Color(0xFFFFF3C4).copy(alpha = 0.35f + 0.45f * glowPulse)
                else Color(0xFFFFE082),
                CircleShape
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(sfx)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = if (big) 26.sp else 13.sp,
            fontWeight = FontWeight.Black,
            color = SpDesign.BgDeep,
            letterSpacing = if (big) 2.sp else 1.sp
        )
    }
}

/** The house currency chip: stars, coins, anything countable. */
@Composable
fun CurrencyChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, SpDesign.Gold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .background(SpDesign.Gold.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = SpDesign.GoldSoft
        )
    }
}
