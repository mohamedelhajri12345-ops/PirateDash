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
        val raw = prefs.getString(KEY_STARS, "") ?: ""
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

    override fun loadCoins() = prefs.getInt(KEY_COINS, 0)
    override fun saveCoins(coins: Int) = prefs.edit().putInt(KEY_COINS, coins).apply()

    override fun loadBestEndless() = prefs.getInt(KEY_BEST, 0)
    override fun saveBestEndless(score: Int) = prefs.edit().putInt(KEY_BEST, score).apply()

    override fun loadDailyDone(key: String) = prefs.getBoolean("daily_$key", false)
    override fun saveDailyDone(key: String) = prefs.edit().putBoolean("daily_$key", true).apply()

    override fun loadSound() = prefs.getBoolean(KEY_SOUND, true)
    override fun saveSound(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND, enabled).apply()

    override fun loadHaptics() = prefs.getBoolean(KEY_HAPTICS, true)
    override fun saveHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()

    companion object {
        private const val KEY_STARS = "stars"
        private const val KEY_COINS = "coins"
        private const val KEY_BEST = "best_endless"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
    }
}
