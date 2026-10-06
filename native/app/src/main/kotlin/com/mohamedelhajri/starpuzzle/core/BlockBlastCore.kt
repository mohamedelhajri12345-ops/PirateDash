package com.mohamedelhajri.starpuzzle.core

/**
 * BlockBlastCore — pure game logic of the native Block Blast mode.
 *
 * Mechanics ported 1:1 from the owner's original reference file
 * (block-blast-clone/game.js):
 *  - 8x8 board
 *  - 26-shape catalog (exact shapes, same order)
 *  - scoring: +1 point per placed cell, +16 points per cleared
 *    line (8 cells x 2), combo when 2+ lines clear at once
 *  - tray of 3 pieces, refilled when all 3 are placed
 *  - game over when none of the remaining pieces fits anywhere
 *
 * No Android dependencies — fully unit-testable.
 */
object BlockBlastSpec {
    const val SIZE = 8
    const val POINTS_PER_LINE = 16 // 8 cells x 2
    const val TRAY_SIZE = 3
    const val COINS_PER_LINE = 5

    /** Block palette — EXACTLY the owner's original file. */
    val COLORS = listOf(
        0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(), 0xFF45B7D1.toInt(), 0xFF96CEB4.toInt(),
        0xFFFFEAA7.toInt(), 0xFFDDA0DD.toInt(), 0xFFFF8C42.toInt(), 0xFF74b9ff.toInt()
    )

    /** The 26 shapes, exactly as in the original file (1 = filled). */
    val SHAPES: List<List<IntArray>> = listOf(
        listOf(intArrayOf(1)),
        listOf(intArrayOf(1, 1)),
        listOf(intArrayOf(1, 1, 1)),
        listOf(intArrayOf(1, 1, 1, 1)),
        listOf(intArrayOf(1, 1, 1, 1, 1)),
        listOf(intArrayOf(1), intArrayOf(1)),
        listOf(intArrayOf(1), intArrayOf(1), intArrayOf(1)),
        listOf(intArrayOf(1), intArrayOf(1), intArrayOf(1), intArrayOf(1)),
        listOf(intArrayOf(1), intArrayOf(1), intArrayOf(1), intArrayOf(1), intArrayOf(1)),
        listOf(intArrayOf(1, 1), intArrayOf(1, 1)),
        listOf(intArrayOf(1, 1, 1), intArrayOf(1, 1, 1), intArrayOf(1, 1, 1)),
        listOf(intArrayOf(1, 0), intArrayOf(1, 0), intArrayOf(1, 1)),
        listOf(intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(1, 1)),
        listOf(intArrayOf(1, 1, 1), intArrayOf(0, 1, 0)),
        listOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
        listOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
        listOf(intArrayOf(1, 0), intArrayOf(1, 1)),
        listOf(intArrayOf(0, 1), intArrayOf(1, 1)),
        listOf(intArrayOf(1, 1), intArrayOf(1, 0)),
        listOf(intArrayOf(1, 1), intArrayOf(0, 1)),
        listOf(intArrayOf(1, 1, 1), intArrayOf(1, 0, 0)),
        listOf(intArrayOf(1, 1, 1), intArrayOf(0, 0, 1)),
        listOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
        listOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)),
        listOf(intArrayOf(1, 1), intArrayOf(1, 1), intArrayOf(1, 1)),
        listOf(intArrayOf(1, 1, 1), intArrayOf(1, 1, 1))
    )
}

/** One piece offered in the tray. */
data class BlockPiece(val shape: List<IntArray>, val color: Int) {
    val rows: Int get() = shape.size
    val cols: Int get() = shape[0].size
    val cellCount: Int get() = shape.sumOf { row -> row.count { it == 1 } }
}

/** Result of a placement. */
data class PlaceResult(val cellsPlaced: Int, val clearedLines: Int) {
    val scoreGained: Int get() = cellsPlaced + clearedLines * BlockBlastSpec.POINTS_PER_LINE
}

class BlockBlastCore {

    /** Board colors; null = empty. */
    val grid = Array(BlockBlastSpec.SIZE) { arrayOfNulls<Int>(BlockBlastSpec.SIZE) }

    var score = 0
        private set

    /** Pieces currently in the tray; null slot = already placed. */
    var tray = listOf<BlockPiece?>()
        private set

    var isGameOver = false
        private set

    /** Lines cleared by the latest placement (0, 1, 2+ = combo). */
    var lastClearedLines = 0
        private set

    fun reset() {
        for (r in grid.indices) java.util.Arrays.fill(grid[r], null)
        score = 0
        isGameOver = false
        lastClearedLines = 0
        refillTray()
    }

    fun refillTray() {
        tray = (0 until BlockBlastSpec.TRAY_SIZE).map {
            BlockPiece(
                shape = BlockBlastSpec.SHAPES[RandomSource.nextInt(BlockBlastSpec.SHAPES.size)],
                color = BlockBlastSpec.COLORS[RandomSource.nextInt(BlockBlastSpec.COLORS.size)]
            )
        }
    }

    fun canPlace(piece: BlockPiece, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0) return false
        if (row + piece.rows > BlockBlastSpec.SIZE) return false
        if (col + piece.cols > BlockBlastSpec.SIZE) return false
        for (r in 0 until piece.rows) {
            for (c in 0 until piece.cols) {
                if (piece.shape[r][c] == 1 && grid[row + r][col + c] != null) return false
            }
        }
        return true
    }

    /** Whether the piece fits anywhere on the board. */
    fun fitsAnywhere(piece: BlockPiece): Boolean {
        for (r in 0 until BlockBlastSpec.SIZE) {
            for (c in 0 until BlockBlastSpec.SIZE) {
                if (canPlace(piece, r, c)) return true
            }
        }
        return false
    }

    /**
     * Place a tray piece. Awards score, clears full lines, marks the
     * slot used, refills the tray when all 3 are placed, and checks
     * game over. Returns null if the move is illegal.
     */
    fun place(trayIndex: Int, row: Int, col: Int): PlaceResult? {
        if (isGameOver) return null
        val piece = tray.getOrNull(trayIndex) ?: return null
        if (!canPlace(piece, row, col)) return null

        for (r in 0 until piece.rows) {
            for (c in 0 until piece.cols) {
                if (piece.shape[r][c] == 1) grid[row + r][col + c] = piece.color
            }
        }

        val cleared = clearLines()
        score += piece.cellCount + cleared * BlockBlastSpec.POINTS_PER_LINE
        lastClearedLines = cleared

        val newTray = tray.toMutableList()
        newTray[trayIndex] = null
        tray = newTray
        if (tray.all { it == null }) refillTray()

        checkGameOver()
        return PlaceResult(piece.cellCount, cleared)
    }

    private fun clearLines(): Int {
        val fullRows = (0 until BlockBlastSpec.SIZE).filter { r ->
            (0 until BlockBlastSpec.SIZE).all { c -> grid[r][c] != null }
        }
        val fullCols = (0 until BlockBlastSpec.SIZE).filter { c ->
            (0 until BlockBlastSpec.SIZE).all { r -> grid[r][c] != null }
        }
        for (r in fullRows) for (c in 0 until BlockBlastSpec.SIZE) grid[r][c] = null
        for (c in fullCols) for (r in 0 until BlockBlastSpec.SIZE) grid[r][c] = null
        return fullRows.size + fullCols.size
    }

    /** Game over when none of the remaining tray pieces fits anywhere. */
    fun checkGameOver(): Boolean {
        isGameOver = tray.none { it != null && fitsAnywhere(it) }
        return isGameOver
    }
}

/** Injectable RNG so unit tests are deterministic. */
object RandomSource {
    var nextInt: (Int) -> Int = { bound -> kotlin.random.Random.nextInt(bound) }
}
