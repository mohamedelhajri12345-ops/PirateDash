package com.mohamedelhajri.starpuzzle.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Star Puzzle design system: premium casual, night-sky identity ───

// Backgrounds
val NightSkyTop = Color(0xFF0B1026)
val NightSkyBottom = Color(0xFF1B2C5C)
val SurfaceCard = Color(0xFF16204A)
val SurfaceCardHigh = Color(0xFF1F2C5E)

// Accents
val GoldPrimary = Color(0xFFFFD54F)
val GoldSoft = Color(0xFFFFE082)
val AccentBlue = Color(0xFF5C7CFA)
val SuccessGreen = Color(0xFF66BB6A)
val DangerRed = Color(0xFFEF5350)
val TextPrimary = Color(0xFFF5F7FF)
val TextSecondary = Color(0xFF9FA8CC)

// One color per piece shape (9 shapes = 9 colors), per selectable skin.
// The size contract lives in PieceSkins.kt: it can never again ship
// shorter than the piece list (the v2.0.x "crash on any move" class).
private val PieceColors get() = PieceSkins[SkinState.active].colors

/** Safe palette lookup — any index resolves to a real color, never a crash. */
fun pieceColor(index: Int): Color {
    val colors = PieceColors
    return colors[((index % colors.size) + colors.size) % colors.size]
}

val SpecialStarColor = Color(0xFFFFD54F)
val SpecialBombColor = Color(0xFF8D6E63)
val SpecialLightningColor = Color(0xFF40C4FF)
