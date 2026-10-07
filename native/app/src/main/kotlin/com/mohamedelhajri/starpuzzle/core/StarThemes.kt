package com.mohamedelhajri.starpuzzle.core

import androidx.compose.ui.graphics.Color

/**
 * Data-driven themes (master prompt §32–§40). Each theme redefines the
 * background gradient, board surface, empty-cell tint and accent color.
 * Selection is persisted by the UI layer via [StarThemes.activeId].
 */
data class StarTheme(
    val name: String,
    val bgTop: Color,
    val bgBottom: Color,
    val board: Color,
    val cell: Color,
    val accent: Color
)

object StarThemes {
    val AURORA = StarTheme("Aurora", Color(0xFF0B1026), Color(0xFF1B2A4A), Color(0xFF16223E), Color(0xFF0E1830), Color(0xFF35E0C2))
    val MIDNIGHT = StarTheme("Midnight", Color(0xFF05070F), Color(0xFF0D1226), Color(0xFF0C1226), Color(0xFF070C1E), Color(0xFF4C7DF0))
    val OCEAN = StarTheme("Ocean", Color(0xFF04263A), Color(0xFF0A4A66), Color(0xFF0A3D5C), Color(0xFF062E48), Color(0xFF2FD4F0))
    val SUNSET = StarTheme("Sunset", Color(0xFF2B0F2E), Color(0xFF5C1F42), Color(0xFF4A1838), Color(0xFF33102A), Color(0xFFFF7E5F))
    val NEON = StarTheme("Neon", Color(0xFF0A0714), Color(0xFF1A0B2E), Color(0xFF150A28), Color(0xFF0D0620), Color(0xFFB14CFF))
    val FOREST = StarTheme("Forest", Color(0xFF07200F), Color(0xFF123B22), Color(0xFF0E3119), Color(0xFF092612), Color(0xFF54E08A))
    val CANDY = StarTheme("Candy", Color(0xFF3A1B4E), Color(0xFF5F2B63), Color(0xFF52255E), Color(0xFF3B1B4C), Color(0xFFFF6FB5))
    val GALAXY = StarTheme("Galaxy", Color(0xFF070318), Color(0xFF180F3A), Color(0xFF130C32), Color(0xFF0C0726), Color(0xFF7F7FFF))

    val ALL = listOf(AURORA, MIDNIGHT, OCEAN, SUNSET, NEON, FOREST, CANDY, GALAXY)

    var activeId: Int = 0
    val active: StarTheme get() = ALL[activeId.coerceIn(0, ALL.size - 1)]
}
