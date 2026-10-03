package com.mohamedelhajri.starpuzzle.core

import java.util.Calendar
import java.util.Random

// Deterministic catalog of 1000 levels across 10 worlds.
// Faithful port of the v1.x LevelCatalog: the same id always produces the
// exact same level on every device. Pure Kotlin, no Android dependency.
object LevelCatalog {

    const val LEVELS_PER_WORLD = 100
    const val TOTAL_WORLDS = 10
    const val TOTAL_LEVELS = LEVELS_PER_WORLD * TOTAL_WORLDS

    private val WORLD_NAMES = arrayOf(
        "Green Valley", "Golden Desert", "Coral Ocean", "Frozen Peaks",
        "Ember Volcano", "Dino Jungle", "Neon City", "Candy Wonderland",
        "Cosmic Space", "Sunken Treasure"
    )

    private val WORLD_UNLOCK_STARS = intArrayOf(0, 30, 70, 120, 170, 220, 270, 320, 370, 420)

    private const val SEED_SALT = 53701221L // "STARPUZZLE" flavor

    fun worldName(world: Int): String =
        WORLD_NAMES[world.coerceIn(1, TOTAL_WORLDS) - 1]

    fun worldUnlockStars(world: Int): Int =
        WORLD_UNLOCK_STARS[world.coerceIn(1, TOTAL_WORLDS) - 1]

    /** Level id -> result of the objective evaluation, injected via stars map. */
    fun firstUnfinishedLevel(stars: Map<Int, Int>): Int {
        for (id in 1..TOTAL_LEVELS) if ((stars[id] ?: 0) == 0) return id
        return TOTAL_LEVELS
    }

    /** Deterministic daily challenge: one level per day, mid difficulty band. */
    fun dailyLevelId(): Int {
        val now = Calendar.getInstance()
        val day = now.get(Calendar.YEAR) * 10000 +
                (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH)
        val random = Random(day * SEED_SALT)
        return 101 + random.nextInt(400) // levels 101..500 band
    }

    fun dailyKey(): String {
        val now = Calendar.getInstance()
        return "%04d%02d%02d".format(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Handcrafted tutorial arc (levels 1-10, world 1): every level is
     * designed by hand to teach exactly ONE thing, with arithmetic-level
     * winnability margins (avg placement ~2.7 pts, a line clear = +10).
     */
    private fun opener(id: Int, type: Int, target: Int, moves: Int): LevelDefinition =
        LevelDefinition(
            id, 1, id, type,
            if (type == LevelDefinition.TYPE_SCORE) target else 0,
            if (type == LevelDefinition.TYPE_LINES) target else 0,
            0, moves, 0,
            0, id * 2654435761L + SEED_SALT, 5, 10 + id
        )

    private val OPENERS = arrayOf(
        opener(1, LevelDefinition.TYPE_SCORE, 60, 25),  // place-and-win: pure placement reaches ~65
        opener(2, LevelDefinition.TYPE_SCORE, 80, 24),  // first clears genuinely help
        opener(3, LevelDefinition.TYPE_LINES, 1, 25),   // teaches WHAT a line clear is
        opener(4, LevelDefinition.TYPE_SCORE, 100, 24),
        opener(5, LevelDefinition.TYPE_LINES, 2, 26),
        opener(6, LevelDefinition.TYPE_SCORE, 120, 25),
        opener(7, LevelDefinition.TYPE_LINES, 2, 22),   // a tighter allowance, gently
        opener(8, LevelDefinition.TYPE_SCORE, 140, 26), // specials noticed at 5%
        opener(9, LevelDefinition.TYPE_LINES, 3, 30),
        opener(10, LevelDefinition.TYPE_LINES, 4, 35)   // graduation to real pacing
    )

    /** Build a level from its id. Pure and deterministic. */
    fun getLevel(id: Int): LevelDefinition {
        require(id in 1..TOTAL_LEVELS) { "level id out of range: $id" }
        // the tutorial arc is handcrafted; everything after follows the curve
        if (id in 1..10) return OPENERS[id - 1]

        val world = (id - 1) / LEVELS_PER_WORLD + 1
        val indexInWorld = (id - 1) % LEVELS_PER_WORLD + 1

        val random = Random(id * 2654435761L + SEED_SALT)

        val t = (indexInWorld - 1).toFloat() / (LEVELS_PER_WORLD - 1).toFloat()
        val w = (world - 1).toFloat()

        val objectiveType = pickObjective(random, world, indexInWorld)

        val prefillDensity =
            if (world < 4) 0
            else minOf(2 + (world - 4) + (t * 5).toInt(), 20)

        val specialChance = if (world >= 7) 10 + (world - 7) * 2 else 5

        // visual prefill pattern rotates through the geometric families
        val prefillPattern = (world + id / 10) % 6

        val rewardCoins = 10 + id / 20 + (if (indexInWorld == 100) 40 else 0)

        // ─── v2.3.0 difficulty redesign ───
        // Design goal (user feedback: "15 moves run out and I never finish"):
        // every level must be completable by an ordinary player with a real
        // margin. Points per move baseline: ~3 (piece area) + line clears
        // (~10 per line + combo bonuses). Early worlds are place-and-win
        // tutorials; only late worlds ask for chained clears.
        return when (objectiveType) {
            LevelDefinition.TYPE_LINES -> {
                // a line every ~3 moves is the comfortable human pace
                val targetLines = 2 + (world / 2) + (t * 4).toInt() +
                        (if (random.nextInt(2) == 1) 1 else 0)
                val maxMoves = targetLines * 3 + 10 + world
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, targetLines, 0, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins,
                    0, prefillPattern)
            }
            LevelDefinition.TYPE_TIME -> {
                val timeLimit = 90 + random.nextInt(45)
                // ~1.5-2s per move; sustained score of a decent player is
                // roughly (2 + 0.35w + 0.8t) points per second
                val targetScore = Math.round(timeLimit * (1.6f + w * 0.22f + t * 0.35f))
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    targetScore, 0, 0, 0, timeLimit,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins,
                    0, prefillPattern)
            }
            LevelDefinition.TYPE_COMBO -> {
                val targetCombo = (2 + w / 3).toInt().coerceIn(2, 5)
                val maxMoves = 20 + world + random.nextInt(4)
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, 0, targetCombo, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins,
                    0, prefillPattern)
            }
            LevelDefinition.TYPE_SURVIVE -> {
                // place N pieces without the board ever blocking you.
                // Stars are earned by chaining combos during the run.
                val targetPieces = (12 + (t * 6).toInt() + (world - 3)).coerceIn(12, 24)
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, 0, 0, targetPieces, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins,
                    targetPieces, prefillPattern)
            }
            LevelDefinition.TYPE_CLEANUP -> {
                // the level opens with a patterned patch of dirt blocks:
                // clear targetCount of them via lines or power-ups
                val dirt = 18 + (t * 10).toInt() + (world / 2)
                val targetBlocks = (dirt * 0.6f).toInt().coerceAtLeast(6)
                val maxMoves = targetBlocks * 2 + 14 + world
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, 0, 0, maxMoves, 0,
                    dirt, random.nextLong(), specialChance, rewardCoins,
                    targetBlocks, prefillPattern)
            }
            else -> { // TYPE_SCORE
                val maxMoves = 20 + world + (t * 8).toInt() + random.nextInt(4)
                // Human sustained rate: ~7-8 pts/move for a decent player.
                // W1 ~3.2 (pure placement finishes it) rising to ~10 in
                // world 10 (needs regular clears and combos — boss tier)
                val targetScore = Math.round(maxMoves * (3.2f + w * 0.6f + t * 1.2f))
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    targetScore, 0, 0, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins,
                    0, prefillPattern)
            }
        }
    }

    private fun pickObjective(random: Random, world: Int, indexInWorld: Int): Int {
        if (indexInWorld == 100)
            return if (world >= 8) LevelDefinition.TYPE_TIME else LevelDefinition.TYPE_LINES

        // Variety rule: outside world 1's tutorial, two consecutive levels
        // NEVER share an objective type, and each world owns a distinct
        // mechanic flavor (worlds 2+ introduce CLEANUP, worlds 3+ SURVIVE).
        return when (world) {
            1 -> if (indexInWorld % 5 == 0) LevelDefinition.TYPE_LINES else LevelDefinition.TYPE_SCORE
            2 -> when {
                indexInWorld % 5 == 0 -> LevelDefinition.TYPE_CLEANUP   // cleanup intro
                indexInWorld % 2 == 0 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SCORE
            }
            3 -> when {
                indexInWorld % 5 == 0 -> LevelDefinition.TYPE_SURVIVE   // survive intro
                indexInWorld % 2 == 0 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SCORE
            }
            4 -> when (indexInWorld % 4) {
                0 -> LevelDefinition.TYPE_TIME
                1 -> LevelDefinition.TYPE_SCORE
                2 -> LevelDefinition.TYPE_CLEANUP
                else -> LevelDefinition.TYPE_LINES
            }
            5 -> when (indexInWorld % 4) {
                0 -> LevelDefinition.TYPE_COMBO
                1 -> LevelDefinition.TYPE_SCORE
                2 -> LevelDefinition.TYPE_SURVIVE
                else -> LevelDefinition.TYPE_LINES
            }
            6 -> when (indexInWorld % 5) {
                0 -> LevelDefinition.TYPE_CLEANUP
                1, 3 -> LevelDefinition.TYPE_SCORE
                2 -> LevelDefinition.TYPE_SURVIVE
                else -> LevelDefinition.TYPE_LINES
            }
            7 -> when (indexInWorld % 4) {
                0 -> LevelDefinition.TYPE_COMBO
                1 -> LevelDefinition.TYPE_CLEANUP
                2 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SCORE
            }
            8 -> when (indexInWorld % 3) {
                0 -> LevelDefinition.TYPE_TIME
                1 -> LevelDefinition.TYPE_CLEANUP
                else -> LevelDefinition.TYPE_COMBO
            }
            9 -> when (indexInWorld % 4) {
                0 -> LevelDefinition.TYPE_TIME
                1 -> LevelDefinition.TYPE_COMBO
                2 -> LevelDefinition.TYPE_SURVIVE
                else -> LevelDefinition.TYPE_LINES
            }
            else -> when (indexInWorld % 3) {
                0 -> LevelDefinition.TYPE_TIME
                1 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SURVIVE
            }
        }
    }
}
