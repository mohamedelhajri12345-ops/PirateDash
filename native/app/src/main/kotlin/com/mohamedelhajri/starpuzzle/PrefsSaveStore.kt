package com.mohamedelhajri.starpuzzle

import android.content.Context
import android.content.SharedPreferences
import com.mohamedelhajri.starpuzzle.core.SaveStore

/**
 * SharedPreferences-backed save store — v5.2 "backend" upgrade.
 *
 * Previous behavior: every game-frame operation (stars sum in the HUD,
 * mission tracking after every placement) re-read and re-parsed raw
 * strings from SharedPreferences on the main thread. With a 1000-level
 * star map that is thousands of string splits per second of play.
 *
 * New architecture (pattern adapted from modern DataStore-based games,
 * with zero new dependencies and no schema change):
 *  1. One in-memory snapshot, loaded lazily on first access.
 *  2. All reads hit the snapshot — parsing happens exactly once.
 *  3. All writes update the snapshot then flush asynchronously
 *     (apply() = background disk write, never blocking the UI thread).
 *  4. The two most valuable keys (stars, coins) carry a redundant
 *     backup copy; a corrupted primary is auto-restored from backup.
 *  5. Corrupt values of ANY key fall back to the default instead of
 *     crashing at startup (kept from v4.1).
 */
class PrefsSaveStore(context: Context) : SaveStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starpuzzle_v2", Context.MODE_PRIVATE)

    // ── one-time snapshot (lazy) ─────────────────────────────────
    private var starsCache: MutableMap<Int, Int>? = null
    private var coinsCache: Int? = null
    private var bestCache: Int? = null
    private var missionsCache: String? = null
    private var skinCache: Int? = null
    private var soundCache: Boolean? = null
    private var hapticsCache: Boolean? = null
    private var musicCache: Boolean? = null
    private var ownedSkinsCache: String? = null
    private var boostersCache: String? = null
    private val extraIntCache = HashMap<String, Int>()
    private val extraBoolCache = HashMap<String, Boolean>()

    // Stars format: "id=stars;id=stars;..." compact and human-debuggable
    override fun loadStars(): MutableMap<Int, Int> {
        starsCache?.let { return it }
        var map = parseStars(safeString(KEY_STARS, ""))
        // primary empty/corrupt but a backup survived → restore
        if (map.isEmpty()) {
            val bak = parseStars(safeString(KEY_STARS_BAK, ""))
            if (bak.isNotEmpty()) {
                map = bak
                prefs.edit().putString(KEY_STARS, serializeStars(map)).apply()
            }
        }
        val result = map
        starsCache = result
        return result
    }

    private fun parseStars(raw: String): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        if (raw.isNotEmpty()) {
            for (entry in raw.split(';')) {
                val parts = entry.split('=')
                if (parts.size == 2) {
                    val id = parts[0].toIntOrNull()
                    val stars = parts[1].toIntOrNull()?.coerceIn(0, 3)
                    if (id != null && stars != null) map[id] = stars
                }
            }
        }
        return map
    }

    private fun serializeStars(stars: Map<Int, Int>): String =
        stars.entries.joinToString(";") { "${it.key}=${it.value}" }

    override fun saveStars(stars: Map<Int, Int>) {
        starsCache = stars.toMutableMap()
        val raw = serializeStars(stars)
        // primary + redundant backup: a torn write can never lose progress
        prefs.edit().putString(KEY_STARS, raw).putString(KEY_STARS_BAK, raw).apply()
    }

    override fun loadCoins(): Int {
        coinsCache?.let { return it }
        var c = safeInt(KEY_COINS, 0)
        if (c < 0) {  // corrupt primary → try the backup before resetting
            val bak = safeInt(KEY_COINS_BAK, 0)
            if (bak >= 0) {
                c = bak
                prefs.edit().putInt(KEY_COINS, bak).apply()
            }
        }
        coinsCache = c
        return c
    }

    override fun saveCoins(coins: Int) {
        val clamped = coins.coerceAtLeast(0)
        coinsCache = clamped
        prefs.edit().putInt(KEY_COINS, clamped).putInt(KEY_COINS_BAK, clamped).apply()
    }

    override fun loadBestEndless(): Int {
        bestCache?.let { return it }
        val v = safeInt(KEY_BEST, 0)
        bestCache = v
        return v
    }

    override fun saveBestEndless(score: Int) {
        val v = score.coerceAtLeast(0)
        bestCache = v
        prefs.edit().putInt(KEY_BEST, v).apply()
    }

    override fun loadDailyDone(key: String) = safeBool("daily_$key", false)
    override fun saveDailyDone(key: String) =
        prefs.edit().putBoolean("daily_$key", true).apply()

    override fun loadMissions(): String {
        missionsCache?.let { return it }
        val v = safeString(KEY_MISSIONS, "")
        missionsCache = v
        return v
    }

    override fun saveMissions(state: String) {
        missionsCache = state
        prefs.edit().putString(KEY_MISSIONS, state).apply()
    }

    override fun loadSkin(): Int {
        skinCache?.let { return it }
        val v = safeInt("skin", 0)
        skinCache = v
        return v
    }

    override fun saveSkin(id: Int) {
        skinCache = id
        prefs.edit().putInt("skin", id).apply()
    }

    override fun loadSound(): Boolean {
        soundCache?.let { return it }
        val v = safeBool(KEY_SOUND, true)
        soundCache = v
        return v
    }

    override fun saveSound(enabled: Boolean) {
        soundCache = enabled
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    override fun loadHaptics(): Boolean {
        hapticsCache?.let { return it }
        val v = safeBool(KEY_HAPTICS, true)
        hapticsCache = v
        return v
    }

    override fun saveHaptics(enabled: Boolean) {
        hapticsCache = enabled
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    override fun loadOwnedSkins(): String {
        ownedSkinsCache?.let { return it }
        val v = safeString(KEY_OWNED_SKINS, "")
        ownedSkinsCache = v
        return v
    }

    override fun saveOwnedSkins(csv: String) {
        ownedSkinsCache = csv
        prefs.edit().putString(KEY_OWNED_SKINS, csv).apply()
    }

    override fun loadBoosters(): String {
        boostersCache?.let { return it }
        val v = safeString(KEY_BOOSTERS, "")
        boostersCache = v
        return v
    }

    override fun saveBoosters(csv: String) {
        boostersCache = csv
        prefs.edit().putString(KEY_BOOSTERS, csv).apply()
    }

    override fun loadMusic(): Boolean {
        musicCache?.let { return it }
        val v = safeBool(KEY_MUSIC, true)
        musicCache = v
        return v
    }

    override fun saveMusic(enabled: Boolean) {
        musicCache = enabled
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
    }

    // ── v4.1: generic extension-key storage (now cached) ──
    override fun loadExtraInt(key: String, def: Int): Int {
        extraIntCache[key]?.let { return it }
        val v = try { prefs.getInt(key, def) } catch (e: Exception) { def }
        extraIntCache[key] = v
        return v
    }

    override fun saveExtraInt(key: String, value: Int) {
        extraIntCache[key] = value
        prefs.edit().putInt(key, value).apply()
    }

    override fun loadExtraBool(key: String, def: Boolean): Boolean {
        extraBoolCache[key]?.let { return it }
        val v = try { prefs.getBoolean(key, def) } catch (e: Exception) { def }
        extraBoolCache[key] = v
        return v
    }

    override fun saveExtraBool(key: String, value: Boolean) {
        extraBoolCache[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }

    // A value stored as a different type by an older install must never
    // crash the app at startup — fall back to the default and clear it.
    private fun safeInt(key: String, def: Int): Int = try {
        prefs.getInt(key, def)
    } catch (e: Exception) {
        prefs.edit().remove(key).apply()
        def
    }

    private fun safeBool(key: String, def: Boolean): Boolean = try {
        prefs.getBoolean(key, def)
    } catch (e: Exception) {
        prefs.edit().remove(key).apply()
        def
    }

    private fun safeString(key: String, def: String): String = try {
        prefs.getString(key, def) ?: def
    } catch (e: Exception) {
        prefs.edit().remove(key).apply()
        def
    }

    companion object {
        private const val KEY_STARS = "stars"
        private const val KEY_STARS_BAK = "stars_bak"
        private const val KEY_COINS = "coins"
        private const val KEY_COINS_BAK = "coins_bak"
        private const val KEY_BEST = "best_endless"
        private const val KEY_MISSIONS = "missions"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_OWNED_SKINS = "owned_skins"
        private const val KEY_BOOSTERS = "boosters"
        private const val KEY_MUSIC = "music"
    }
}
