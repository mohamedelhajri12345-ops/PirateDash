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
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "screen"
        ) { current ->
            when (current) {
                is BbScreen.Menu -> BlockBlastMenuScreen(
                    best = best,
                    coins = coins,
                    soundManager = sound,
                    onPlay = { screen = BbScreen.Game },
                    onShop = { screen = BbScreen.Shop },
                    onMissions = { screen = BbScreen.Missions }
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
                is BbScreen.GameOver -> BlockBlastGameOverScreen(
                    score = current.score,
                    best = current.best,
                    coins = coins,
                    onPlayAgain = { screen = BbScreen.Game },
                    onMenu = { screen = BbScreen.Menu }
                )
            }
        }
    }
}
