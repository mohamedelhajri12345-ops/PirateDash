package com.mohamedelhajri.starpuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import com.mohamedelhajri.starpuzzle.ui.screens.GameScreen
import com.mohamedelhajri.starpuzzle.ui.screens.MainMenuScreen
import com.mohamedelhajri.starpuzzle.ui.screens.StoreScreen
import com.mohamedelhajri.starpuzzle.ui.screens.WorldMapScreen
import com.mohamedelhajri.starpuzzle.ui.theme.PieceSkins
import com.mohamedelhajri.starpuzzle.ui.theme.SkinState
import com.mohamedelhajri.starpuzzle.ui.theme.StarPuzzleTheme

sealed class Screen {
    data object Menu : Screen()
    data object WorldMap : Screen()
    data object Store : Screen()
    data class Game(val levelId: Int, val daily: Boolean) : Screen()
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
     * exact stack trace. The audit fixes keep this from ever firing in play.
     */
    private fun installCrashShield() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                java.io.File(filesDir, "crash.log").writeText(
                    "version=3.0.1\n" + android.util.Log.getStackTraceString(throwable)
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

@Composable
fun StarPuzzleApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { PrefsSaveStore(context) }
    val sound = remember { SoundManager(context) }
    // restore the saved piece-color skin (coerced against catalog size)
    SkinState.active = store.loadSkin().coerceIn(0, PieceSkins.size - 1)
    val progress = remember { GameProgress(store) }

    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
    var soundOn by remember { mutableStateOf(store.loadSound()) }
    var musicOn by remember { mutableStateOf(store.loadMusic()) }
    var hapticsOn by remember { mutableStateOf(store.loadHaptics()) }

    sound.enabled = soundOn
    sound.music(musicOn)

    // Release the native SoundPool when the UI finally leaves
    DisposableEffect(sound) {
        onDispose { sound.release() }
    }

    // Lifecycle: silence everything in the background, resume in front
    DisposableEffect(sound, musicOn) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> sound.pauseAll()
                Lifecycle.Event.ON_RESUME -> sound.resumeMusic()
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "screen"
    ) { current ->
        when (current) {
            is Screen.Menu -> MainMenuScreen(
                progress = progress,
                sound = sound,
                soundOn = soundOn,
                musicOn = musicOn,
                hapticsOn = hapticsOn,
                onToggleSound = { soundOn = it; store.saveSound(it) },
                onToggleMusic = { musicOn = it; store.saveMusic(it); sound.music(it) },
                onToggleHaptics = { hapticsOn = it; store.saveHaptics(it) },
                onPlay = { screen = Screen.Game(progress.firstUnfinished(), false) },
                onWorldMap = { screen = Screen.WorldMap },
                onDaily = { screen = Screen.Game(LevelCatalog.dailyLevelId(), true) },
                onOpenStore = { screen = Screen.Store }
            )
            is Screen.Store -> StoreScreen(
                progress = progress,
                sound = sound,
                onBack = { screen = Screen.Menu }
            )
            is Screen.WorldMap -> WorldMapScreen(
                progress = progress,
                onBack = { screen = Screen.Menu },
                onPickLevel = { id -> screen = Screen.Game(id, false) }
            )
            is Screen.Game -> GameScreen(
                levelId = current.levelId,
                daily = current.daily,
                progress = progress,
                sound = sound,
                onExit = { screen = Screen.Menu },
                onWorldMap = { screen = Screen.WorldMap },
                onNext = { id -> screen = Screen.Game(id, current.daily) }
            )
        }
    }
}
