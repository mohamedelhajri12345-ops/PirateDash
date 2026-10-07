package com.mohamedelhajri.starpuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.BlockBlastMissions
import com.mohamedelhajri.starpuzzle.core.BlockBlastSpec
import com.mohamedelhajri.starpuzzle.ui.screens.BbPersistence
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastGameScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastGameOverScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastMenuScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastMissionsScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastSettingsScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastThemesScreen
import com.mohamedelhajri.starpuzzle.core.StarThemes
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastShopScreen
import com.mohamedelhajri.starpuzzle.ui.theme.StarPuzzleTheme

/**
 * Navigation for the native Block Blast mode (owner direction, Oct 6 2026):
 * Menu -> Game -> Game Over, plus Shop & Missions. All Kotlin/Compose —
 * no WebView. Owner has Arabic locale: all game screens are locked to
 * LTR so the STAR logo and UI never render mirrored, and there is NO
 * background music (removed permanently by owner request) — only SFX.
 */
sealed class BbScreen {
    data object Menu : BbScreen()
    data object Game : BbScreen()
    data object Shop : BbScreen()
    data object Missions : BbScreen()
    data object Settings : BbScreen()
    data object Themes : BbScreen()
    data object DailyChallenge : BbScreen()
    data class GameOver(val score: Int, val best: Int, val coinsEarned: Int) : BbScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashShield()
        setContent {
            StarPuzzleTheme {
                StarPuzzleApp()
            }
        }
    }

    /**
     * Crash shield: every uncaught exception is persisted to crash.log before
     * the default handler runs, so any field report can be diagnosed from the
     * exact stack trace.
     */
    private fun installCrashShield() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                java.io.File(filesDir, "crash.log").writeText(
                    "version=8.2.0\n" + android.util.Log.getStackTraceString(throwable)
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

/** Best score, coins & skin persistence via the SharedPreferences store. */
private class BbPrefs(private val store: PrefsSaveStore) : BbPersistence {
    override fun loadBest(): Int = store.loadBestEndless()
    override fun loadCoins(): Int = store.loadCoins()
    override fun saveBest(value: Int) = store.saveBestEndless(value)
    override fun saveCoins(value: Int) = store.saveCoins(value)
    override fun loadGems(): Int = store.loadExtraInt("bb_gems", 0)
    override fun saveGems(value: Int) = store.saveExtraInt("bb_gems", value)
    override fun loadTotalLines(): Int = store.loadExtraInt("bb_total_lines", 0)
    override fun saveTotalLines(value: Int) = store.saveExtraInt("bb_total_lines", value)
}

/** Daily-mission progress persistence (per day, per mission index). */
private class BbMissionStore(private val store: PrefsSaveStore) : BlockBlastMissions.Store {
    override fun loadProgress(day: String, index: Int): Int = store.loadExtraInt("bbm_${day}_$index", 0)
    override fun saveProgress(day: String, index: Int, value: Int) = store.saveExtraInt("bbm_${day}_$index", value)
    override fun loadClaimed(day: String, index: Int): Boolean = store.loadExtraBool("bbm_claim_${day}_$index", false)
    override fun saveClaimed(day: String, index: Int, claimed: Boolean) =
        store.saveExtraBool("bbm_claim_${day}_$index", claimed)
}

@Composable
fun StarPuzzleApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { PrefsSaveStore(context) }
    val sound = remember { SoundManager(context) }
    val persistence = remember { BbPrefs(store) }
    val missionStore = remember { BbMissionStore(store) }

    // global one-time wiring
    remember {
        BlockBlastMissions.store = missionStore
        BlockBlastSpec.activeSkinId = store.loadSkin().coerceIn(0, BlockBlastSpec.SKINS.size - 1)
        StarThemes.activeId = store.loadExtraInt("bb_theme", 0).coerceIn(0, StarThemes.ALL.size - 1)
        BlockBlastSpec.reducedFx = store.loadExtraInt("bb_reduced_fx", 0) == 1
        // cell style & clear effect from the reference package (persisted)
        BlockBlastSpec.activeCellSkinId = store.loadExtraInt("bb_cell_skin", 0)
            .coerceIn(0, BlockBlastSpec.CELL_SKINS.size - 1)
        BlockBlastSpec.activeClearEffectId = store.loadExtraInt("bb_clear_effect", 0)
            .coerceIn(0, BlockBlastSpec.CLEAR_EFFECTS.size - 1)
        store.saveMusic(false) // background music removed permanently
        true
    }

    var screen by remember { mutableStateOf<BbScreen>(BbScreen.Menu) }
    var best by remember { mutableIntStateOf(persistence.loadBest()) }
    var coins by remember { mutableIntStateOf(persistence.loadCoins()) }
    var gems by remember { mutableIntStateOf(persistence.loadGems()) }
    // level grows with total career lines cleared (10 lines per level)
    var level by remember { mutableIntStateOf(1 + persistence.loadTotalLines() / 10) }
    var dailyAvailable by remember {
        mutableStateOf(store.loadExtraInt("bb_daily_stamp", 0) != todayStamp())
    }

    sound.enabled = store.loadSound()
    sound.music(false) // BGM off for good

    // Release the native SoundPool when the UI finally leaves
    DisposableEffect(sound) {
        onDispose { sound.release() }
    }

    // Lifecycle: silence everything in the background, resume in front
    DisposableEffect(sound) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> sound.onPause()
                Lifecycle.Event.ON_RESUME -> sound.onResume()
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    fun refreshPersisted() {
        best = persistence.loadBest()
        coins = persistence.loadCoins()
        gems = persistence.loadGems()
        level = 1 + persistence.loadTotalLines() / 10
    }

    fun claimDailyBonus() {
        if (dailyAvailable) {
            coins += 100
            gems += 5
            persistence.saveCoins(coins)
            persistence.saveGems(gems)
            store.saveExtraInt("bb_daily_stamp", todayStamp())
            dailyAvailable = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "screen"
        ) { current ->
            when (current) {
                is BbScreen.Menu -> BlockBlastMenuScreen(
                    level = level,
                    best = best,
                    coins = coins,
                    gems = gems,
                    dailyBonusAvailable = dailyAvailable,
                    soundManager = sound,
                    onPlay = { screen = BbScreen.Game },
                    onShop = { screen = BbScreen.Shop },
                    onMissions = { screen = BbScreen.Missions },
                    onSettings = { screen = BbScreen.Settings },
                    onClaimDailyBonus = { claimDailyBonus() },
                    onThemes = { screen = BbScreen.Themes },
                    dailyChallengeDone = store.loadExtraInt("bb_daily_done", 0) == todayStamp(),
                    onDailyChallenge = { screen = BbScreen.DailyChallenge }
                )
                is BbScreen.Game -> BlockBlastGameScreen(
                    soundManager = sound,
                    persistence = persistence,
                    onExit = {
                        refreshPersisted()
                        screen = BbScreen.Menu
                    },
                    onGameOver = { s, b, earned ->
                        refreshPersisted()
                        screen = BbScreen.GameOver(s, b, earned)
                    }
                )
                // Daily Challenge (§51): deterministic date-seeded game with a score goal.
                is BbScreen.DailyChallenge -> BlockBlastGameScreen(
                    soundManager = sound,
                    persistence = persistence,
                    dailySeed = dailySeed(),
                    dailyGoal = dailyGoal(),
                    onDailyComplete = {
                        if (store.loadExtraInt("bb_daily_done", 0) != todayStamp()) {
                            coins += 150
                            gems += 3
                            persistence.saveCoins(coins)
                            persistence.saveGems(gems)
                            store.saveExtraInt("bb_daily_done", todayStamp())
                        }
                    },
                    onExit = {
                        refreshPersisted()
                        screen = BbScreen.Menu
                    },
                    onGameOver = { s, b, earned ->
                        refreshPersisted()
                        screen = BbScreen.GameOver(s, b, earned)
                    }
                )
                is BbScreen.Shop -> BlockBlastShopScreen(
                    soundManager = sound,
                    onSkinSelected = { id ->
                        BlockBlastSpec.activeSkinId = id
                        store.saveSkin(id)
                    },
                    onCellSkinSelected = { id ->
                        BlockBlastSpec.activeCellSkinId = id
                        store.saveExtraInt("bb_cell_skin", id)
                    },
                    onClearEffectSelected = { id ->
                        BlockBlastSpec.activeClearEffectId = id
                        store.saveExtraInt("bb_clear_effect", id)
                    },
                    onBack = {
                        refreshPersisted()
                        screen = BbScreen.Menu
                    }
                )
                is BbScreen.Missions -> BlockBlastMissionsScreen(
                    soundManager = sound,
                    onCoinsEarned = { reward ->
                        coins += reward
                        persistence.saveCoins(coins)
                    },
                    onBack = { screen = BbScreen.Menu }
                )
                is BbScreen.Themes -> BlockBlastThemesScreen(
                    soundManager = sound,
                    selectedId = StarThemes.activeId,
                    onSelect = { id ->
                        StarThemes.activeId = id
                        store.saveExtraInt("bb_theme", id)
                    },
                    onBack = { screen = BbScreen.Menu }
                )
                is BbScreen.Settings -> BlockBlastSettingsScreen(
                    soundInitial = store.loadSound(),
                    hapticsInitial = store.loadHaptics(),
                    soundManager = sound,
                    onSoundChanged = { store.saveSound(it) },
                    onHapticsChanged = { store.saveHaptics(it) },
                    musicInitial = store.loadMusic(),
                    onMusicChanged = { store.saveMusic(it); sound.music(it) },
                    reducedFxInitial = store.loadExtraInt("bb_reduced_fx", 0) == 1,
                    onReducedFxChanged = {
                        BlockBlastSpec.reducedFx = it
                        store.saveExtraInt("bb_reduced_fx", if (it) 1 else 0)
                    },
                    onBack = { screen = BbScreen.Menu }
                )
                is BbScreen.GameOver -> BlockBlastGameOverScreen(
                    score = current.score,
                    best = current.best,
                    coins = coins,
                    onPlayAgain = { screen = BbScreen.Game },
                    onMenu = { screen = BbScreen.Menu },
                    newBest = current.score >= current.best && current.score > 0
                )
            }
        }
    }
}

/** Day stamp (days since epoch) for the daily-bonus cadence. */
private fun todayStamp(): Int =
    (System.currentTimeMillis() / 86_400_000L).toInt()

/** Deterministic daily-challenge seed from the calendar date (§51). */
private fun dailySeed(): Long {
    val cal = java.util.Calendar.getInstance()
    val y = cal.get(java.util.Calendar.YEAR)
    val m = cal.get(java.util.Calendar.MONTH) + 1
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return y * 10_000L + m * 100L + d
}

/** Daily goal: stable per date, gently scaled by the day number. */
private fun dailyGoal(): Int {
    val cal = java.util.Calendar.getInstance()
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return 1200 + (d % 12) * 150
}
