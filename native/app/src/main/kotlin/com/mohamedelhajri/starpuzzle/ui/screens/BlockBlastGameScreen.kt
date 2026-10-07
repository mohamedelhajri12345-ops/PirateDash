package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.R
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.BlockBlastCore
import com.mohamedelhajri.starpuzzle.core.BlockBlastMissions
import com.mohamedelhajri.starpuzzle.core.BlockBlastSpec
import com.mohamedelhajri.starpuzzle.core.StarThemes
import com.mohamedelhajri.starpuzzle.core.BlockPiece
import com.mohamedelhajri.starpuzzle.core.ClearedCell
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// Real Block Blast colors, measured from the official Play Store screenshots.
val BbBackground get() = StarThemes.active.bgTop
val BbPanel get() = StarThemes.active.board
val BbCell get() = StarThemes.active.cell
val BbTextSoft = Color(0xFF9AA5CE)
val BbGold = Color(0xFFFFD34E)
val BbAccent get() = StarThemes.active.accent

/** Best score & coins persistence, implemented by the host activity. */
interface BbPersistence {
    fun loadBest(): Int
    fun loadCoins(): Int
    fun saveBest(value: Int)
    fun saveCoins(value: Int)
    fun loadGems(): Int
    fun saveGems(value: Int)
    fun loadTotalLines(): Int
    fun saveTotalLines(value: Int)
}

// ---------------- particle burst ----------------
/** Cleared cells kept for the burst effect; particle motion is
 *  derived deterministically from the cell coords, so no per-frame
 *  state is needed. */
data class BurstSeed(val clearedCells: List<ClearedCell>, val lines: Int, val gained: Int = 0)

/** Lightning strike visual (§25). */
data class LightningSeed(val cells: List<ClearedCell>, val isRow: Boolean, val index: Int)

/** Deterministic pseudo-random from a cell + particle index (0..1). */
private fun hash01(r: Int, c: Int, i: Int): Float {
    val x = (r * 73856093) xor (c * 19349663) xor (i * 83492791)
    return ((x ushr 8) and 0xFFFF).toFloat() / 65535f
}

/**
 * Native Block Blast game screen — 100% Kotlin/Compose, no WebView.
 *
 * Owner tuning (Oct 6 2026): vivid BB-style block skins, particle
 * bursts + score popup + combo banner on line clears, NO background
 * music (original in-house soundtrack added in v8.5.0), more forgiving drag & drop (easy
 * pickup, clamped snapping), haptics and the reference SFX set.
 */
@Composable
fun BlockBlastGameScreen(
    soundManager: SoundManager,
    persistence: BbPersistence,
    onExit: () -> Unit,
    onGameOver: (score: Int, best: Int, coinsEarned: Int) -> Unit,
    dailySeed: Long? = null,
    dailyGoal: Int = 0,
    onDailyComplete: () -> Unit = {}
) {
    val core = remember { BlockBlastCore() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // Tintable cell textures ported from the owner's reference package.
    val appContext = androidx.compose.ui.platform.LocalContext.current
    fun cellTex(res: Int): ImageBitmap =
        android.graphics.BitmapFactory.decodeResource(appContext.resources, res).asImageBitmap()
    val cellTextures = mapOf(
        1 to R.drawable.bb_cell_basic,
        2 to R.drawable.bb_cell_bubble,
        3 to R.drawable.bb_cell_bulb,
        4 to R.drawable.bb_cell_circle,
        5 to R.drawable.bb_cell_drop,
        6 to R.drawable.bb_cell_ghost,
        7 to R.drawable.bb_cell_grass,
        8 to R.drawable.bb_cell_leaf,
        9 to R.drawable.bb_cell_snowflake,
        10 to R.drawable.bb_cell_sun
    ).mapValues { cellTex(it.value) }
    val texture = cellTextures[BlockBlastSpec.activeCellSkinId]

    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(0) }
    var gems by remember { mutableIntStateOf(0) }
    var coinsEarned by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) } // triggers redraw after each move

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val pickupAnim = remember { Animatable(1f) }

    // clear-line burst effects
    var burstSeed by remember { mutableStateOf<BurstSeed?>(null) }
    val burstProgress = remember { Animatable(1f) }
    var comboCount by remember { mutableIntStateOf(0) }
    var newBestShown by remember { mutableStateOf(false) }
    var dailyDone by remember { mutableStateOf(false) }
    var lightningSeed by remember { mutableStateOf<LightningSeed?>(null) }
    val lightningProgress = remember { Animatable(1f) }
    val newBestAnim = remember { Animatable(1f) }
    val comboAnim = remember { Animatable(0f) }

    var gameOverFired by remember { mutableStateOf(false) }

    fun commitPlacement(trayIndex: Int, row: Int, col: Int) {
        val result = core.place(trayIndex, row, col) ?: return
        version++
        score = core.score
        BlockBlastMissions.onPlaced(result.cellsPlaced, result.clearedLines, core.score)
        if (result.clearedLines > 0) {
            val earned = result.clearedLines * BlockBlastSpec.COINS_PER_LINE
            coins += earned
            coinsEarned += earned
            persistence.saveCoins(coins)
            persistence.saveTotalLines(persistence.loadTotalLines() + result.clearedLines)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            soundManager.play(SoundManager.Sfx.CLEAR)
            if (result.comboStreak >= 2) {
                soundManager.play(SoundManager.Sfx.COMBO)
                comboCount = result.comboStreak
                scope.launch { comboAnim.snapTo(0f); comboAnim.animateTo(1f, tween(500)) }
            }
            soundManager.play(SoundManager.Sfx.COIN)
            // reference-package clear effect + its own SFX
            when (BlockBlastSpec.activeClearEffectId) {
                1 -> soundManager.play(SoundManager.Sfx.EFFECT_SPIN)
                2 -> soundManager.play(SoundManager.Sfx.EFFECT_WATERDROP)
                3 -> soundManager.play(SoundManager.Sfx.EFFECT_EVAPORATE)
                4 -> soundManager.play(SoundManager.Sfx.EFFECT_VANISH)
                else -> soundManager.play(SoundManager.Sfx.EFFECT_EXPLODE)
            }
            burstSeed = BurstSeed(result.clearedCells, result.clearedLines, result.gained)
            scope.launch { burstProgress.snapTo(0f) }
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            soundManager.play(SoundManager.Sfx.PLACE)
        }
        if (!newBestShown && best > 0 && score > best) {
            newBestShown = true
            soundManager.play(SoundManager.Sfx.HIGH_SCORE)
            scope.launch { newBestAnim.snapTo(0f); newBestAnim.animateTo(1f, tween(700)) }
        }
        if (score > best) {
            best = score
            persistence.saveBest(best)
        }
        if (dailyGoal > 0 && !dailyDone && score >= dailyGoal) {
            dailyDone = true
            soundManager.play(SoundManager.Sfx.COMPLETE)
            onDailyComplete()
        }
        if (core.isGameOver && !gameOverFired) {
            gameOverFired = true
            soundManager.play(SoundManager.Sfx.GAME_OVER)
            onGameOver(core.score, best, coinsEarned)
        }
    }

    LaunchedEffect(Unit) {
        best = persistence.loadBest()
        coins = persistence.loadCoins()
        gems = persistence.loadGems()
        dailySeed?.let { core.seedRng(it) }
        soundManager.playMusic(com.mohamedelhajri.starpuzzle.R.raw.game_theme)
        core.reset()
        score = 0
        coinsEarned = 0
        gameOverFired = false
        version++
    }

    // drive the burst animation
    LaunchedEffect(version) {
        if (burstProgress.value < 1f) {
            burstProgress.animateTo(1f, tween(550))
        }
    }

    // pickup scale (the real game grows the piece when lifted)
    LaunchedEffect(draggingIndex) {
        if (draggingIndex >= 0) pickupAnim.animateTo(1.25f, tween(90))
        else pickupAnim.snapTo(1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // ---------------- TOP BAR ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\u2699",
                color = BbTextSoft,
                fontSize = 26.sp,
                modifier = Modifier.clickable {
                    soundManager.play(SoundManager.Sfx.BACK)
                    onExit()
                }
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    score.toString(),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2605", color = BbGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("  BEST $best", color = BbTextSoft, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.weight(1f))
            BbCoinChip(coins)
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- BOARD + TRAY CANVAS ----------------
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val g = geometry(size.width.toFloat(), size.height.toFloat())
                            val idx = g.trayIndexAt(offset)
                            if (idx >= 0 && core.tray.getOrNull(idx) != null) {
                                draggingIndex = idx
                                dragPos = offset
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                soundManager.play(SoundManager.Sfx.PICKUP)
                            }
                        },
                        onDrag = { change, _ ->
                            dragPos = change.position
                        },
                        onDragEnd = {
                            val g = geometry(size.width.toFloat(), size.height.toFloat())
                            val piece = core.tray.getOrNull(draggingIndex)
                            val lifted = Offset(dragPos.x, dragPos.y - g.cell * 1.6f)
                            val rc = g.boardAnchor(lifted, piece, core)
                            if (rc != null) commitPlacement(draggingIndex, rc.first, rc.second)
                            else {
                                // invalid release: audible + tactile rejection (§9)
                                soundManager.play(SoundManager.Sfx.INVALID)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            draggingIndex = -1
                        },
                        onDragCancel = { draggingIndex = -1 }
                    )
                }
        ) {
            val g = geometry(size.width, size.height)

            // ---- board container panel ----
            drawRoundRect(
                color = BbPanel,
                topLeft = Offset(g.boardX, g.boardY),
                size = Size(g.boardSide, g.boardSide),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // ---- 8x8 cells ----
            for (r in 0 until BlockBlastSpec.SIZE) {
                for (c in 0 until BlockBlastSpec.SIZE) {
                    val x = g.boardX + g.pad + c * (g.cell + g.gap)
                    val y = g.boardY + g.pad + r * (g.cell + g.gap)
                    val color = core.grid[r][c]
                    if (color != null) {
                        drawBbBlock(color, x, y, g.cell, 0f, texture)
                    } else {
                        drawRoundRect(
                            color = BbCell,
                            topLeft = Offset(x, y),
                            size = Size(g.cell, g.cell),
                            cornerRadius = CornerRadius(g.cell * 0.16f)
                        )
                    }
                }
            }

            // ---- drop preview ghost ----
            val piece = core.tray.getOrNull(draggingIndex)
            if (draggingIndex >= 0 && piece != null) {
                val lifted = Offset(dragPos.x, dragPos.y - g.cell * 1.6f)
                val rc = g.boardAnchor(lifted, piece, core)
                if (rc != null) {
                    // predicted clear-lines glow (BB lights up the lines that WILL explode)
                    val occupied = Array(BlockBlastSpec.SIZE) { r ->
                        Array(BlockBlastSpec.SIZE) { c -> core.grid[r][c] != null }
                    }
                    for (pr in 0 until piece.rows) for (pc in 0 until piece.cols) {
                        if (piece.shape[pr][pc] == 1) occupied[rc.first + pr][rc.second + pc] = true
                    }
                    val glow = Color.White.copy(alpha = 0.14f)
                    for (r in 0 until BlockBlastSpec.SIZE) {
                        if ((0 until BlockBlastSpec.SIZE).all { occupied[r][it] }) {
                            drawRoundRect(
                                color = glow,
                                topLeft = Offset(g.boardX + g.pad / 2f, g.boardY + g.pad + r * (g.cell + g.gap)),
                                size = Size(g.boardSide - g.pad, g.cell),
                                cornerRadius = CornerRadius(g.cell * 0.16f)
                            )
                        }
                    }
                    for (c in 0 until BlockBlastSpec.SIZE) {
                        if ((0 until BlockBlastSpec.SIZE).all { occupied[it][c] }) {
                            drawRoundRect(
                                color = glow,
                                topLeft = Offset(g.boardX + g.pad + c * (g.cell + g.gap), g.boardY + g.pad / 2f),
                                size = Size(g.cell, g.boardSide - g.pad),
                                cornerRadius = CornerRadius(g.cell * 0.16f)
                            )
                        }
                    }
                    for (pr in 0 until piece.rows) {
                        for (pc in 0 until piece.cols) {
                            if (piece.shape[pr][pc] == 1) {
                                val x = g.boardX + g.pad + (rc.second + pc) * (g.cell + g.gap)
                                val y = g.boardY + g.pad + (rc.first + pr) * (g.cell + g.gap)
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.35f),
                                    topLeft = Offset(x, y),
                                    size = Size(g.cell, g.cell),
                                    cornerRadius = CornerRadius(g.cell * 0.16f)
                                )
                            }
                        }
                    }
                }
            }

            // ---- tray slots ----
            for (i in 0 until BlockBlastSpec.TRAY_SIZE) {
                val sx = g.trayX + i * (g.slotW + g.slotGap)
                drawRoundRect(
                    color = BbCell,
                    topLeft = Offset(sx, g.trayY),
                    size = Size(g.slotW, g.slotH),
                    cornerRadius = CornerRadius(28f, 28f)
                )
                val p = core.tray.getOrNull(i)
                if (p != null && i != draggingIndex) {
                    drawPiece(p, sx, g.trayY, g.slotW, g.slotH, g.previewCell, texture)
                }
            }

            // ---- line-clear effect (ported from the reference package) ----
            val t = burstProgress.value
            val seed = burstSeed
            if (t < 1f && seed != null && seed.clearedCells.isNotEmpty()) {
                val effId = BlockBlastSpec.activeClearEffectId
                val popupXAvg = seed.clearedCells.map { g.boardX + g.pad + (it.col + 0.5f) * (g.cell + g.gap) }.average().toFloat()
                val popupYAvg = seed.clearedCells.map { g.boardY + g.pad + (it.row + 0.5f) * (g.cell + g.gap) }.average().toFloat()
                val fade = (1f - t).coerceIn(0f, 1f)

                if (effId == 0) {
                    // EXPLODE: shards burst out of every cleared cell
                    for (cell in seed.clearedCells) {
                        val cx = g.boardX + g.pad + (cell.col + 0.5f) * (g.cell + g.gap)
                        val cy = g.boardY + g.pad + (cell.row + 0.5f) * (g.cell + g.gap)
                        for (i in 0 until 6) {
                            val ang = hash01(cell.row, cell.col, i) * 2f * Math.PI.toFloat()
                            val speed = 0.4f + hash01(cell.col, cell.row, i + 7) * 0.8f
                            val px = cx + kotlin.math.cos(ang) * speed * t * 120f
                            val py = cy + kotlin.math.sin(ang) * speed * t * 120f + 40f * t * t
                            drawCircle(
                                color = Color(cell.color.toLong() or 0xFF000000L).copy(alpha = fade),
                                radius = g.cell * 0.16f * (1f - t * 0.6f),
                                center = Offset(px, py)
                            )
                        }
                    }
                } else {
                    // the cleared cells themselves animate away
                    for (cell in seed.clearedCells) {
                        val x = g.boardX + g.pad + cell.col * (g.cell + g.gap)
                        val y = g.boardY + g.pad + cell.row * (g.cell + g.gap)
                        val cx = x + g.cell / 2f
                        val cy = y + g.cell / 2f
                        when (effId) {
                            1 -> rotate(degrees = t * 540f, pivot = Offset(cx, cy)) {
                                val sc = (1f - t).coerceAtLeast(0.05f)
                                drawBbBlock(cell.color, cx - g.cell * sc / 2f, cy - g.cell * sc / 2f,
                                    g.cell * sc, 0f, texture, fade)
                            }
                            2 -> { // WATERDROP: shrinks into the bottom of the cell
                                val sc = (1f - t).coerceAtLeast(0.05f)
                                drawBbBlock(cell.color, cx - g.cell * sc / 2f, y + g.cell - g.cell * sc,
                                    g.cell * sc, 0f, texture, fade)
                            }
                            3 -> { // EVAPORATE: drifts up with a wobble
                                val wob = kotlin.math.sin(t * 6.28f + cell.col) * 6f
                                drawBbBlock(cell.color, x + wob, y - t * 60f, g.cell, 0f, texture, fade)
                            }
                            else -> { // VANISH: pops up then fades
                                val sc = 1f + 0.25f * kotlin.math.sin(t * Math.PI.toFloat())
                                drawBbBlock(cell.color, cx - g.cell * sc / 2f, cy - g.cell * sc / 2f,
                                    g.cell * sc, 0f, texture, fade)
                            }
                        }
                    }
                }

                // floating score popup
                val layout = textMeasurer.measure(
                    "+" + seed.gained,
                    TextStyle(fontSize = (g.cell * 0.9f).toSp(), fontWeight = FontWeight.Black)
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        popupXAvg - layout.size.width / 2f,
                        popupYAvg - layout.size.height / 2f - t * 90f
                    ),
                    alpha = fade
                )

                // coins fly up to the coin chip (reference bonus-particle feel)
                val nCoins = seed.lines * 2
                for (i in 0 until nCoins) {
                    val delay = hash01(i, seed.lines, 3) * 0.3f
                    val ct = if (t <= delay) 0f else ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
                    val sx0 = popupXAvg + (hash01(i, 1, 5) - 0.5f) * 90f
                    val sy0 = popupYAvg + (hash01(i, 2, 6) - 0.5f) * 40f
                    val targetX = size.width - 24f
                    val px = sx0 + (targetX - sx0) * ct
                    val py = sy0 - (sy0 + 24f) * ct
                    if (ct > 0f) {
                        drawCircle(
                            color = BbGold.copy(alpha = (1f - ct * 0.2f)),
                            radius = g.cell * 0.16f,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // ---- LIGHTNING strike VFX (§25): flash + branching energy ----
            lightningSeed?.let { ls ->
                val t = lightningProgress.value
                if (t < 1f) {
                    val alpha = (1f - t)
                    val flash = Color.White.copy(alpha = alpha * 0.45f)
                    if (ls.isRow) {
                        val y = g.boardY + g.pad + ls.index * (g.cell + g.gap) + g.cell / 2f
                        drawRoundRect(
                            color = flash,
                            topLeft = Offset(g.boardX + g.pad, y - g.cell / 2f),
                            size = Size(g.boardSide - g.pad * 2f, g.cell),
                            cornerRadius = CornerRadius(g.cell * 0.16f)
                        )
                        val path = Path()
                        var px = g.boardX + g.pad
                        path.moveTo(px, y)
                        while (px < g.boardX + g.boardSide - g.pad) {
                            px += (g.boardSide - g.pad * 2f) / 7f
                            val jitter = if ((px.toInt() / 31) % 2 == 0) -g.cell * 0.30f else g.cell * 0.30f
                            path.lineTo(px, y + jitter)
                        }
                        drawPath(path, BbAccent.copy(alpha = alpha), style = Stroke(width = g.cell * 0.12f))
                        drawPath(path, Color.White.copy(alpha = alpha * 0.8f), style = Stroke(width = g.cell * 0.05f))
                    } else {
                        val x = g.boardX + g.pad + ls.index * (g.cell + g.gap) + g.cell / 2f
                        drawRoundRect(
                            color = flash,
                            topLeft = Offset(x - g.cell / 2f, g.boardY + g.pad),
                            size = Size(g.cell, g.boardSide - g.pad * 2f),
                            cornerRadius = CornerRadius(g.cell * 0.16f)
                        )
                        val path = Path()
                        var py = g.boardY + g.pad
                        path.moveTo(x, py)
                        while (py < g.boardY + g.boardSide - g.pad) {
                            py += (g.boardSide - g.pad * 2f) / 7f
                            val jitter = if ((py.toInt() / 31) % 2 == 0) -g.cell * 0.30f else g.cell * 0.30f
                            path.lineTo(x + jitter, py)
                        }
                        drawPath(path, BbAccent.copy(alpha = alpha), style = Stroke(width = g.cell * 0.12f))
                        drawPath(path, Color.White.copy(alpha = alpha * 0.8f), style = Stroke(width = g.cell * 0.05f))
                    }
                }
            }

            // ---- BOMB shockwave ring (§23) during any burst ----
            burstSeed?.let { seed ->
                val t = burstProgress.value
                if (t < 1f && seed.cells.isNotEmpty()) {
                    val avg = seed.cells.fold(Offset.Zero) { acc, c ->
                        Offset(acc.x + g.boardX + g.pad + c.col * (g.cell + g.gap) + g.cell / 2f,
                               acc.y + g.boardY + g.pad + c.row * (g.cell + g.gap) + g.cell / 2f)
                    }.div(seed.cells.size.toFloat())
                    drawCircle(
                        color = BbAccent.copy(alpha = (1f - t) * 0.7f),
                        radius = g.cell * (0.5f + t * 5f),
                        center = avg,
                        style = Stroke(width = g.cell * 0.18f * (1f - t))
                    )
                }
            }

            // ---- dragged piece follows the finger ----
            if (draggingIndex >= 0 && piece != null) {
                val c = g.cell * pickupAnim.value
                val w = piece.cols * c
                val h = piece.rows * c
                drawPiece(piece, dragPos.x - w / 2f, dragPos.y - h / 2f - g.cell * 1.6f, w, h, c, texture)
            }
        }

        // ---------------- BOOSTER ROW ----------------
        // NEW PIECES: free during the testing phase (owner instruction).
        // RETRY & BOOST: paid with gems only (owner: NOT free).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BbGoldPill("RETRY \uD83D\uDC8E${BlockBlastSpec.RETRY_COST_GEMS}") {
                if (draggingIndex < 0 && core.canUndo && gems >= BlockBlastSpec.RETRY_COST_GEMS) {
                    gems -= BlockBlastSpec.RETRY_COST_GEMS
                    persistence.saveGems(gems)
                    core.undoLastMove()
                    version++
                    score = core.score
                    soundManager.play(SoundManager.Sfx.BACK)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    soundManager.play(SoundManager.Sfx.INVALID)
                }
            }
            Spacer(Modifier.width(10.dp))
            BbGoldPill("BOOST \uD83D\uDCA5${BlockBlastSpec.BOOST_COST_GEMS}") {
                if (draggingIndex < 0 && gems >= BlockBlastSpec.BOOST_COST_GEMS) {
                    val hasAnyBlock = core.grid.any { row -> row.any { it != null } }
                    if (hasAnyBlock) {
                        gems -= BlockBlastSpec.BOOST_COST_GEMS
                        persistence.saveGems(gems)
                        val cells = core.useBombBoost()
                        version++
                        burstSeed = BurstSeed(cells, 0)
                        scope.launch { burstProgress.snapTo(0f) }
                        soundManager.play(SoundManager.Sfx.BOMB)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        soundManager.play(SoundManager.Sfx.INVALID)
                    }
                } else {
                    soundManager.play(SoundManager.Sfx.INVALID)
                }
            }
            Spacer(Modifier.width(10.dp))
            BbGoldPill("LIGHTNING \u26A1${BlockBlastSpec.LIGHTNING_COST_GEMS}") {
                if (draggingIndex < 0 && gems >= BlockBlastSpec.LIGHTNING_COST_GEMS) {
                    val hasAnyBlock = core.grid.any { row -> row.any { it != null } }
                    if (hasAnyBlock) {
                        gems -= BlockBlastSpec.LIGHTNING_COST_GEMS
                        persistence.saveGems(gems)
                        val (cells, isRow, idx) = core.useLightning()
                        score = core.score
                        version++
                        lightningSeed = LightningSeed(cells, isRow, idx)
                        burstSeed = BurstSeed(cells, 1, cells.size * 12)
                        scope.launch { burstProgress.snapTo(0f) }
                        scope.launch { lightningProgress.snapTo(0f); lightningProgress.animateTo(1f, tween(450)) }
                        soundManager.play(SoundManager.Sfx.BOMB)
                        soundManager.play(SoundManager.Sfx.EFFECT_SPIN)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        soundManager.play(SoundManager.Sfx.INVALID)
                    }
                } else {
                    soundManager.play(SoundManager.Sfx.INVALID)
                }
            }
            Spacer(Modifier.width(10.dp))
            BbGoldPill("NEW PIECES") {
                if (draggingIndex < 0) {
                    core.rerollTray()
                    version++
                    soundManager.play(SoundManager.Sfx.CONFIRM)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
        }
    }

    // ---------------- NEW BEST celebration (§18) ----------------
    if (newBestAnim.value < 1f) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                "\u2B50 NEW BEST! \u2B50",
                color = BbGold,
                fontSize = (20 + 8 * (1f - newBestAnim.value)).sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .padding(top = 60.dp)
                    .alpha(newBestAnim.value.coerceIn(0f, 1f))
            )
        }
    }

    // ---------------- COMBO BANNER overlay ----------------
    if (comboCount > 0 && comboAnim.value < 1f) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "COMBO \u00D7$comboCount",
                color = BbGold,
                fontSize = (34 + 10 * (1f - comboAnim.value)).sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.alpha(comboAnim.value)
            )
        }
    }
}

// ----------------------------------------------------------------
// Geometry — all in canvas pixels, computed from the canvas size
// ----------------------------------------------------------------
private data class GameGeometry(
    val boardX: Float, val boardY: Float, val boardSide: Float,
    val pad: Float, val cell: Float, val gap: Float,
    val trayX: Float, val trayY: Float, val slotW: Float, val slotH: Float,
    val slotGap: Float, val previewCell: Float
) {
    /** Easy pickup: generous hitbox around each tray slot. */
    fun trayIndexAt(offset: Offset): Int {
        val slack = 90f
        if (offset.y < trayY - slack || offset.y > trayY + slotH + slack) return -1
        val rel = offset.x - trayX
        if (rel < -slotW / 2f) return -1
        val idx = ((rel + slotW / 2f) / (slotW + slotGap)).toInt()
        return if (idx in 0..2) idx else -1
    }

    /**
     * Anchor (row, col) when the piece is centered on the finger.
     * Forgiving snapping: the raw cell is clamped into the valid
     * range before the fit check, so drops near the border land.
     */
    fun boardAnchor(pos: Offset, piece: BlockPiece?, core: BlockBlastCore): Pair<Int, Int>? {
        if (piece == null) return null
        val rawCol = ((pos.x - boardX - pad - piece.cols * (cell + gap) / 2f) / (cell + gap))
        val rawRow = ((pos.y - boardY - pad - piece.rows * (cell + gap) / 2f) / (cell + gap))
        val col = min(max(rawCol.toInt(), 0), BlockBlastSpec.SIZE - piece.cols)
        val row = min(max(rawRow.toInt(), 0), BlockBlastSpec.SIZE - piece.rows)
        if (core.canPlace(piece, row, col)) return row to col
        return null
    }
}

private fun geometry(widthPx: Float, heightPx: Float): GameGeometry {
    val margin = 10f
    val padPx = 12f
    val gapPx = 3f
    val slotGap = 14f
    // bottom = 16.15 + 1.425 * boardSide  (tray sits below the board)
    val byHeight = (heightPx - 16.15f) / 1.425f
    val boardSide = minOf(widthPx - margin * 2f, byHeight)
    val cell = (boardSide - padPx * 2f - gapPx * 7f) / 8f
    val boardY = margin
    val slotW = (widthPx - margin * 2f - slotGap * 2f) / 3f
    val slotH = cell * 3.4f
    val trayY = boardY + boardSide + slotGap + 10f
    return GameGeometry(
        boardX = margin, boardY = boardY, boardSide = boardSide,
        pad = padPx, cell = cell, gap = gapPx,
        trayX = margin, trayY = trayY, slotW = slotW, slotH = slotH,
        slotGap = slotGap, previewCell = cell * 0.62f
    )
}

// ----------------------------------------------------------------
// Drawing
// ----------------------------------------------------------------
private fun DrawScope.drawPiece(
    piece: BlockPiece,
    x: Float, y: Float, boxW: Float, boxH: Float, cellPx: Float,
    texture: ImageBitmap? = null
) {
    val w = piece.cols * cellPx
    val h = piece.rows * cellPx
    val ox = x + (boxW - w) / 2f
    val oy = y + (boxH - h) / 2f
    for (r in 0 until piece.rows) {
        for (c in 0 until piece.cols) {
            if (piece.shape[r][c] == 1) {
                drawBbBlock(piece.color, ox + c * cellPx, oy + r * cellPx, cellPx - 2f, 0f, texture)
            }
        }
    }
}

/** Glossy 3D block like the real game: dark bottom edge, main face, glossy top.
 *  When a reference texture is active the texture is multiply-tinted
 *  with the piece color instead. */
private fun DrawScope.drawBbBlock(
    color: Int, x: Float, y: Float, cell: Float, flashAlpha: Float,
    texture: ImageBitmap? = null, alpha: Float = 1f
) {
    if (texture != null) {
        drawImage(
            image = texture,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(texture.width, texture.height),
            dstOffset = IntOffset(x.toInt(), y.toInt()),
            dstSize = IntSize(cell.toInt().coerceAtLeast(1), cell.toInt().coerceAtLeast(1)),
            alpha = alpha,
            colorFilter = ColorFilter.tint(Color(color.toLong() or 0xFF000000L), BlendMode.Multiply)
        )
        return
    }
    val c = Color(color.toLong() or 0xFF000000L)
    val corner = CornerRadius(cell * 0.16f, cell * 0.16f)
    val edge = maxOf(1.5f, cell * 0.10f)
    drawRoundRect(
        color = Color(c.red * 0.62f, c.green * 0.62f, c.blue * 0.62f),
        topLeft = Offset(x, y),
        size = Size(cell, cell),
        cornerRadius = corner
    )
    drawRoundRect(
        color = c,
        topLeft = Offset(x, y),
        size = Size(cell, cell - edge),
        cornerRadius = corner
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f + flashAlpha * 0.35f),
        topLeft = Offset(x + cell * 0.06f, y + cell * 0.05f),
        size = Size(cell * 0.88f, cell * 0.42f),
        cornerRadius = CornerRadius(cell * 0.10f)
    )
}

// ----------------------------------------------------------------
// Shared chips & buttons
// ----------------------------------------------------------------
@Composable
fun BbCoinChip(coins: Int, big: Boolean = false) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(start = 6.dp, end = if (big) 16.dp else 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(if (big) 26.dp else 20.dp)
                .background(BbGold, CircleShape)
        )
        Text(
            coins.toString(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (big) 18.sp else 15.sp
        )
    }
}

@Composable
fun BbGoldPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(BbGold, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = Color(0xFF3A2E00),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }
}
