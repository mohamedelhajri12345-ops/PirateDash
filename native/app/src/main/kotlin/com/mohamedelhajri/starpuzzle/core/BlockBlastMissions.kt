package com.mohamedelhajri.starpuzzle.core

/**
 * BlockBlastMissions — three rotating daily missions for the native
 * Block Blast mode, deterministic per calendar day (same idea as the
 * classic DailyMissions but tracked against endless Block Blast play).
 * Rewards are plain coins — no loot boxes, no gambling loops.
 *
 * Progress is persisted by the host via [BbMissionStore]; all state is
 * in-memory here, so this object stays unit-testable.
 */
object BlockBlastMissions {

    data class Mission(
        val index: Int,
        val text: String,
        val target: Int,
        val reward: Int
    )

    /** Persist/restore hook set by the app at startup. */
    interface Store {
        fun loadProgress(day: String, index: Int): Int
        fun saveProgress(day: String, index: Int, value: Int)
        fun loadClaimed(day: String, index: Int): Boolean
        fun saveClaimed(day: String, index: Int, claimed: Boolean)
    }

    @Volatile var store: Store? = null

    /** Day key, e.g. 2026-10-06. */
    fun today(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    fun forDay(dayKey: String): List<Mission> {
        val rng = java.util.Random(dayKey.hashCode().toLong() * 7919L)
        val lines = 6 + rng.nextInt(7)          // clear 6..12 lines
        val pieces = 25 + rng.nextInt(11) * 5   // place 25..75 pieces
        val score = 400 + rng.nextInt(9) * 100  // score 400..1200 in one game
        return listOf(
            Mission(0, "Clear $lines lines", lines, 30 + rng.nextInt(3) * 10),
            Mission(1, "Place $pieces pieces", pieces, 30 + rng.nextInt(3) * 10),
            Mission(2, "Score $score in one game", score, 30 + rng.nextInt(3) * 10)
        )
    }

    fun progress(mission: Mission, day: String = today()): Int =
        store?.loadProgress(day, mission.index) ?: 0

    fun isClaimed(mission: Mission, day: String = today()): Boolean =
        store?.loadClaimed(day, mission.index) ?: false

    fun isComplete(mission: Mission, day: String = today()): Boolean =
        progress(mission, day) >= mission.target

    /**
     * Call after every placement. [gameScore] is the running score of
     * the current game (mission 2 tracks the best single game today).
     */
    fun onPlaced(piecesPlaced: Int, linesCleared: Int, gameScore: Int) {
        val day = today()
        val missions = forDay(day)
        val s = store ?: return
        // 0: total cleared lines
        missions[0].let { s.saveProgress(day, it.index, s.loadProgress(day, it.index) + linesCleared) }
        // 1: total pieces placed
        missions[1].let { s.saveProgress(day, it.index, s.loadProgress(day, it.index) + piecesPlaced) }
        // 2: best score in one game
        missions[2].let {
            val cur = s.loadProgress(day, it.index)
            if (gameScore > cur) s.saveProgress(day, it.index, gameScore)
        }
    }

    /** Claim the reward for a completed mission. */
    fun claim(mission: Mission): Int {
        val day = today()
        val s = store ?: return 0
        if (!isComplete(mission, day) || isClaimed(mission, day)) return 0
        s.saveClaimed(day, mission.index, true)
        return mission.reward
    }
}
