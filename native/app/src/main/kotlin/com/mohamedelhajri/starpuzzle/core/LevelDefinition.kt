package com.mohamedelhajri.starpuzzle.core

// A level definition, derived deterministically from its id.
// Faithful port of the v1.x LevelDefinition (same targets, same curve,
// same star gates — so the 1000-level adventure carries over).
data class LevelDefinition(
    val id: Int,
    val world: Int,
    val indexInWorld: Int,
    val objectiveType: Int,
    val targetScore: Int,
    val targetLines: Int,
    val targetCombo: Int,
    val maxMoves: Int,
    val timeLimit: Int,
    val prefillDensity: Int,
    val seed: Long,
    val specialChance: Int,
    val rewardCoins: Int,
    // v2.4 objective variety:
    //  SURVIVE — place targetCount pieces before the board blocks you
    //  CLEANUP — clear targetCount dirt cells the level starts with
    val targetCount: Int = 0,
    // visual prefill pattern: 0 random, 1 border, 2 checker, 3 corners,
    // 4 cross, 5 diagonals — every level band looks different
    val prefillPattern: Int = 0
) {
    companion object {
        const val TYPE_SCORE = 0
        const val TYPE_LINES = 1
        const val TYPE_TIME = 2
        const val TYPE_COMBO = 3
        const val TYPE_SURVIVE = 4
        const val TYPE_CLEANUP = 5
    }

    val isBoss: Boolean get() = indexInWorld == 100

    fun objectiveText(): String = when (objectiveType) {
        TYPE_SCORE -> "SCORE $targetScore"
        TYPE_LINES -> "CLEAR $targetLines LINES"
        TYPE_TIME -> "SCORE $targetScore IN ${timeLimit}s"
        TYPE_COMBO -> "COMBO x$targetCombo"
        TYPE_SURVIVE -> "PLACE $targetCount PIECES"
        TYPE_CLEANUP -> "CLEAR $targetCount BLOCKS"
        else -> "SURVIVE"
    }

    fun target(): Int = when (objectiveType) {
        TYPE_LINES -> targetLines
        TYPE_COMBO -> targetCombo
        TYPE_SURVIVE -> targetCount
        TYPE_CLEANUP -> targetCount
        else -> targetScore
    }

    fun starsFor(allowanceLeftFraction: Float): Int = when {
        allowanceLeftFraction >= 0.4f -> 3
        allowanceLeftFraction >= 0.2f -> 2
        else -> 1
    }
}
