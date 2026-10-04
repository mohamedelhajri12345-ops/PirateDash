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
                    assertTrue(l.maxMoves >= 20)
                }
                LevelDefinition.TYPE_LINES -> {
                    assertTrue("lines ${l.targetLines} id ${l.id}", l.targetLines in 1..12)
                    assertTrue(l.maxMoves >= 15)
                    // a line every ~3 moves must always be possible
                    assertTrue(l.maxMoves >= l.targetLines * 3)
                }
                LevelDefinition.TYPE_TIME -> {
                    assertTrue(l.targetScore > 0)
                    assertTrue(l.timeLimit >= 90)
                }
                LevelDefinition.TYPE_COMBO -> {
                    assertTrue(l.targetCombo in 2..5)
                    assertTrue(l.maxMoves >= 20)
                }
                LevelDefinition.TYPE_SURVIVE -> {
                    assertTrue("survive ${l.targetCount} id ${l.id}", l.targetCount in 12..24)
                    assertTrue(l.maxMoves == l.targetCount)
                }
                LevelDefinition.TYPE_CLEANUP -> {
                    assertTrue("cleanup ${l.targetCount} id ${l.id}", l.targetCount in 6..24)
                    assertTrue(l.maxMoves >= l.targetCount * 2)
                    // dirt on the board must cover the objective with margin
                    assertTrue(l.prefillDensity >= l.targetCount)
                    assertTrue(l.prefillPattern in 0..5)
                }
                else -> throw AssertionError("bad objective type ${l.objectiveType}")
            }
            assertTrue(l.prefillDensity in 0..40)
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
    fun gentleStartIsAWalkthrough() {
        // the very first levels must be finishable by pure placement
        val l1 = LevelCatalog.getLevel(1)
        assertTrue(l1.target() <= 120)
        assertTrue(l1.maxMoves >= 20)
        val l2 = LevelCatalog.getLevel(2)
        assertTrue(l2.target() <= 130)
    }

    @Test
    fun scoreTargetsStayHumanlyReachable() {
        // sustained 14+ pts/move is beyond a good human: cap the curve
        for (id in 1..1000 step 7) {
            val l = LevelCatalog.getLevel(id)
            if (l.objectiveType == LevelDefinition.TYPE_SCORE || l.objectiveType == LevelDefinition.TYPE_TIME) {
                if (l.maxMoves > 0) {
                    val perMove = l.targetScore.toFloat() / l.maxMoves
                    assertTrue("id $id needs $perMove pts/move", perMove <= 10.5f)
                }
            }
        }
    }

    @Test
    fun tutorialArcIsHandcraftedAndTeachesInOrder() {
        // level 1: score by placement; level 3: the first single-line lesson
        assertEquals(LevelDefinition.TYPE_SCORE, LevelCatalog.getLevel(1).objectiveType)
        assertEquals(60, LevelCatalog.getLevel(1).targetScore)
        assertEquals(25, LevelCatalog.getLevel(1).maxMoves)
        assertEquals(LevelDefinition.TYPE_LINES, LevelCatalog.getLevel(3).objectiveType)
        assertEquals(1, LevelCatalog.getLevel(3).targetLines)
        // world 1 tutorial: no prefill, no time pressure, 5% specials
        for (id in 1..10) {
            val l = LevelCatalog.getLevel(id)
            assertEquals(0, l.prefillDensity)
            assertEquals(0, l.timeLimit)
            assertEquals(5, l.specialChance)
            assertTrue(l.maxMoves >= 22)
        }
        // level 11 continues the generated curve seamlessly
        assertTrue(LevelCatalog.getLevel(11).maxMoves >= 20)
    }

    @Test
    fun skinPersistRoundTrips() {
        val store = FakeStore()
        val progress = GameProgress(store)
        assertEquals(0, progress.loadSkin())
        progress.saveSkin(3)
        assertEquals(3, progress.loadSkin())
    }

    @Test
    fun adjacentLevelsNeverRepeatObjectiveOutsideWorld1() {
        // the variety rule: two consecutive levels in the same world (that
        // are not both the boss) never share an objective type.
        // World 1's tutorial deliberately repeats SCORE runs — it starts
        // at world 2.
        for (id in 101..999) {
            val a = LevelCatalog.getLevel(id)
            val b = LevelCatalog.getLevel(id + 1)
            if (a.world == b.world) {
                assertTrue(
                    "id $id -> ${id + 1} repeats type ${a.objectiveType}",
                    a.objectiveType != b.objectiveType || b.indexInWorld == 100
                )
            }
        }
    }

    @Test
    fun cleanupAndSurviveExistAndAreWinnableByDesign() {
        var cleanup = 0; var survive = 0
        for (id in 1..1000) {
            when (LevelCatalog.getLevel(id).objectiveType) {
                LevelDefinition.TYPE_CLEANUP -> cleanup++
                LevelDefinition.TYPE_SURVIVE -> survive++
            }
        }
        assertTrue("cleanup count $cleanup", cleanup >= 60)
        assertTrue("survive count $survive", survive >= 50)
    }

    @Test
    fun prefillPatternsRotateDeterministically() {
        val a = LevelCatalog.getLevel(415)
        val b = LevelCatalog.getLevel(415)
        assertEquals(a.prefillPattern, b.prefillPattern)
        // pattern changes across level bands (id/10) — variety is real
        assertTrue(
            (1..50).map { LevelCatalog.getLevel(400 + it).prefillPattern }.distinct().size >= 4
        )
    }

    @Test
    fun dailyMissionsTrackAndClaim() {
        val store = FakeStore()
        val progress = GameProgress(store)
        val state = progress.dailyMissionState()
        val missions = DailyMissions.forDay(state.day)
        assertEquals(3, missions.size)
        assertEquals(0, progress.missionValue(state, 0))

        progress.trackDailyMission(lines = 3, score = 120, levelDone = false)
        val mid = progress.dailyMissionState()
        assertEquals(3, mid.lines)
        assertEquals(120, mid.score)

        // claim before reaching the target is refused
        assertFalse(progress.claimDailyMission(1))

        // finish the lines mission and claim it once
        progress.trackDailyMission(lines = 200, score = 0, levelDone = true)
        val before = progress.coins
        assertTrue(progress.claimDailyMission(0))
        assertEquals(before + missions[0].reward, progress.coins)
        // double claim blocked
        assertFalse(progress.claimDailyMission(0))
        // level completion counted
        assertEquals(1, progress.dailyMissionState().levels)
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
        private var skin = 0
        override fun loadSkin() = skin
        override fun saveSkin(id: Int) { skin = id }
        private var missions = ""
        override fun loadMissions() = missions
        override fun saveMissions(state: String) { missions = state }
    }
}