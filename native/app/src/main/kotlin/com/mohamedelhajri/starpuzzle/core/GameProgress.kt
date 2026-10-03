package com.mohamedelhajri.starpuzzle.core

/**
 * Persistence contract. The Android app implements it with SharedPreferences;
 * unit tests use an in-memory fake. Pure Kotlin.
 */
interface SaveStore {
    fun loadStars(): MutableMap<Int, Int>          // levelId -> stars (0..3)
    fun saveStars(stars: Map<Int, Int>)
    fun loadCoins(): Int
    fun saveCoins(coins: Int)
    fun loadBestEndless(): Int
    fun saveBestEndless(score: Int)
    fun loadDailyDone(key: String): Boolean
    fun saveDailyDone(key: String)
    fun loadSound(): Boolean
    fun saveSound(enabled: Boolean)
    fun loadHaptics(): Boolean
    fun saveHaptics(enabled: Boolean)
}

/** Progress helper over a SaveStore. */
class GameProgress(private val store: SaveStore) {

    val stars: MutableMap<Int, Int> get() = store.loadStars()
    val coins: Int get() = store.loadCoins()

    fun starsForLevel(id: Int): Int = stars[id] ?: 0

    fun totalStars(): Int = stars.values.sum()

    fun worldUnlocked(world: Int): Boolean =
        totalStars() >= LevelCatalog.worldUnlockStars(world)

    fun firstUnfinished(): Int = LevelCatalog.firstUnfinishedLevel(stars)

    fun recordLevelResult(id: Int, stars: Int, rewardCoins: Int): Boolean {
        val current = stars
        val previous = current[id] ?: 0
        if (stars <= previous) return false
        current[id] = stars
        store.saveStars(current)
        store.saveCoins(store.loadCoins() + rewardCoins)
        return true
    }

    fun isDailyDone(key: String): Boolean = store.loadDailyDone(key)
    fun markDailyDone(key: String) = store.saveDailyDone(key)
}
