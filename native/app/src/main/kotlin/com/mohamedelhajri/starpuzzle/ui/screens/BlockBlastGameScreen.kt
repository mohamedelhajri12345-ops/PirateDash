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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.BlockBlastCore
import com.mohamedelhajri.starpuzzle.core.BlockBlastSpec
import com.mohamedelhajri.starpuzzle.core.BlockPiece

// Real Block Blast colors, measured from the official Play Store screenshots.
val BbBackground = Color(0xFF242C54)
val BbPanel = Color(0xFF2A3260)
val BbCell = Color(0xFF1E264A)
val BbTextSoft = Color(0xFF9AA5CE)
val BbGold = Color(0xFFFFD34E)

/** Best score & coins persistence, implemented by the host activity. */
interface BbPersistence {
    fun loadBest(): Int
    fun loadCoins(): Int
    fun saveBest(value: Int)
    fun saveCoins(value: Int)
}

/**
 * Native Block Blast game screen — 100% Kotlin/Compose, no WebView.
 *
 * Layout mirrors the real Block Blast game screen: big white score,
 * BEST line with a gold star, coin chip, 8x8 board on a panel,
 * three tray slots, drag & drop with drop preview, pickup scale,
 * haptics and the reference SFX set.
 */
@Composable
fun BlockBlastGameScreen(
    soundManager: SoundManager,
    persistence: BbPersistence,
    onExit: () -> Unit,
    onGameOver: (score: Int, best: Int, coinsEarned: Int) -> Unit
) {
    val core = remember { BlockBlastCore() }
    val haptics = LocalHapticFeedback.current

    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(0) }
    var coinsEarned by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) } // triggers redraw after each move

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val pickupAnim = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }

    var gameOverFired by remember { mutableStateOf(false) }

    fun commitPlacement(trayIndex: Int, row: Int, col: Int) {
        val result = core.place(trayIndex, row, col) ?: return
        version++
        score = core.score
        if (result.clearedLines > 0) {
            val earned = result.clearedLines * BlockBlastSpec.COINS_PER_LINE
            coins += earned
            coinsEarned += earned
            persistence.saveCoins(coins)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (result.clearedLines >= 2) soundManager.play(SoundManager.Sfx.COMBO)
            else soundManager.play(SoundManager.Sfx.CLEAR)
            flash.snapTo(1f)
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            soundManager.play(SoundManager.Sfx.PLACE)
        }
        if (score > best) {
            best = score
            persistence.saveBest(best)
        }
        if (core.isGameOver && !gameOverFired) {
            gameOverFired = true
            soundManager.play(SoundManager.Sfx.GAME_OVER)
            soundManager.stopMusic()
            onGameOver(core.score, best, coinsEarned)
        }
    }

    LaunchedEffect(Unit) {
        best = persistence.loadBest()
        coins = persistence.loadCoins()
        core.reset()
        score = 0
        coinsEarned = 0
        gameOverFired = false
        version++
        soundManager.play(SoundManager.Sfx.START)
        soundManager.startMusic()
    }

    LaunchedEffect(version) {
        if (flash.value > 0f) flash.animateTo(0f, tween(350))
    }

    // pickup scale animation (the real game grows the piece when lifted)
    LaunchedEffect(draggingIndex) {
        if (draggingIndex >= 0) pickupAnim.animateTo(1.25f, tween(120))
        else pickupAnim.snapTo(1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground)
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
                modifier = Modifier.clickable { onExit() }
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
                            val rc = g.boardAnchor(dragPos, piece, core)
                            if (rc != null) commitPlacement(draggingIndex, rc.first, rc.second)
                            draggingIndex = -1
                        },
                        onDragCancel = { draggingIndex = -1 }
                    )
                }
        ) {
            val g = geometry(size.width, size.height)
            val flashAlpha = flash.value

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
                        drawBbBlock(color, x, y, g.cell, flashAlpha)
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
                val rc = g.boardAnchor(dragPos, piece, core)
                if (rc != null) {
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
                    drawPiece(p, sx, g.trayY, g.slotW, g.slotH, g.previewCell)
                }
            }

            // ---- dragged piece follows the finger ----
            if (draggingIndex >= 0 && piece != null) {
                val c = g.cell * pickupAnim.value
                val w = piece.cols * c
                val h = piece.rows * c
                drawPiece(piece, dragPos.x - w / 2f, dragPos.y - h / 2f, w, h, c)
            }
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
    fun trayIndexAt(offset: Offset): Int {
        if (offset.y < trayY || offset.y > trayY + slotH) return -1
        val rel = offset.x - trayX
        if (rel < 0f) return -1
        val idx = (rel / (slotW + slotGap)).toInt()
        if (idx in 0..2 && rel <= idx * (slotW + slotGap) + slotW) return idx
        return -1
    }

    /** Anchor (row, col) when the piece is centered on the finger. */
    fun boardAnchor(pos: Offset, piece: BlockPiece?, core: BlockBlastCore): Pair<Int, Int>? {
        if (piece == null) return null
        val col = ((pos.x - boardX - pad - piece.cols * (cell + gap) / 2f) / (cell + gap)).toInt()
        val row = ((pos.y - boardY - pad - piece.rows * (cell + gap) / 2f) / (cell + gap)).toInt()
        if (core.canPlace(piece, row, col)) return row to col
        return null
    }
}

private fun geometry(widthPx: Float, heightPx: Float): GameGeometry {
    val margin = 10f
    val padPx = 12f
    val gapPx = 3f
    val slotGap = 14f
    // bottom = 16.15 + 1.425 * boardSide  (derived: tray sits below the board)
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
    x: Float, y: Float, boxW: Float, boxH: Float, cellPx: Float
) {
    val w = piece.cols * cellPx
    val h = piece.rows * cellPx
    val ox = x + (boxW - w) / 2f
    val oy = y + (boxH - h) / 2f
    for (r in 0 until piece.rows) {
        for (c in 0 until piece.cols) {
            if (piece.shape[r][c] == 1) {
                drawBbBlock(piece.color, ox + c * cellPx, oy + r * cellPx, cellPx - 2f, 0f)
            }
        }
    }
}

/** Glossy 3D block like the real game: dark bottom edge, main face, glossy top. */
private fun DrawScope.drawBbBlock(color: Int, x: Float, y: Float, cell: Float, flashAlpha: Float) {
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
// Coin chip (shared with the menu screen)
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
