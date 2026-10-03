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
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import com.mohamedelhajri.starpuzzle.ui.screens.GameScreen
import com.mohamedelhajri.starpuzzle.ui.screens.MainMenuScreen
import com.mohamedelhajri.starpuzzle.ui.screens.WorldMapScreen
import com.mohamedelhajri.starpuzzle.ui.theme.PieceSkins
import com.mohamedelhajri.starpuzzle.ui.theme.SkinState
import com.mohamedelhajri.starpuzzle.ui.theme.StarPuzzleTheme

sealed class Screen {
    data object Menu : Screen()
    data object WorldMap : Screen()
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
                    "version=2.4.0\n" + android.util.Log.getStackTraceString(throwable)
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
    var hapticsOn by remember { mutableStateOf(store.loadHaptics()) }

    sound.enabled = soundOn

    // Release the native SoundPool when the UI finally leaves
    DisposableEffect(sound) {
        onDispose { sound.release() }
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
                hapticsOn = hapticsOn,
                onToggleSound = { soundOn = it; store.saveSound(it) },
                onToggleHaptics = { hapticsOn = it; store.saveHaptics(it) },
                onPlay = { screen = Screen.Game(progress.firstUnfinished(), false) },
                onWorldMap = { screen = Screen.WorldMap },
                onDaily = { screen = Screen.Game(LevelCatalog.dailyLevelId(), true) }
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
