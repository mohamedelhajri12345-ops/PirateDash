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
        "Ember Volcano", "Deep Forest", "Neon City", "Candy Wonderland",
        "Cosmic Space", "Mystery Realm"
    )

    private val WORLD_UNLOCK_STARS = intArrayOf(0, 30, 70, 120, 170, 220, 270, 320, 370, 420)

    private const val SEED_SALT = 53701221L // "STARPUZZLE" flavor

    fun worldName(world: Int): String = WORLD_NAMES[world - 1]

    fun worldUnlockStars(world: Int): Int = WORLD_UNLOCK_STARS[world - 1]

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
            if (world < 3) 0
            else minOf(4 + world + (t * 6).toInt(), 30)

        val specialChance = if (world >= 7) 10 + (world - 7) * 2 else 5

        val rewardCoins = 10 + id / 20 + (if (indexInWorld == 100) 40 else 0)

        return when (objectiveType) {
            LevelDefinition.TYPE_LINES -> {
                val targetLines = 3 + (t * 6).toInt() + world + random.nextInt(3)
                val maxMoves = (targetLines * 1.7f).toInt() + 8
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, targetLines, 0, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            LevelDefinition.TYPE_TIME -> {
                val timeLimit = if (world >= 8) 60 + random.nextInt(30) else 90 + random.nextInt(60)
                val targetScore = Math.round(timeLimit * (10f + w * 1.2f + t * 4f))
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    targetScore, 0, 0, 0, timeLimit,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            LevelDefinition.TYPE_COMBO -> {
                val targetCombo = minOf(
                    3 + (w * 0.4f).toInt() + (if (t > 0.6f) 1 else 0) + (if (random.nextBoolean()) 1 else 0),
                    7
                )
                val maxMoves = 12 + random.nextInt(6) + world
                LevelDefinition(id, world, indexInWorld, objectiveType,
                    0, 0, targetCombo, maxMoves, 0,
                    prefillDensity, random.nextLong(), specialChance, rewardCoins)
            }
            else -> { // TYPE_SCORE
                val maxMoves = 14 + world + (t * 8).toInt() + random.nextInt(5)
                val targetScore = Math.round(maxMoves * (20f + w * 2.2f + t * 6f) * 0.92f)
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
            9 -> if (indexInWorld % 2 == 0) LevelDefinition.TYPE_COMBO
            else -> when {
                indexInWorld % 3 == 1 -> LevelDefinition.TYPE_TIME
                indexInWorld % 3 == 2 -> LevelDefinition.TYPE_LINES
                else -> random.nextInt(4)
            }
        }
    }
}
