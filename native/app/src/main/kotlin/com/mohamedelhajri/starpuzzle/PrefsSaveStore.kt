package com.mohamedelhajri.starpuzzle

import android.content.Context
import android.content.SharedPreferences
import com.mohamedelhajri.starpuzzle.core.SaveStore

/** SharedPreferences-backed save store. */
class PrefsSaveStore(context: Context) : SaveStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starpuzzle_v2", Context.MODE_PRIVATE)

    // Stars format: "id=stars;id=stars;..." compact and human-debuggable
    override fun loadStars(): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val raw = safeString(KEY_STARS, "")
        if (raw.isNotEmpty()) {
            for (entry in raw.split(';')) {
                val parts = entry.split('=')
                if (parts.size == 2) {
                    val id = parts[0].toIntOrNull()
                    val stars = parts[1].toIntOrNull()
                    if (id != null && stars != null) map[id] = stars
                }
            }
        }
        return map
    }

    override fun saveStars(stars: Map<Int, Int>) {
        val raw = stars.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString(KEY_STARS, raw).apply()
    }

    override fun loadCoins() = safeInt(KEY_COINS, 0)
    override fun saveCoins(coins: Int) = prefs.edit().putInt(KEY_COINS, coins).apply()

    override fun loadBestEndless() = safeInt(KEY_BEST, 0)
    override fun saveBestEndless(score: Int) = prefs.edit().putInt(KEY_BEST, score).apply()

    override fun loadDailyDone(key: String) = safeBool("daily_$key", false)
    override fun saveDailyDone(key: String) = prefs.edit().putBoolean("daily_$key", true).apply()

    override fun loadSkin() = safeInt("skin", 0)
    override fun saveSkin(id: Int) = prefs.edit().putInt("skin", id).apply()

    override fun loadSound() = safeBool(KEY_SOUND, true)
    override fun saveSound(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND, enabled).apply()

    override fun loadHaptics() = safeBool(KEY_HAPTICS, true)
    override fun saveHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()

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
        private const val KEY_COINS = "coins"
        private const val KEY_BEST = "best_endless"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
    }
}
