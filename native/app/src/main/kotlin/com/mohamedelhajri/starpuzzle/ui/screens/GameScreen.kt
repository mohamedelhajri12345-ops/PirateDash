package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.R
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.FeelState
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.GameSession
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import com.mohamedelhajri.starpuzzle.ui.components.BoosterChip
import com.mohamedelhajri.starpuzzle.ui.components.ConfettiBurst
import com.mohamedelhajri.starpuzzle.ui.components.StarMilestoneStrip
import com.mohamedelhajri.starpuzzle.core.LevelDefinition
import com.mohamedelhajri.starpuzzle.core.Piece
import com.mohamedelhajri.starpuzzle.ui.theme.pieceColor
import com.mohamedelhajri.starpuzzle.ui.theme.SkinState
import com.mohamedelhajri.starpuzzle.ui.theme.MaterialSkinCatalog
import com.mohamedelhajri.starpuzzle.ui.theme.drawMaterial
import com.mohamedelhajri.starpuzzle.engine.ParticleEngine
import com.mohamedelhajri.starpuzzle.engine.ParticleEvent
import com.mohamedelhajri.starpuzzle.ui.screens.CosmosBackdrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialBombColor
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialLightningColor
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialStarColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * The gameplay screen. The board is the hero: centered, clean, with live
 * drag preview (ghost piece + power-up affected area), snappy placement,
 * short satisfying clear animations and a quiet HUD above it.
 */
@Composable
fun GameScreen(
    levelId: Int,
    daily: Boolean,
    progress: GameProgress,
    sound: SoundManager,
    onExit: () -> Unit,
    onWorldMap: () -> Unit,
    onNext: (Int) -> Unit
) {
    val level = remember(levelId) { LevelCatalog.getLevel(levelId) }
    var restartKey by remember { mutableStateOf(0) }
    val liveSession = remember(levelId, restartKey) { GameSession(level) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // v3.1: particle juice engine, seeded per level so replays feel the same
    val particles = remember(levelId, restartKey) { ParticleEngine(level.seed) }

    // redraw trigger for tick-driven state (timer bar, time levels)
    var frame by remember { mutableStateOf(0) }

    // geometry captured from layout
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardPx by remember { mutableStateOf(0f) }
    var trayOrigin by remember { mutableStateOf(Offset.Zero) }
    var trayHeight by remember { mutableStateOf(0f) }
    var trayWidth by remember { mutableStateOf(0f) }
    // Real tray slot centers captured from layout — exact hit-testing
    val slotCenters = remember { FloatArray(8) }

    // drag state
    var dragIndex by remember { mutableStateOf(-1) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var dragValid by remember { mutableStateOf(false) }
    var dragTargetX by remember { mutableStateOf(-1) }
    var dragTargetY by remember { mutableStateOf(-1) }

    // snappy pick-up pop: the instant the finger grabs a piece it pops
    // bigger with a bouncy spring (TikTok-fast feel), then settles while held
    val pickupScale = remember { Animatable(1f) }
    LaunchedEffect(dragIndex) {
        if (dragIndex >= 0) {
            pickupScale.snapTo(1f)
            pickupScale.animateTo(
                1.18f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 1400f)
            )
            pickupScale.animateTo(
                1.08f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)
            )
        } else {
            pickupScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 900f))
        }
    }

    // clear animation — keyed by levelId: transient state must NEVER
    // leak from one level into the next (the v2.4.0 dialog bug)
    var clearAnimCells by remember(levelId) { mutableStateOf(listOf<Pair<Int, Int>>()) }
    // v6: the clear burst now carries the placed piece's real colour
    var clearAnimColor by remember(levelId) { mutableStateOf(Color(0xFFFFD54F)) }
    val clearProgress = remember(levelId) { Animatable(1f) }

    // combo popup
    var comboPopup by remember(levelId) { mutableStateOf(0) }
    val comboPopupAlpha = remember(levelId) { Animatable(0f) }
    // v3.1: encouragement bubble (the reference game's "+120 EXCELLENT!")
    var bubbleText by remember(levelId) { mutableStateOf("") }
    val bubbleAlpha = remember(levelId) { Animatable(0f) }

    var resultShown by remember(levelId) { mutableStateOf(false) }
    // store-booster targeting mode (tap a cell to apply)
    var boostTarget: GameProgress.BoosterKind? by remember(levelId) { mutableStateOf<GameProgress.BoosterKind?>(null) }
    var earnedStars by remember(levelId) { mutableStateOf(0) }
    var earnedCoins by remember(levelId) { mutableStateOf(0) }

    // The result path: called directly from placePiece/tick contexts.
    // (v2.4.0 bug: a LaunchedEffect keyed on the plain status field never
    // re-ran, so the win dialog never appeared — the player had to exit
    // the level manually. Direct calls can never miss.)
    fun showResult() {
        if (resultShown || liveSession.status == GameSession.Status.PLAYING) return
        resultShown = true
        if (liveSession.status == GameSession.Status.WON) {
            // SURVIVE has no leftover allowance by design — its stars
            // reward combo skill during the run instead
            val stars = level.starsFor(
                if (level.objectiveType == LevelDefinition.TYPE_SURVIVE)
                    (liveSession.maxCombo / 3f).coerceAtMost(1f)
                else liveSession.allowanceLeftFraction()
            )
            earnedStars = stars
            progress.recordBestCombo(liveSession.maxCombo)
            val recorded = progress.recordLevelResult(level.id, stars, level.rewardCoins)
            earnedCoins = if (recorded) level.rewardCoins else 0
            if (daily) progress.markDailyDone(LevelCatalog.dailyKey())
            // "complete N levels" mission: this win counts as one
            progress.trackDailyMission(lines = 0, score = 0, levelDone = true)
            sound.play(SoundManager.Sfx.COMPLETE)
            if (stars >= 3) sound.play(SoundManager.Sfx.HIGH_SCORE)
            sound.play(SoundManager.Sfx.COIN)
        } else {
            sound.play(SoundManager.Sfx.GAME_OVER)
        }
    }

    BackHandler { onExit() }

    // a soft sting when the level opens (and on retry)
    LaunchedEffect(levelId, restartKey) { sound.play(SoundManager.Sfx.START) }

    // timer + frame redraw loop
    LaunchedEffect(liveSession) {
        var last = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            liveSession.tick(dt)
            particles.tick(dt * 1000f)
            // Redraw while anything is animating: the timed levels' progress
            // bar or live particles/shake. Idle boards cost zero frames.
            if (level.timeLimit > 0 || particles.isActive) {
                frame++
                if (liveSession.status != GameSession.Status.PLAYING) showResult()
            }
        }
    }

    fun updateDragTarget() {
        if (boardPx <= 0f) return
        val piece = liveSession.tray.getOrNull(dragIndex) ?: return
        val px = dragPos.x - boardPx * piece.cellCols * 0.5f
        val py = dragPos.y - boardPx * piece.cellRows * 0.5f - boardPx * 1.2f
        val gx = ((px - boardOrigin.x) / boardPx).toInt()
        val gy = ((py - boardOrigin.y) / boardPx).toInt()
        dragTargetX = gx
        dragTargetY = gy
        dragValid = liveSession.board.canPut(piece, gx, gy)
    }

    fun tryPlace() {
        if (dragIndex !in liveSession.tray.indices || boardPx <= 0f) {
            dragIndex = -1
            return
        }
        val piece = liveSession.tray[dragIndex]
        val event = liveSession.placePiece(
            dragIndex,
            ((dragPos.x - boardPx * (piece.cellCols * 0.5f) - boardOrigin.x) / boardPx).toInt(),
            ((dragPos.y - boardPx * (piece.cellRows * 0.5f) - boardPx * 1.2f - boardOrigin.y) / boardPx).toInt()
        )
        if (event == null) {
            sound.play(SoundManager.Sfx.INVALID)
        } else {
            frame++ // placement changed the board — redraw now
            // daily missions: every placement feeds the day's counters
            progress.trackDailyMission(
                lines = event.lines, score = event.points, levelDone = false
            )
            sound.play(SoundManager.Sfx.PLACE)
            if (event.clearedCells.isNotEmpty()) {
                clearAnimCells = event.clearedCells
                clearAnimColor = pieceColor(piece.colorIndex)
                scope.launch {
                    clearProgress.snapTo(0f)
                    clearProgress.animateTo(1f, tween(320))
                }
                sound.play(SoundManager.Sfx.CLEAR)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            when (event.powerUpKind) {
                Piece.SPECIAL_BOMB -> sound.play(SoundManager.Sfx.BOMB)
                Piece.SPECIAL_LIGHTNING -> sound.play(SoundManager.Sfx.COMBO)
                Piece.SPECIAL_STAR -> sound.play(SoundManager.Sfx.STAR)
            }
            if (event.combo >= 2) {
                comboPopup = event.combo
                scope.launch {
                    comboPopupAlpha.snapTo(1f)
                    comboPopupAlpha.animateTo(0f, tween(900))
                }
                when {
                    event.combo >= 4 -> sound.play(SoundManager.Sfx.COMBO4)
                    event.combo == 3 -> sound.play(SoundManager.Sfx.COMBO3)
                    else -> sound.play(SoundManager.Sfx.COMBO2)
                }
            }

            // ── v3.1 juice: particles + shake + encouragement bubble ──
            // (honors the Reduced-motion accessibility setting)
            if (FeelState.motionOn) {
            val lx = dragPos.x - boardOrigin.x
            val ly = dragPos.y - boardOrigin.y
            val landColor = pieceColor(piece.colorIndex)
            particles.spawn(ParticleEvent.PIECE_LAND, lx, ly, boardPx, listOf(landColor))
            if (event.clearedCells.isNotEmpty()) {
                for ((cx, cy) in event.clearedCells.take(60)) {
                    particles.spawn(
                        ParticleEvent.LINE_CLEAR,
                        (cx + 0.5f) * boardPx, (cy + 0.5f) * boardPx,
                        boardPx, listOf(landColor)
                    )
                }
                if (event.lines >= 2) {
                    particles.spawn(ParticleEvent.MULTI_CLEAR, lx, ly, boardPx, listOf(landColor))
                    particles.triggerShake(min(2f + event.lines, 6f))
                }
            }
            when (event.powerUpKind) {
                Piece.SPECIAL_BOMB -> {
                    particles.spawn(ParticleEvent.BOMB_BOOST, lx, ly, boardPx, listOf(landColor))
                    particles.triggerShake(6f)
                }
                Piece.SPECIAL_LIGHTNING -> {
                    particles.spawn(ParticleEvent.MULTI_CLEAR, lx, ly, boardPx, listOf(landColor))
                    particles.triggerShake(4f)
                }
                Piece.SPECIAL_STAR -> particles.spawn(
                    ParticleEvent.STAR_SPECIAL, lx, ly, boardPx,
                    listOf(Color(0xFFFFD54F))
                )
            }
            if (event.combo >= 2) {
                particles.spawn(ParticleEvent.COMBO, lx, ly, boardPx, listOf(Color(0xFFFFD54F)))
            }
            if (event.points >= 25 || event.lines > 0) {
                val praise = when {
                    event.lines >= 3 -> "AMAZING!"
                    event.lines == 2 -> "EXCELLENT!"
                    event.combo >= 3 -> "EXCELLENT!"
                    event.lines == 1 -> "GREAT!"
                    else -> "NICE!"
                }
                bubbleText = "+${event.points} $praise"
                scope.launch {
                    bubbleAlpha.snapTo(1f)
                    bubbleAlpha.animateTo(0f, tween(1000))
                }
            }
            } // FeelState gate
        }
        // a placement may have just finished the level (or lost it)
        if (liveSession.status != GameSession.Status.PLAYING) showResult()
        dragIndex = -1
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(liveSession, boostTarget) {
                detectTapGestures { pos ->
                    val kind = boostTarget ?: return@detectTapGestures
                    if (liveSession.status != GameSession.Status.PLAYING) {
                        boostTarget = null; return@detectTapGestures
                    }
                    val gx = ((pos.x - boardOrigin.x) / boardPx).toInt()
                    val gy = ((pos.y - boardOrigin.y) / boardPx).toInt()
                    if (gx !in 0..9 || gy !in 0..9) return@detectTapGestures
                    // BOMB/ZAP need a filled target cell — never burn a
                    // booster on an empty tap. STAR may also fill an empty
                    // cell (that is its purpose: drop a helpful block).
                    val needsCell = kind != GameProgress.BoosterKind.STAR
                    if (needsCell && liveSession.board.colorAt(gx, gy) < 0) {
                        sound.play(SoundManager.Sfx.INVALID)
                        boostTarget = null; return@detectTapGestures
                    }
                    if (progress.spendBooster(kind)) {
                        val cells = liveSession.applyBooster(kind, gx, gy)
                        if (cells.isNotEmpty()) {
                            if (cells.size > 1 || kind == GameProgress.BoosterKind.BOMB) {
                                clearAnimCells = cells
                                clearAnimColor = when (kind) {
                                    GameProgress.BoosterKind.LIGHTNING -> Color(0xFF40C4FF)
                                    else -> Color(0xFFFFD54F)
                                }
                                scope.launch {
                                    clearProgress.snapTo(0f)
                                    clearProgress.animateTo(1f, tween(320))
                                }
                            }
                            sound.play(
                                when (kind) {
                                    GameProgress.BoosterKind.BOMB -> SoundManager.Sfx.BOMB
                                    GameProgress.BoosterKind.LIGHTNING -> SoundManager.Sfx.COMBO
                                    else -> SoundManager.Sfx.STAR
                                }
                            )
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            progress.incBoostersUsed()
                            if (FeelState.motionOn) {
                            val blx = (gx + 0.5f) * boardPx
                            val bly = (gy + 0.5f) * boardPx
                            when (kind) {
                                GameProgress.BoosterKind.BOMB -> {
                                    particles.spawn(ParticleEvent.BOMB_BOOST, blx, bly, boardPx, listOf(Color(0xFFFFD54F)))
                                    particles.triggerShake(6f)
                                }
                                GameProgress.BoosterKind.LIGHTNING -> {
                                    particles.spawn(ParticleEvent.MULTI_CLEAR, blx, bly, boardPx, listOf(Color(0xFF40C4FF)))
                                    particles.triggerShake(4f)
                                }
                                else -> particles.spawn(ParticleEvent.STAR_SPECIAL, blx, bly, boardPx, listOf(Color(0xFFFFD54F)))
                            }
                            }
                            frame++
                        }
                    }
                    boostTarget = null
                }
            }
            .pointerInput(liveSession) {
                detectDragGestures(
                    onDragStart = { pos ->
                        if (trayWidth <= 0f) return@detectDragGestures
                        val inTrayY = pos.y >= trayOrigin.y &&
                                pos.y <= trayOrigin.y + trayHeight
                        if (inTrayY && liveSession.tray.isNotEmpty()) {
                            // exact hit-test: the piece whose real captured
                            // center is nearest to the finger gets picked
                            val n = liveSession.tray.size
                            val idx = if (n <= 8 && slotCenters[n - 1] > 0f) {
                                var best = 0
                                var bestDist = Float.MAX_VALUE
                                for (i in 0 until n) {
                                    val d = kotlin.math.abs(pos.x - slotCenters[i])
                                    if (d < bestDist) { bestDist = d; best = i }
                                }
                                best
                            } else {
                                ((pos.x - trayOrigin.x) / (trayWidth / n))
                                        .toInt().coerceIn(0, n - 1)
                            }
                            dragIndex = idx
                            dragPos = pos
                            updateDragTarget()
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragIndex >= 0) {
                            dragPos = change.position
                            updateDragTarget()
                        }
                    },
                    onDragEnd = { tryPlace() },
                    onDragCancel = { dragIndex = -1 }
                )
            }
    ) {
        // Phase B: the level's world identity animates behind the board
        CosmosBackdrop(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            frame // read the ticking state so the HUD recomposes every frame

            // ── HUD ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    if (daily) "DAILY CHALLENGE" else "LEVEL ${level.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                // v6: real gem artwork for the coin balance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC1F1B3A))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gem_coin),
                        contentDescription = "Coins",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${progress.coins}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFE082)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${liveSession.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Objective ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        level.objectiveText(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    val allowText = if (level.timeLimit > 0)
                        "${liveSession.timeLeft.toInt()}s"
                    else
                        "${liveSession.movesLeft} moves"
                    Text(
                        allowText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                val objProgress = min(
                    1f,
                    liveSession.objectiveProgress().toFloat() / level.target().coerceAtLeast(1)
                )
                // approved design: glowing star gems replace the old bar
                StarMilestoneStrip(objProgress, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.weight(1f))

            // ── Board ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { coords ->
                        boardOrigin = coords.positionInRoot()
                        boardPx = coords.size.width.toFloat() / 10f
                    }
            ) {
                BoardCanvas(
                    session = liveSession,
                    particles = particles,
                    clearAnimCells = clearAnimCells,
                    clearAnimColor = clearAnimColor,
                    clearProgress = clearProgress.value,
                    dragIndex = dragIndex,
                    dragValid = dragValid,
                    dragTargetX = dragTargetX,
                    dragTargetY = dragTargetY,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── Booster bar (store inventory — the approved store items) ──
            val inv = progress.boosters()
            val playing = liveSession.status == GameSession.Status.PLAYING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                BoosterChip("BOMB", inv.bomb, boostTarget == GameProgress.BoosterKind.BOMB, {
                    boostTarget = if (boostTarget == GameProgress.BoosterKind.BOMB) null
                    else GameProgress.BoosterKind.BOMB
                }, enabled = playing && inv.bomb > 0, sound)
                BoosterChip("ZAP", inv.lightning, boostTarget == GameProgress.BoosterKind.LIGHTNING, {
                    boostTarget = if (boostTarget == GameProgress.BoosterKind.LIGHTNING) null
                    else GameProgress.BoosterKind.LIGHTNING
                }, enabled = playing && inv.lightning > 0, sound)
                BoosterChip("STAR", inv.star, boostTarget == GameProgress.BoosterKind.STAR, {
                    boostTarget = if (boostTarget == GameProgress.BoosterKind.STAR) null
                    else GameProgress.BoosterKind.STAR
                }, enabled = playing && inv.star > 0, sound)
                BoosterChip("MOVE", inv.move, false, {
                    if (playing && liveSession.canTakeBack() &&
                        progress.spendBooster(GameProgress.BoosterKind.MOVE)) {
                        liveSession.takeBackLast()
                        sound.play(SoundManager.Sfx.PLACE)
                        frame++
                    } else sound.play(SoundManager.Sfx.INVALID)
                }, enabled = playing && inv.move > 0, sound)
            }
            if (boostTarget != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "TAP A CELL ON THE BOARD",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Piece tray ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .onGloballyPositioned { coords ->
                        trayOrigin = coords.positionInRoot()
                        trayHeight = coords.size.height.toFloat()
                        trayWidth = coords.size.width.toFloat()
                    }
            ) {
                val slotSize = if (liveSession.tray.size > 3) 72.dp else 84.dp
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    liveSession.tray.forEachIndexed { index, piece ->
                        Canvas(
                            modifier = Modifier
                                .size(slotSize)
                                .onGloballyPositioned { c ->
                                    if (c.size.width > 0 && index < 8)
                                        slotCenters[index] =
                                            c.positionInRoot().x + c.size.width / 2f
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            val cell = min(size.width, size.height) / 5.6f
                            drawPiece(
                                piece,
                                origin = Offset(
                                    (size.width - piece.cellCols * cell) / 2f,
                                    (size.height - piece.cellRows * cell) / 2f
                                ),
                                cellSize = cell,
                                alpha = if (index == dragIndex) 0.25f else 1f
                            )
                        }
                    }
                }
            }
        }

        // ── Dragged piece overlay (root coords) ──
        // pickupScale pops the piece bigger the instant it's grabbed (snappy
        // "TikTok-fast" feedback), then eases to a slightly-enlarged held size.
        if (dragIndex >= 0 || pickupScale.value > 1.001f) {
            val heldPiece = liveSession.tray.getOrNull(dragIndex)
            if (heldPiece != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val liveBoardPx = boardPx * pickupScale.value
                    val cx = dragPos.x
                    val cy = dragPos.y - boardPx * 1.2f
                    val px = cx - liveBoardPx * heldPiece.cellCols * 0.5f
                    val py = cy - liveBoardPx * heldPiece.cellRows * 0.5f
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.13f),
                        radius = liveBoardPx * 1.1f,
                        center = Offset(
                            px + heldPiece.cellCols * liveBoardPx * 0.5f,
                            py + heldPiece.cellRows * liveBoardPx * 0.5f
                        )
                    )
                    drawPiece(heldPiece, Offset(px, py), liveBoardPx, alpha = 0.95f)
                }
            }
        }

        // ── Combo popup ──
        if (comboPopupAlpha.value > 0f && comboPopup >= 2) {
            Text(
                "COMBO x$comboPopup",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFFFD54F).copy(alpha = comboPopupAlpha.value),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
            )
        }

        // ── v3.1: encouragement bubble ──
        if (bubbleAlpha.value > 0f && bubbleText.isNotEmpty()) {
            Text(
                bubbleText,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFD54F).copy(alpha = bubbleAlpha.value),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 60.dp)
            )
        }

        // ── Input: drag from tray onto board ──
        // NOTE: input lives on the ROOT so children (Back, MOVE, dialogs)
        // receive their taps first. The old invisible full-screen overlay
        // sat on top of the HUD and swallowed every button click — that was
        // why the back arrow and MOVE appeared "broken".

        // ── Result dialog ──
        if (resultShown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                if (liveSession.status == GameSession.Status.WON) ConfettiBurst()
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    ResultContent(
                        won = liveSession.status == GameSession.Status.WON,
                        stars = earnedStars,
                        score = liveSession.score,
                        coins = earnedCoins,
                        onReplay = { restartKey++; resultShown = false; earnedStars = 0 },
                        onMap = onWorldMap,
                        // the daily challenge never chains into the level flow
                        onNext = if (!daily && liveSession.status == GameSession.Status.WON &&
                            level.id < LevelCatalog.TOTAL_LEVELS)
                            ({ onNext(level.id + 1) }) else null,
                        onDone = if (daily && liveSession.status == GameSession.Status.WON)
                            ({ onExit() }) else null
                    )
                }
            }
        }
    }
}

/** Draws the 10x10 board: empty slots, filled cells, clear animation, previews. */
@Composable
private fun BoardCanvas(
    session: GameSession,
    particles: ParticleEngine,
    clearAnimCells: List<Pair<Int, Int>>,
    clearAnimColor: Color,
    clearProgress: Float,
    dragIndex: Int,
    dragValid: Boolean,
    dragTargetX: Int,
    dragTargetY: Int,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        withTransform({ translate(particles.shakeOffsetX, particles.shakeOffsetY) }) {
        val n = session.board.size
        val cell = size.width / n
        val gap = cell * 0.08f
        val radius = CornerRadius(cell * 0.22f)

        drawRoundRect(
            color = Color(0xFF16204A),
            cornerRadius = CornerRadius(cell * 0.5f),
            size = Size(size.width, size.height)
        )

        // power-up affected-area preview
        if (dragIndex >= 0 && dragTargetX in 0 until n && dragTargetY in 0 until n) {
            val held = session.tray.getOrNull(dragIndex)
            val tint = if (dragValid) Color(0xFFFFD54F).copy(alpha = 0.28f)
            else Color(0xFFEF5350).copy(alpha = 0.20f)
            if (held != null && held.isSpecial) {
                when (held.colorIndex) {
                    Piece.SPECIAL_BOMB -> {
                        for (i in (dragTargetY - 1)..(dragTargetY + 1))
                            for (j in (dragTargetX - 1)..(dragTargetX + 1))
                                if (i in 0 until n && j in 0 until n)
                                    drawRoundRect(
                                        tint,
                                        topLeft = Offset(j * cell + gap / 2, i * cell + gap / 2),
                                        size = Size(cell - gap, cell - gap),
                                        cornerRadius = radius
                                    )
                    }
                    Piece.SPECIAL_LIGHTNING -> {
                        for (j in 0 until n)
                            drawRoundRect(
                                tint,
                                topLeft = Offset(j * cell + gap / 2, dragTargetY * cell + gap / 2),
                                size = Size(cell - gap, cell - gap),
                                cornerRadius = radius
                            )
                        for (i in 0 until n)
                            drawRoundRect(
                                tint,
                                topLeft = Offset(dragTargetX * cell + gap / 2, i * cell + gap / 2),
                                size = Size(cell - gap, cell - gap),
                                cornerRadius = radius
                            )
                    }
                    else -> drawRoundRect(
                        tint,
                        topLeft = Offset(dragTargetX * cell + gap / 2, dragTargetY * cell + gap / 2),
                        size = Size(cell - gap, cell - gap),
                        cornerRadius = radius
                    )
                }
            } else if (held != null && dragValid) {
                for (i in 0 until held.cellRows)
                    for (j in 0 until held.cellCols)
                        if (held.filled(i, j))
                            drawRoundRect(
                                pieceColor(held.colorIndex).copy(alpha = 0.35f),
                                topLeft = Offset(
                                    (dragTargetX + j) * cell + gap / 2,
                                    (dragTargetY + i) * cell + gap / 2),
                                size = Size(cell - gap, cell - gap),
                                cornerRadius = radius
                            )
            }
        }

        // empty slots
        for (y in 0 until n)
            for (x in 0 until n) {
                drawRoundRect(
                    color = Color(0x14FFFFFF),
                    topLeft = Offset(x * cell + gap / 2, y * cell + gap / 2),
                    size = Size(cell - gap, cell - gap),
                    cornerRadius = radius
                )
            }

        // filled cells — v3.1: full material rendering (gloss, bevel, shadow)
        val skin = MaterialSkinCatalog.getSkin(SkinState.active)
        val matCount = skin.materials.size
        for (y in 0 until n)
            for (x in 0 until n) {
                val color = session.board.colorAt(x, y)
                if (color >= 0) {
                    val idx = ((color % matCount) + matCount) % matCount
                    drawMaterial(
                        topLeft = Offset(x * cell + gap / 2, y * cell + gap / 2),
                        size = Size(cell - gap, cell - gap),
                        spec = skin.materials[idx]
                    )
                }
            }

        // v6 clear burst: coloured glow + expanding pop + hot core
        if (clearProgress < 1f) {
            val p = clearProgress
            for ((x, y) in clearAnimCells) {
                val cx = x * cell + cell / 2f
                val cy = y * cell + cell / 2f
                // 1. expanding glow halo in the piece's real colour
                val halo = cell * (0.55f + 1.15f * p)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            clearAnimColor.copy(alpha = 0.60f * (1f - p)),
                            clearAnimColor.copy(alpha = 0f)
                        ),
                        center = Offset(cx, cy),
                        radius = halo
                    ),
                    radius = halo,
                    center = Offset(cx, cy)
                )
                // 2. the cell itself pops BIGGER while fading (explosion feel)
                val cellPx = (cell - gap) * (1f + 0.38f * p)
                val off = (cell - cellPx) / 2f
                drawRoundRect(
                    color = clearAnimColor.copy(alpha = 0.92f * (1f - p)),
                    topLeft = Offset(x * cell + off, y * cell + off),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(cellPx * 0.22f)
                )
                // 3. white-hot core collapsing to nothing
                val corePx = (cell - gap) * (1f - p) * 0.62f
                if (corePx > 0f) {
                    val coreOff = (cell - corePx) / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f * (1f - p)),
                        topLeft = Offset(x * cell + coreOff, y * cell + coreOff),
                        size = Size(corePx, corePx),
                        cornerRadius = CornerRadius(corePx * 0.22f)
                    )
                }
            }
        // v6.1: golden spark particles rising from the burst (the video's
        // satisfying sparkle feel — procedural, zero bitmap cost)
        if (clearProgress < 1f && clearAnimCells.isNotEmpty()) {
            for (k in clearAnimCells.indices) {
                val (x, y) = clearAnimCells[k]
                for (j in 0 until 3) {
                    val seed = ((k * 31 + j * 17) % 100) / 100f
                    val delay = seed * 0.35f
                    val pp = ((clearProgress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    if (pp <= 0f) continue
                    val sx = x * cell + cell / 2f + (seed - 0.5f) * cell * 1.6f
                    val sy = (y * cell + cell / 2f) - pp * cell * (1.2f + seed)
                    val sr = cell * 0.09f * (1f - pp) * (0.6f + seed)
                    if (sr > 0.5f) {
                        drawCircle(
                            Color(0xFFFFF3C4).copy(alpha = (1f - pp)),
                            radius = sr,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }
        }
        }
        // v3.1: the particle layer, on top of everything
        particles.draw(this)
        }
    }
}

/** Draws a piece (used by the tray and the drag overlay). */
fun DrawScope.drawPiece(
    piece: Piece,
    origin: Offset,
    cellSize: Float,
    alpha: Float = 1f
) {
    if (piece.isSpecial) {
        drawSpecialGlyph(piece.colorIndex, origin, cellSize, alpha)
        return
    }
    val gap = cellSize * 0.08f
    val skin = MaterialSkinCatalog.getSkin(SkinState.active)
    val matCount = skin.materials.size
    val idx = ((piece.colorIndex % matCount) + matCount) % matCount
    for (i in 0 until piece.cellRows)
        for (j in 0 until piece.cellCols)
            if (piece.filled(i, j)) {
                val tl = Offset(
                    origin.x + j * cellSize + gap / 2,
                    origin.y + i * cellSize + gap / 2
                )
                drawMaterial(tl, Size(cellSize - gap, cellSize - gap), skin.materials[idx], alpha)
            }
}

/** Star / bomb / lightning glyphs drawn with vector paths (no bitmaps). */
fun DrawScope.drawSpecialGlyph(kind: Int, origin: Offset, cellSize: Float, alpha: Float) {
    val center = Offset(origin.x + cellSize / 2f, origin.y + cellSize / 2f)
    when (kind) {
        Piece.SPECIAL_STAR -> {
            val path = Path()
            val rOuter = cellSize * 0.42f
            val rInner = rOuter * 0.45f
            for (k in 0 until 10) {
                val r = if (k % 2 == 0) rOuter else rInner
                val a = -Math.PI / 2 + k * Math.PI / 10
                val x = (center.x + r * kotlin.math.cos(a)).toFloat()
                val y = (center.y + r * kotlin.math.sin(a)).toFloat()
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, SpecialStarColor.copy(alpha = alpha))
        }
        Piece.SPECIAL_BOMB -> {
            drawCircle(SpecialBombColor.copy(alpha = alpha), cellSize * 0.32f, center)
            drawCircle(
                Color.Black.copy(alpha = alpha * 0.35f), cellSize * 0.16f,
                Offset(center.x - cellSize * 0.08f, center.y - cellSize * 0.10f)
            )
            drawArc(
                color = Color(0xFFFFA726).copy(alpha = alpha),
                startAngle = -60f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(center.x - cellSize * 0.08f, center.y - cellSize * 0.40f),
                size = Size(cellSize * 0.16f, cellSize * 0.16f),
                style = Stroke(width = cellSize * 0.05f)
            )
        }
        else -> {
            val path = Path()
            val w = cellSize * 0.36f
            path.moveTo(center.x - w * 0.3f, center.y - cellSize * 0.42f)
            path.lineTo(center.x + w * 0.35f, center.y - cellSize * 0.05f)
            path.lineTo(center.x + w * 0.05f, center.y + cellSize * 0.02f)
            path.lineTo(center.x + w * 0.30f, center.y + cellSize * 0.42f)
            path.lineTo(center.x - w * 0.35f, center.y - cellSize * 0.02f)
            path.lineTo(center.x - w * 0.05f, center.y - cellSize * 0.09f)
            path.close()
            drawPath(path, SpecialLightningColor.copy(alpha = alpha))
        }
    }
}

/** Win / lose content: stars, score, coins, clear next steps. */
@Composable
private fun ResultContent(
    won: Boolean,
    stars: Int,
    score: Int,
    coins: Int,
    onReplay: () -> Unit,
    onMap: () -> Unit,
    onNext: (() -> Unit)?,
    onDone: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (won) "LEVEL COMPLETE!" else "THE SKY IS FULL!",
            style = MaterialTheme.typography.headlineMedium,
            color = if (won) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Row {
            repeat(3) { i ->
                val active = i < stars
                val scale by animateFloatAsState(
                    targetValue = if (active) 1f else 0.85f,
                    animationSpec = tween(300, delayMillis = i * 120),
                    label = "star$i"
                )
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .scale(scale)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$score",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "SCORE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (coins > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "+$coins coins",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        when {
            onNext != null -> {
                GlossyButton("NEXT LEVEL", onNext, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                        Text(" RETRY")
                    }
                    OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                        Text("MAP")
                    }
                }
            }
            onDone != null -> {
                GlossyButton("DONE", onDone, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onReplay, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                    Text(" RETRY")
                }
            }
            else -> {
                // the sky is full: RETRY is the warm gold call to action
                GlossyButton("RETRY", onReplay, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth()) {
                    Text("WORLD MAP")
                }
            }
        }
    }
}

/**
 * The glossy primary CTA from the reference game, in our night-sky palette:
 * a vertical gold gradient with a soft sheen band near the top edge.
 */
@Composable
private fun GlossyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(base, base.copy(alpha = 0.68f))))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
