package com.mohamedelhajri.starpuzzle.core

/**
 * Three rotating missions per calendar day, deterministic from the date:
 * the same three goals for every player, fair and simple.
 * Rewards are plain coins — no loot boxes, no gambling loops.
 */
object DailyMissions {

    data class Mission(val index: Int, val text: String, val target: Int, val reward: Int)

    fun forDay(dayKey: String): List<Mission> {
        val rng = java.util.Random(dayKey.hashCode().toLong() * 7919L)
        val lines = 8 + rng.nextInt(9)            // clear 8..16 lines
        val score = 800 + rng.nextInt(9) * 100    // score 800..1600
        val levels = 2 + rng.nextInt(3)           // complete 2..4 levels
        return listOf(
            Mission(0, "Clear $lines lines today", lines, 25 + rng.nextInt(3) * 5),
            Mission(1, "Score $score today", score, 25 + rng.nextInt(3) * 5),
            Mission(2, "Complete $levels levels", levels, 25 + rng.nextInt(3) * 5)
        )
    }
}
