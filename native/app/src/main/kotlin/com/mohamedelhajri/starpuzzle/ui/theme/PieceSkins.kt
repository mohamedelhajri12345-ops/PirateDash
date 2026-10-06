package com.mohamedelhajri.starpuzzle.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ─── The 20-skin catalog (design-team approved palette system) ───
// Skins are bought in the SHOP with in-game coins only.
// Contract: every skin holds EXACTLY PIECE_COLOR_COUNT colors.

data class PieceSkin(val name: String, val colors: List<Color>)

object SkinState {
    var active by mutableIntStateOf(0)
}

val PieceSkins = listOf(
    // Block Blast reference palette (game.js COLORS) + one violet to fill the 9-slot contract
    PieceSkin("BLAST", listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
        Color(0xFF96CEB4), Color(0xFFFFEAA7), Color(0xFFDDA0DD), Color(0xFFFF8C42), Color(0xFF74B9FF), Color(0xFFA29BFE)
    )),
    PieceSkin("ROSE", listOf(
        Color(0xFFEF9A9A), Color(0xFFF48FB1), Color(0xFFEC407A),
        Color(0xFFD81B60), Color(0xFFFF8A65), Color(0xFFFFB74D), Color(0xFFE57373), Color(0xFFAD1457), Color(0xFFF06292)
    )),
    PieceSkin("ICE", listOf(
        Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFF039BE5),
        Color(0xFF80DEEA), Color(0xFF4DD0E1), Color(0xFF26C6DA), Color(0xFF5C7CFA), Color(0xFF7986CB), Color(0xFFB3E5FC)
    )),
    PieceSkin("NEON", listOf(
        Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFFFF4081),
        Color(0xFFD500F9), Color(0xFFFFD600), Color(0xFFFF3D00), Color(0xFF651FFF), Color(0xFF00BFA5), Color(0xFFFF1744)
    )),
    PieceSkin("MONO", listOf(
        Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD),
        Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFFEEEEEE), Color(0xFFD6D6D6), Color(0xFF8C8C8C), Color(0xFF616161)
    )),
    PieceSkin("GOLD", listOf(
        Color(0xFFFFD54F), Color(0xFFFFC107), Color(0xFFFFB300),
        Color(0xFFFFE082), Color(0xFFFFCA28), Color(0xFFE6A817), Color(0xFFFFF176), Color(0xFFD4AF37), Color(0xFFFF8F00)
    )),
    PieceSkin("GALAXY", listOf(
        Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFFE040FB),
        Color(0xFFAA00FF), Color(0xFF651FFF), Color(0xFF7E57C2), Color(0xFFF50057), Color(0xFF3D5AFE), Color(0xFFD500F9)
    )),
    PieceSkin("CANDY", listOf(
        Color(0xFFFF8A80), Color(0xFFF48FB1), Color(0xFFFFE082),
        Color(0xFFA5D6A7), Color(0xFF80DEEA), Color(0xFFB39DDB), Color(0xFFFFAB91), Color(0xFFE6EE9C), Color(0xFF90CAF9)
    )),
    PieceSkin("OCEAN", listOf(
        Color(0xFF00ACC1), Color(0xFF26C6DA), Color(0xFF42A5F5),
        Color(0xFF5C6BC0), Color(0xFF00897B), Color(0xFF4DD0E1), Color(0xFF3949AB), Color(0xFF80CBC4), Color(0xFF1E88E5)
    )),
    PieceSkin("SAKURA", listOf(
        Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFEC407A),
        Color(0xFFF06292), Color(0xFFFFA726), Color(0xFFE1BEE7), Color(0xFFFFCDD2), Color(0xFFD81B60), Color(0xFFFFE0B2)
    )),
    PieceSkin("EMERALD", listOf(
        Color(0xFF66BB6A), Color(0xFFA5D6A7), Color(0xFF2E7D32),
        Color(0xFF9CCC65), Color(0xFF00C853), Color(0xFF81C784), Color(0xFF388E3C), Color(0xFFC5E1A5), Color(0xFF558B2F)
    )),
    PieceSkin("SUNSET", listOf(
        Color(0xFFFF7043), Color(0xFFFFA726), Color(0xFFFFCA28),
        Color(0xFFFF5252), Color(0xFFFF8A65), Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFFF4511E), Color(0xFFFFE57F)
    )),
    PieceSkin("AURORA", listOf(
        Color(0xFF00E5FF), Color(0xFF64FFDA), Color(0xFF69F0AE),
        Color(0xFFB388FF), Color(0xFF18FFFF), Color(0xFF1DE9B6), Color(0xFFEEFF41), Color(0xFF7C4DFF), Color(0xFF84FFFF)
    )),
    PieceSkin("SAPPHIRE", listOf(
        Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFF64B5F6),
        Color(0xFF90CAF9), Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF3949AB), Color(0xFF82B1FF), Color(0xFF4FC3F7)
    )),
    PieceSkin("VOLCANO", listOf(
        Color(0xFFE53935), Color(0xFFFF5722), Color(0xFFFF7043),
        Color(0xFFFF8A65), Color(0xFFD32F2F), Color(0xFFF4511E), Color(0xFFC62828), Color(0xFFFFAB91), Color(0xFF8D6E63)
    )),
    PieceSkin("JEWEL", listOf(
        Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5),
        Color(0xFF8E24AA), Color(0xFFFDD835), Color(0xFFFF8F00), Color(0xFF00ACC1), Color(0xFFEC407A), Color(0xFF5E35B1)
    )),
    PieceSkin("MINT", listOf(
        Color(0xFF80CBC4), Color(0xFF4DB6AC), Color(0xFF26A69A),
        Color(0xFF009688), Color(0xFFB2DFDB), Color(0xFF80DEEA), Color(0xFF66BB6A), Color(0xFFA7FFEB), Color(0xFF00897B)
    )),
    PieceSkin("LAVENDER", listOf(
        Color(0xFFB39DDB), Color(0xFF9575CD), Color(0xFF7E57C2),
        Color(0xFF673AB7), Color(0xFFD1C4E9), Color(0xFFC5CAE9), Color(0xFFFF8A80), Color(0xFF9FA8DA), Color(0xFF5C6BC0)
    )),
    PieceSkin("MIDNIGHT", listOf(
        Color(0xFF5C7CFA), Color(0xFF42A5F5), Color(0xFF26C6DA),
        Color(0xFFAB47BC), Color(0xFF7986CB), Color(0xFF5E35B1), Color(0xFF26A69A), Color(0xFF7C4DFF), Color(0xFF1E88E5)
    )),
    PieceSkin("CARBON", listOf(
        Color(0xFF90A4AE), Color(0xFF78909C), Color(0xFF607D8B),
        Color(0xFF455A64), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFFFFD54F), Color(0xFFFFC107), Color(0xFFECEFF1)
    ))
).also { skins ->
    check(skins.isNotEmpty()) { "at least one piece skin required" }
    for (skin in skins) check(skin.colors.size ==
            com.mohamedelhajri.starpuzzle.core.Piece.PIECE_COLOR_COUNT) {
        "skin ${skin.name} must hold exactly PIECE_COLOR_COUNT colors"
    }
}

/** Coin price of a skin (0 = owned-by-default). */
fun skinPrice(skinId: Int): Int = if (skinId <= 0) 0 else 200 + skinId * 10
