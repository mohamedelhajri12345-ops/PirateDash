package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
/**
 * Static premium backdrop: deep night gradient with a faint vignette.
 * Replaces the old animated cosmos (owner verdict: animated backgrounds
 * and drawn stars looked cheap). Same signature, zero per-frame work.
 */
@Composable
fun CosmosBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF16162A),
                        Color(0xFF1C1C36),
                        Color(0xFF101020)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF000000).copy(alpha = 0.45f)),
                    radius = 1400f
                )
            )
    )
}
