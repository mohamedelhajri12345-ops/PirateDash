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

    /** Build a level from its id. Pure and deterministic. */
    fun getLevel(id: Int): LevelDefinition {
        require(id in 1..TOTAL_LEVELS) { "level id out of range: $id" }

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
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            LevelDefinition.TYPE_TIME -> {
                val timeLimit = 90 + random.nextInt(45)
                // ~1.5-2s per move; sustained score of a decent player is
                // roughly (2 + 0.35w + 0.8t) points per second
                val targetScore = Math.round(timeLimit * (1.6f + w * 0.22f + t * 0.35f))
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    targetScore, 0, 0, 0, timeLimit,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            LevelDefinition.TYPE_COMBO -> {
                val targetCombo = (2 + w / 3).toInt().coerceIn(2, 5)
                val maxMoves = 20 + world + random.nextInt(4)
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, 0, targetCombo, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            else -> { // TYPE_SCORE
                val maxMoves = 20 + world + (t * 8).toInt() + random.nextInt(4)
                // Human sustained rate: ~7-8 pts/move for a decent player.
                // W1 ~3.2 (pure placement finishes it) rising to ~10 in
                // world 10 (needs regular clears and combos — boss tier)
                val targetScore = Math.round(maxMoves * (3.2f + w * 0.6f + t * 1.2f))
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    targetScore, 0, 0, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
        }
    }

    private fun pickObjective(random: Random, world: Int, indexInWorld: Int): Int {
        if (indexInWorld == 100)
            return if (world >= 8) LevelDefinition.TYPE_TIME else LevelDefinition.TYPE_LINES

        return when (world) {
            1 -> if (indexInWorld % 5 == 0) LevelDefinition.TYPE_LINES else LevelDefinition.TYPE_SCORE
            2 -> if (indexInWorld % 4 <= 1) LevelDefinition.TYPE_LINES else LevelDefinition.TYPE_SCORE
            3 -> if (indexInWorld % 3 == 0) LevelDefinition.TYPE_LINES else LevelDefinition.TYPE_SCORE
            4 -> when {
                indexInWorld % 3 == 0 -> LevelDefinition.TYPE_TIME
                indexInWorld % 3 == 1 -> LevelDefinition.TYPE_SCORE
                else -> LevelDefinition.TYPE_LINES
            }
            5 -> when {
                indexInWorld % 4 == 0 -> LevelDefinition.TYPE_COMBO
                indexInWorld % 2 == 0 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SCORE
            }
            6 -> if (indexInWorld % 3 == 0) LevelDefinition.TYPE_SCORE else LevelDefinition.TYPE_LINES
            7 -> when {
                indexInWorld % 4 == 1 -> LevelDefinition.TYPE_COMBO
                indexInWorld % 2 == 0 -> LevelDefinition.TYPE_LINES
                else -> LevelDefinition.TYPE_SCORE
            }
            8 -> if (indexInWorld % 3 != 2) LevelDefinition.TYPE_TIME else LevelDefinition.TYPE_COMBO
            9 -> when {
                indexInWorld % 2 == 0 -> LevelDefinition.TYPE_COMBO
                indexInWorld % 3 == 1 -> LevelDefinition.TYPE_TIME
                else -> LevelDefinition.TYPE_LINES
            }
            else -> when {
                indexInWorld % 3 == 1 -> LevelDefinition.TYPE_TIME
                indexInWorld % 3 == 2 -> LevelDefinition.TYPE_LINES
                else -> random.nextInt(4)
            }
        }
    }
}
