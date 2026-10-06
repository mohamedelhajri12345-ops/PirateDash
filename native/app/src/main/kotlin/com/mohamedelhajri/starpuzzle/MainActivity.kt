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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.ui.screens.BbPersistence
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastGameScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastGameOverScreen
import com.mohamedelhajri.starpuzzle.ui.screens.BlockBlastMenuScreen
import com.mohamedelhajri.starpuzzle.ui.theme.StarPuzzleTheme

/**
 * Navigation for the native Block Blast mode (owner direction, Oct 6 2026):
 * Menu -> Game -> Game Over, all in Kotlin/Compose — no WebView.
 */
sealed class BbScreen {
    data object Menu : BbScreen()
    data object Game : BbScreen()
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
                    "version=8.1.0\n" + android.util.Log.getStackTraceString(throwable)
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

/** Best score & coins via the existing SharedPreferences-backed store. */
private class BbPrefs(private val store: PrefsSaveStore) : BbPersistence {
    override fun loadBest(): Int = store.loadBestEndless()
    override fun loadCoins(): Int = store.loadCoins()
    override fun saveBest(value: Int) = store.saveBestEndless(value)
    override fun saveCoins(value: Int) = store.saveCoins(value)
}

@Composable
fun StarPuzzleApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { PrefsSaveStore(context) }
    val sound = remember { SoundManager(context) }
    val persistence = remember { BbPrefs(store) }

    var screen by remember { mutableStateOf<BbScreen>(BbScreen.Menu) }
    var best by remember { mutableStateOf(persistence.loadBest()) }
    var coins by remember { mutableStateOf(persistence.loadCoins()) }

    sound.enabled = store.loadSound()
    sound.music(store.loadMusic())

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

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "screen"
    ) { current ->
        when (current) {
            is BbScreen.Menu -> BlockBlastMenuScreen(
                best = best,
                coins = coins,
                onPlay = { screen = BbScreen.Game }
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
