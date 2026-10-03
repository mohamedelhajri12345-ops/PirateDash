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
    fun loadSkin(): Int
    fun saveSkin(id: Int)
    fun loadMissions(): String
    fun saveMissions(state: String)
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

    fun recordLevelResult(id: Int, starsEarned: Int, rewardCoins: Int): Boolean {
        val current = store.loadStars()
        val previous = current[id] ?: 0
        if (starsEarned <= previous) return false
        current[id] = starsEarned
        store.saveStars(current)
        store.saveCoins(store.loadCoins() + rewardCoins)
        return true
    }

    fun loadSkin(): Int = store.loadSkin()
    fun saveSkin(id: Int) = store.saveSkin(id)

    // ── Daily missions ──────────────────────────────────────────────
    // Storage format: "day|lines|score|levels|claim0|claim1|claim2"

    data class MissionState(
        val day: String,
        val lines: Int, val score: Int, val levels: Int,
        val claimed: BooleanArray
    )

    fun dailyMissionState(): MissionState {
        val day = LevelCatalog.dailyKey()
        val raw = store.loadMissions()
        val parts = raw.split('|')
        if (parts.size == 7 && parts[0] == day) {
            return MissionState(
                day, parts[1].toInt(), parts[2].toInt(), parts[3].toInt(),
                booleanArrayOf(parts[4] == "1", parts[5] == "1", parts[6] == "1")
            )
        }
        return MissionState(day, 0, 0, 0, booleanArrayOf(false, false, false))
    }

    /** Feed the day's counters after every placement and level completion. */
    fun trackDailyMission(lines: Int, score: Int, levelDone: Boolean) {
        val st = dailyMissionState()
        store.saveMissions(
            "${st.day}|${st.lines + lines}|${st.score + score}|" +
                    "${st.levels + if (levelDone) 1 else 0}|" +
                    "${if (st.claimed[0]) 1 else 0}|" +
                    "${if (st.claimed[1]) 1 else 0}|" +
                    "${if (st.claimed[2]) 1 else 0}"
        )
    }

    fun missionValue(state: MissionState, index: Int): Int = when (index) {
        0 -> state.lines
        1 -> state.score
        else -> state.levels
    }

    /** Claim the finished mission's coin reward. False if not earned yet. */
    fun claimDailyMission(index: Int): Boolean {
        val st = dailyMissionState()
        val def = DailyMissions.forDay(st.day).getOrNull(index) ?: return false
        if (st.claimed[index]) return false
        if (missionValue(st, index) < def.target) return false
        addCoins(def.reward)
        val claimed = st.claimed.copyOf().also { it[index] = true }
        store.saveMissions(
            "${st.day}|${st.lines}|${st.score}|${st.levels}|" +
                    claimed.joinToString("|") { if (it) "1" else "0" }
        )
        return true
    }

    /** Atomic coin spend for boosters: false when not enough coins. */
    /** Mission/mission-claim coin grant — never negative, never throws. */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        store.saveCoins(store.loadCoins() + amount)
    }

    fun spendCoins(amount: Int): Boolean {
        val c = store.loadCoins()
        if (c < amount) return false
        store.saveCoins(c - amount)
        return true
    }

    fun isDailyDone(key: String): Boolean = store.loadDailyDone(key)
    fun markDailyDone(key: String) = store.saveDailyDone(key)
}
