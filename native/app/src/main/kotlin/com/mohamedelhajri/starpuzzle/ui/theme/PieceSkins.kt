package com.mohamedelhajri.starpuzzle.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ─── Piece color themes (the reference game's "color themes", ours) ───
// Switching a theme instantly recolors every piece on the board and tray.
// The palette size contract is enforced per skin: can never ship short.

data class PieceSkin(val name: String, val colors: List<Color>)

object SkinState {
    var active by mutableIntStateOf(0)
}

val PieceSkins = listOf(
    PieceSkin("STARLIGHT", listOf( // the default night-sky palette
        Color(0xFFEF5350), Color(0xFFFFA726), Color(0xFFFFEE58),
        Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFF7E57C2),
        Color(0xFFEC407A), Color(0xFF26C6DA), Color(0xFFAB47BC)
    )),
    PieceSkin("ROSE", listOf(
        Color(0xFFEF9A9A), Color(0xFFF48FB1), Color(0xFFEC407A),
        Color(0xFFD81B60), Color(0xFFFF8A65), Color(0xFFFFB74D),
        Color(0xFFE57373), Color(0xFFAD1457), Color(0xFFF06292)
    )),
    PieceSkin("ICE", listOf(
        Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFF039BE5),
        Color(0xFF80DEEA), Color(0xFF4DD0E1), Color(0xFF26C6DA),
        Color(0xFF5C7CFA), Color(0xFF7986CB), Color(0xFFB3E5FC)
    )),
    PieceSkin("NEON", listOf(
        Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFFFF4081),
        Color(0xFFD500F9), Color(0xFFFFD600), Color(0xFFFF3D00),
        Color(0xFF651FFF), Color(0xFF00BFA5), Color(0xFFFF1744)
    )),
    PieceSkin("MONO", listOf( // premium single-hue (reference video's white theme)
        Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD),
        Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFFEEEEEE),
        Color(0xFFD6D6D6), Color(0xFF8C8C8C), Color(0xFF616161)
    )),
    PieceSkin("GOLD", listOf(
        Color(0xFFFFD54F), Color(0xFFFFC107), Color(0xFFFFB300),
        Color(0xFFFFE082), Color(0xFFFFCA28), Color(0xFFE6A817),
        Color(0xFFFFF176), Color(0xFFD4AF37), Color(0xFFFF8F00)
    ))
).also { skins ->
    check(skins.isNotEmpty()) { "at least one piece skin required" }
    for (skin in skins) check(skin.colors.size ==
            com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT) {
        "skin ${skin.name} must hold exactly PIECE_COLOR_COUNT colors"
    }
}
