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
    fun loadOwnedSkins(): String          // csv of owned skin ids, "" = first run
    fun saveOwnedSkins(csv: String)
    fun loadBoosters(): String            // "bomb|lightning|star|move" counts
    fun saveBoosters(csv: String)
    fun loadMusic(): Boolean
    fun saveMusic(enabled: Boolean)
    // v4.1: generic extension keys — streak, achievements, reduced motion
    fun loadExtraInt(key: String, def: Int): Int
    fun saveExtraInt(key: String, value: Int)
    fun loadExtraBool(key: String, def: Boolean): Boolean
    fun saveExtraBool(key: String, value: Boolean)
}

/** Progress helper over a SaveStore. */
class GameProgress(private val store: SaveStore) {

    companion object {
        // ── TESTING MODE (temporary): while the owner finalises QA every
        // store item is FREE. Set to false before commercial release to
        // restore the coin economy. TODO(owner): flip to false for prod.
        const val STORE_TEST_FREE = true
    }


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
            // toIntOrNull: a corrupted legacy string can never crash startup
            return MissionState(
                day,
                parts[1].toIntOrNull() ?: 0,
                parts[2].toIntOrNull() ?: 0,
                parts[3].toIntOrNull() ?: 0,
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

    /** Mission-claim coin grant — never negative, never throws. */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        store.saveCoins(store.loadCoins() + amount)
    }

    /** Atomic coin spend for boosters: false when not enough coins. */
    fun spendCoins(amount: Int): Boolean {
        val c = store.loadCoins()
        if (c < amount) return false
        store.saveCoins(c - amount)
        return true
    }

    fun isDailyDone(key: String): Boolean = store.loadDailyDone(key)
    fun markDailyDone(key: String) {
        if (!isDailyDone(key)) {
            store.saveDailyDone(key)
            store.saveExtraInt("daily_count", store.loadExtraInt("daily_count", 0) + 1)
        }
    }

    // ── v4.1: daily streak — consecutive play days, +100 coins every 7th ──
    fun refreshStreak(): Int {
        val today = (System.currentTimeMillis() / 86_400_000L).toInt()
        val last = store.loadExtraInt("streak_day", -1)
        val streak = when (today) {
            last -> store.loadExtraInt("streak_count", 0)
            last + 1 -> store.loadExtraInt("streak_count", 0) + 1
            else -> 1
        }
        store.saveExtraInt("streak_day", today)
        store.saveExtraInt("streak_count", streak)
        if (streak > 0 && streak % 7 == 0 && store.loadExtraInt("streak_bonus", -1) != today) {
            store.saveExtraInt("streak_bonus", today)
            addCoins(100)
        }
        return streak
    }
    fun streakCount(): Int = store.loadExtraInt("streak_count", 0)

    // ── v4.1: achievement data points ──
    fun recordBestCombo(combo: Int) {
        if (combo > store.loadExtraInt("best_combo", 0))
            store.saveExtraInt("best_combo", combo)
    }
    fun bestCombo(): Int = store.loadExtraInt("best_combo", 0)
    fun incBoostersUsed() =
        store.saveExtraInt("boosters_used", store.loadExtraInt("boosters_used", 0) + 1)
    fun boostersUsed(): Int = store.loadExtraInt("boosters_used", 0)
    fun dailyDoneCount(): Int = store.loadExtraInt("daily_count", 0)
    fun threeStarLevels(): Int = stars.values.count { it >= 3 }

    // ── Store: boosters + skins, in-game currency ONLY (no real money,
    // no stars — approved spec) ─────────────────────────────────────


    enum class BoosterKind { BOMB, LIGHTNING, STAR, MOVE }

    data class BoosterCounts(val bomb: Int, val lightning: Int, val star: Int, val move: Int)

    /** Prices in coins — one source of truth for the store screen. */
    fun boosterPrice(kind: BoosterKind): Int = if (STORE_TEST_FREE) 0 else when (kind) {
        BoosterKind.BOMB -> 150
        BoosterKind.LIGHTNING -> 150
        BoosterKind.STAR -> 200
        BoosterKind.MOVE -> 100
    }

    fun skinPrice(skinId: Int): Int = if (STORE_TEST_FREE || skinId <= 0) 0 else (200 + skinId * 10)

    fun boosters(): BoosterCounts {
        ensureStarterKit()
        val raw = store.loadBoosters()
        val parts = raw.split('|')
        fun at(i: Int) = parts.getOrNull(i)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return BoosterCounts(at(0), at(1), at(2), at(3))
    }

    private fun saveBoosters(c: BoosterCounts) =
        store.saveBoosters("${c.bomb}|${c.lightning}|${c.star}|${c.move}")

    fun buyBooster(kind: BoosterKind): Boolean {
        if (!spendCoins(boosterPrice(kind))) return false
        val c = boosters()
        saveBoosters(
            when (kind) {
                BoosterKind.BOMB -> c.copy(bomb = c.bomb + 1)
                BoosterKind.LIGHTNING -> c.copy(lightning = c.lightning + 1)
                BoosterKind.STAR -> c.copy(star = c.star + 1)
                BoosterKind.MOVE -> c.copy(move = c.move + 1)
            }
        )
        return true
    }

    /** Atomic consume for in-game use: false when none left. */
    fun spendBooster(kind: BoosterKind): Boolean {
        val c = boosters()
        val next = when (kind) {
            BoosterKind.BOMB -> if (c.bomb <= 0) null else c.copy(bomb = c.bomb - 1)
            BoosterKind.LIGHTNING -> if (c.lightning <= 0) null else c.copy(lightning = c.lightning - 1)
            BoosterKind.STAR -> if (c.star <= 0) null else c.copy(star = c.star - 1)
            BoosterKind.MOVE -> if (c.move <= 0) null else c.copy(move = c.move - 1)
        } ?: return false
        saveBoosters(next)
        return true
    }

    fun ownedSkins(): Set<Int> {
        ensureStarterKit()
        return store.loadOwnedSkins().split(',').mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun skinOwned(skinId: Int): Boolean = skinId in ownedSkins()

    /** Buy + equip in one tap, exactly as the approved store shows. */
    fun buySkin(skinId: Int): Boolean {
        if (skinOwned(skinId)) return true
        if (!spendCoins(skinPrice(skinId))) return false
        store.saveOwnedSkins(store.loadOwnedSkins().let {
            if (it.isBlank()) "$skinId" else "$it,$skinId"
        })
        return true
    }

    /** Buy with an explicit price (material skins carry their own catalog price). */
    fun buySkinAt(skinId: Int, price: Int): Boolean {
        if (skinOwned(skinId)) return true
        if (!spendCoins(if (STORE_TEST_FREE) 0 else price)) return false
        store.saveOwnedSkins(store.loadOwnedSkins().let {
            if (it.isBlank()) "$skinId" else "$it,$skinId"
        })
        return true
    }

    /** First run: default skin owned + a small starter booster kit. */
    private fun ensureStarterKit() {
        val raw = store.loadOwnedSkins()
        if (raw.isNotBlank()) return
        store.saveOwnedSkins("0")
        // Owner spec (QA): exactly 3 free uses of every booster for the
        // ENTIRE game, never refilled per level. More must be bought with
        // coins (free while STORE_TEST_FREE).
        saveBoosters(BoosterCounts(bomb = 3, lightning = 3, star = 3, move = 3))
    }
}
