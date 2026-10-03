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

// One color per piece shape (9 shapes = 9 colors). The size is
// contract-checked against the core so the palette can never again ship
// shorter than the piece list (the v2.0.x "crash on any move" class).
val PieceColors = listOf(
    Color(0xFFEF5350), // red
    Color(0xFFFFA726), // orange
    Color(0xFFFFEE58), // yellow
    Color(0xFF66BB6A), // green
    Color(0xFF42A5F5), // blue
    Color(0xFF7E57C2), // purple
    Color(0xFFEC407A), // pink
    Color(0xFF26C6DA), // cyan
    Color(0xFFAB47BC)  // violet — the 9th shape (was missing in v2.0.x)
).also {
    check(it.size == com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT) {
        "PieceColors must hold exactly PIECE_COLOR_COUNT colors"
    }
}

/** Safe palette lookup — any index resolves to a real color, never a crash. */
fun pieceColor(index: Int): Color =
    PieceColors[((index % PieceColors.size) + PieceColors.size) % PieceColors.size]

val SpecialStarColor = Color(0xFFFFD54F)
val SpecialBombColor = Color(0xFF8D6E63)
val SpecialLightningColor = Color(0xFF40C4FF)
