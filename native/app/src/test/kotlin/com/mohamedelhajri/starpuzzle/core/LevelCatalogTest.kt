package com.mohamedelhajri.starpuzzle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Level catalog tests: 1000 levels, determinism, sane targets, star gates. */
class LevelCatalogTest {

    @Test
    fun totalLevelCounts() {
        assertEquals(1000, LevelCatalog.TOTAL_LEVELS)
        assertEquals(10, LevelCatalog.TOTAL_WORLDS)
        assertEquals(100, LevelCatalog.LEVELS_PER_WORLD)
    }

    @Test
    fun levelsAreDeterministic() {
        for (id in listOf(1, 42, 100, 777, 1000)) {
            val a = LevelCatalog.getLevel(id)
            val b = LevelCatalog.getLevel(id)
            assertEquals(a, b)
            assertEquals(id, a.id)
        }
    }

    @Test
    fun everyLevelIsValid() {
        for (id in 1..1000) {
            val l = LevelCatalog.getLevel(id)
            assertEquals(id, l.id)
            assertTrue(l.world in 1..10)
            assertTrue(l.indexInWorld in 1..100)
            assertTrue(l.rewardCoins >= 10)
            when (l.objectiveType) {
                LevelDefinition.TYPE_SCORE -> {
                    assertTrue(l.targetScore > 0)
                    assertTrue(l.maxMoves >= 14)
                }
                LevelDefinition.TYPE_LINES -> {
                    assertTrue(l.targetLines in 3..21)
                    assertTrue(l.maxMoves > 0)
                }
                LevelDefinition.TYPE_TIME -> {
                    assertTrue(l.targetScore > 0)
                    assertTrue(l.timeLimit >= 60)
                }
                LevelDefinition.TYPE_COMBO -> {
                    assertTrue(l.targetCombo in 3..7)
                    assertTrue(l.maxMoves > 0)
                }
                else -> throw AssertionError("bad objective type ${l.objectiveType}")
            }
            assertTrue(l.prefillDensity in 0..30)
            assertTrue(l.specialChance in 5..16)
        }
    }

    @Test
    fun worldDistributionIsCorrect() {
        assertEquals(1, LevelCatalog.getLevel(1).world)
        assertEquals(1, LevelCatalog.getLevel(100).world)
        assertEquals(2, LevelCatalog.getLevel(101).world)
        assertEquals(10, LevelCatalog.getLevel(1000).world)
        assertTrue(LevelCatalog.getLevel(100).isBoss)
    }

    @Test
    fun worldStarGatesAreMonotonicAndReachable() {
        var prev = 0
        for (w in 1..10) {
            val gate = LevelCatalog.worldUnlockStars(w)
            assertTrue(gate >= prev)
            assertTrue(gate <= 300 * (w - 1)) // reachable with all stars from previous worlds
            prev = gate
        }
    }

    @Test
    fun firstUnfinishedLevelRespectsStars() {
        val empty = mutableMapOf<Int, Int>()
        assertEquals(1, LevelCatalog.firstUnfinishedLevel(empty))
        empty[1] = 3
        empty[2] = 1
        assertEquals(3, LevelCatalog.firstUnfinishedLevel(empty))
        for (id in 1..1000) empty[id] = 3
        assertEquals(1000, LevelCatalog.firstUnfinishedLevel(empty))
    }

    @Test
    fun starsFollowPerformance() {
        val l = LevelCatalog.getLevel(1)
        assertEquals(3, l.starsFor(0.5f))
        assertEquals(2, l.starsFor(0.25f))
        assertEquals(1, l.starsFor(0.1f))
    }

    @Test
    fun progressRecordsBestStarsAndGrantsCoinsOnce() {
        val store = FakeStore()
        val progress = GameProgress(store)
        assertTrue(progress.recordLevelResult(1, 3, 50))
        assertEquals(3, progress.starsForLevel(1))
        assertEquals(50, progress.coins)
        // replay with same or worse stars: no duplicate reward
        assertFalse(progress.recordLevelResult(1, 2, 50))
        assertEquals(50, progress.coins)
        assertFalse(progress.recordLevelResult(1, 3, 50)) // same stars, not an improvement
    }

    @Test
    fun worldUnlockFollowsStars() {
        val store = FakeStore()
        val progress = GameProgress(store)
        assertTrue(progress.worldUnlocked(1))
        assertFalse(progress.worldUnlocked(2))
        store.savedStars[1] = 3
        for (id in 1..15) store.savedStars[id] = 2
        assertTrue(progress.worldUnlocked(2))
    }

    @Test
    fun spendCoinsIsAtomicAndSafe() {
        val store = FakeStore()
        val progress = GameProgress(store)
        store.saveCoins(20)
        assertTrue(progress.spendCoins(15))
        assertEquals(5, progress.coins)
        assertFalse(progress.spendCoins(6))
        assertEquals(5, progress.coins)
    }

    class FakeStore : SaveStore {
        val savedStars = mutableMapOf<Int, Int>()
        var coins = 0
        override fun loadStars(): MutableMap<Int, Int> = savedStars.toMutableMap()
        override fun saveStars(stars: Map<Int, Int>) { savedStars.putAll(stars) }
        override fun loadCoins(): Int = coins
        override fun saveCoins(c: Int) { coins = c }
        override fun loadBestEndless() = 0
        override fun saveBestEndless(score: Int) {}
        override fun loadDailyDone(key: String) = false
        override fun saveDailyDone(key: String) {}
        override fun loadSound() = true
        override fun saveSound(enabled: Boolean) {}
        override fun loadHaptics() = true
        override fun saveHaptics(enabled: Boolean) {}
    }
}