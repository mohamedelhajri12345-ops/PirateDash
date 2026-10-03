package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.GameSession
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import com.mohamedelhajri.starpuzzle.core.LevelDefinition
import com.mohamedelhajri.starpuzzle.core.Piece
import com.mohamedelhajri.starpuzzle.ui.theme.pieceColor
import com.mohamedelhajri.starpuzzle.ui.worlds.WorldBackdrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialBombColor
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialLightningColor
import com.mohamedelhajri.starpuzzle.ui.theme.SpecialStarColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/** Coin cost of the MOVE booster (lift the last placed piece back). */
private const val MOVE_COST = 15

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

    // clear animation
    var clearAnimCells by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    val clearProgress = remember { Animatable(1f) }

    // combo popup
    var comboPopup by remember { mutableStateOf(0) }
    val comboPopupAlpha = remember { Animatable(0f) }

    var resultShown by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableStateOf(0) }
    var earnedCoins by remember { mutableStateOf(0) }

    BackHandler { onExit() }

    // timer + frame redraw loop
    LaunchedEffect(liveSession) {
        var last = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            liveSession.tick(dt)
            // Redraw per tick only when a tick-driven visual exists (timed
            // levels' progress bar). Placements bump `frame` themselves,
            // so other levels no longer recompose 60-120x per second.
            if (level.timeLimit > 0) frame++
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
                sound.play(SoundManager.Sfx.COMBO)
            }
        }
        dragIndex = -1
    }

    LaunchedEffect(liveSession.status) {
        if (liveSession.status != GameSession.Status.PLAYING && !resultShown) {
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
                val recorded = progress.recordLevelResult(level.id, stars, level.rewardCoins)
                earnedCoins = if (recorded) level.rewardCoins else 0
                if (daily) progress.markDailyDone(LevelCatalog.dailyKey())
                sound.play(SoundManager.Sfx.COIN)
            } else {
                sound.play(SoundManager.Sfx.GAME_OVER)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        WorldBackdrop(
            world = level.world,
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
                Spacer(Modifier.height(8.dp))
                val objProgress = min(
                    1f,
                    liveSession.objectiveProgress().toFloat() / level.target().coerceAtLeast(1)
                )
                LinearProgressIndicator(
                    progress = { objProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                    clearAnimCells = clearAnimCells,
                    clearProgress = clearProgress.value,
                    dragIndex = dragIndex,
                    dragValid = dragValid,
                    dragTargetX = dragTargetX,
                    dragTargetY = dragTargetY,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── MOVE booster: lift the last placed piece back ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        if (liveSession.takeBackLast() != null) {
                            progress.spendCoins(MOVE_COST)
                            sound.play(SoundManager.Sfx.COIN)
                            frame++
                        }
                    },
                    enabled = liveSession.status == GameSession.Status.PLAYING &&
                            liveSession.canTakeBack() &&
                            progress.coins >= MOVE_COST,
                    border = BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("MOVE LAST · $MOVE_COST", style = MaterialTheme.typography.labelMedium)
                }
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
        if (dragIndex >= 0) {
            val heldPiece = liveSession.tray.getOrNull(dragIndex)
            if (heldPiece != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val px = dragPos.x - boardPx * heldPiece.cellCols * 0.5f
                    val py = dragPos.y - boardPx * heldPiece.cellRows * 0.5f - boardPx * 1.2f
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.13f),
                        radius = boardPx * 1.1f,
                        center = Offset(
                            px + heldPiece.cellCols * boardPx * 0.5f,
                            py + heldPiece.cellRows * boardPx * 0.5f
                        )
                    )
                    drawPiece(heldPiece, Offset(px, py), boardPx, alpha = 0.95f)
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
                        onNext = if (liveSession.status == GameSession.Status.WON &&
                            level.id < LevelCatalog.TOTAL_LEVELS)
                            ({ onNext(level.id + 1) }) else null
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
    clearAnimCells: List<Pair<Int, Int>>,
    clearProgress: Float,
    dragIndex: Int,
    dragValid: Boolean,
    dragTargetX: Int,
    dragTargetY: Int,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
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

        // filled cells
        for (y in 0 until n)
            for (x in 0 until n) {
                val color = session.board.colorAt(x, y)
                if (color >= 0) {
                    drawCellShape(
                        pieceColor(color),
                        Offset(x * cell + gap / 2, y * cell + gap / 2),
                        cell - gap
                    )
                }
            }

        // clear animation
        if (clearProgress < 1f) {
            val s = 1f - clearProgress
            for ((x, y) in clearAnimCells) {
                val cellPx = (cell - gap) * s
                val off = (cell - cellPx) / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f * (1f - clearProgress)),
                    topLeft = Offset(x * cell + off, y * cell + off),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(cellPx * 0.22f)
                )
            }
        }
    }
}

/** A colored cell: rounded square with a soft drop shadow and top light. */
private fun DrawScope.drawCellShape(color: Color, topLeft: Offset, cellSize: Float) {
    val r = CornerRadius(cellSize * 0.22f)
    drawRoundRect(
        color = color.copy(alpha = 0.35f),
        topLeft = topLeft.copy(y = topLeft.y + cellSize * 0.10f),
        size = Size(cellSize, cellSize),
        cornerRadius = r
    )
    drawRoundRect(color, topLeft, Size(cellSize, cellSize), r)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.20f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize * 0.45f),
        cornerRadius = CornerRadius(cellSize * 0.22f, cellSize * 0.30f)
    )
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
    val color = pieceColor(piece.colorIndex).copy(alpha = alpha)
    val gap = cellSize * 0.08f
    val r = CornerRadius((cellSize - gap) * 0.22f)
    for (i in 0 until piece.cellRows)
        for (j in 0 until piece.cellCols)
            if (piece.filled(i, j)) {
                val tl = Offset(
                    origin.x + j * cellSize + gap / 2,
                    origin.y + i * cellSize + gap / 2
                )
                val s = cellSize - gap
                drawRoundRect(color, tl, Size(s, s), r)
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
    onNext: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (won) "LEVEL COMPLETE!" else "LEVEL FAILED",
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
        if (onNext != null) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("NEXT LEVEL") }
            Spacer(Modifier.height(8.dp))
        }
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
}
