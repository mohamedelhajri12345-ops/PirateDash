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

    // ---- paid-only boosters (owner: NOT free, cost real gems) ----
    const val GEMS_PER_LEVEL_UP = 0 // reserved
    const val RETRY_COST_GEMS = 15
    const val BOOST_COST_GEMS = 25

    /**
     * Vibrant Block Blast-style block skins (owner request, Oct 6 2026:
     * "vibrant, fun colors like the blue in Block Blast").
     * Index 0 = default, selected skin is persisted in the shop.
     */
    data class Skin(val id: Int, val name: String, val colors: List<Int>)

    val SKINS = listOf(
        Skin(0, "Blast Classic", listOf(
            0xFF4D7CFE.toInt(), 0xFFEF5350.toInt(), 0xFFFFC93C.toInt(), 0xFF66BB6A.toInt(),
            0xFFAB47BC.toInt(), 0xFFFF7043.toInt(), 0xFF26C6DA.toInt(), 0xFFEC407A.toInt()
        )),
        Skin(1, "Ocean Splash", listOf(
            0xFF29B6F6.toInt(), 0xFF26C6DA.toInt(), 0xFF66BB6A.toInt(), 0xFFFFCA28.toInt(),
            0xFF42A5F5.toInt(), 0xFF00ACC1.toInt(), 0xFF7E57C2.toInt(), 0xFFFF7043.toInt()
        )),
        Skin(2, "Candy Party", listOf(
            0xFFEC407A.toInt(), 0xFFFFA726.toInt(), 0xFFAB47BC.toInt(), 0xFFEF5350.toInt(),
            0xFFFFD54F.toInt(), 0xFFFF80AB.toInt(), 0xFF8BC34A.toInt(), 0xFF42A5F5.toInt()
        )),
        Skin(3, "Neon Night", listOf(
            0xFF00E5FF.toInt(), 0xFF7C4DFF.toInt(), 0xFF00E676.toInt(), 0xFFFF4081.toInt(),
            0xFFFFD600.toInt(), 0xFF651FFF.toInt(), 0xFF00BFA5.toInt(), 0xFFFF6D00.toInt()
        ))
    )

    /** Currently selected skin (set by the shop, read by the core). */
    @Volatile var activeSkinId: Int = 0

    val COLORS: List<Int>
        get() = SKINS[activeSkinId].colors

    /**
     * Cell styles — texture cell skins ported from the owner's
     * reference package (96x96 tintable cell textures). Id 0 keeps
     * the current glossy flat look; 1..10 are the reference textures.
     */
    data class CellSkin(val id: Int, val name: String)

    val CELL_SKINS = listOf(
        CellSkin(0, "Glossy"),
        CellSkin(1, "Basic"),
        CellSkin(2, "Bubble"),
        CellSkin(3, "Bulb"),
        CellSkin(4, "Circle"),
        CellSkin(5, "Drop"),
        CellSkin(6, "Ghost"),
        CellSkin(7, "Grass"),
        CellSkin(8, "Leaf"),
        CellSkin(9, "Snowflake"),
        CellSkin(10, "Sun")
    )

    @Volatile var activeCellSkinId: Int = 0

    /**
     * Line-clear visual effects, ported 1:1 from the reference
     * package's effect factories (Explode/Spin/Waterdrop/
     * Evaporate/Vanish) — each with its own SFX.
     */
    data class ClearEffect(val id: Int, val name: String)

    val CLEAR_EFFECTS = listOf(
        ClearEffect(0, "Explode"),
        ClearEffect(1, "Spin"),
        ClearEffect(2, "Waterdrop"),
        ClearEffect(3, "Evaporate"),
        ClearEffect(4, "Vanish")
    )

    @Volatile var activeClearEffectId: Int = 0

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

/** A cell cleared by a placement (pre-clear color kept for the burst effect). */
data class ClearedCell(val row: Int, val col: Int, val color: Int)

/** Result of a placement. */
data class PlaceResult(
    val cellsPlaced: Int,
    val clearedLines: Int,
    val clearedCells: List<ClearedCell> = emptyList(),
    val placedCells: List<ClearedCell> = emptyList()
) {
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

    /** One-level undo snapshot captured right before each successful placement. */
    private data class Snapshot(
        val grid: Array<Array<Int?>>,
        val score: Int,
        val tray: List<BlockPiece?>,
        val lastClearedLines: Int
    )
    private var snapshot: Snapshot? = null

    /** Whether a RETRY (undo last placement) is currently available. */
    val canUndo: Boolean get() = snapshot != null

    /**
     * RETRY booster: restores the board to right before the last
     * placement. One level deep — consumed by use. Paid with gems
     * by the caller (gem spending lives in the UI layer).
     */
    fun undoLastMove(): Boolean {
        val s = snapshot ?: return false
        for (r in grid.indices) {
            for (c in grid[r].indices) grid[r][c] = s.grid[r][c]
        }
        score = s.score
        tray = s.tray
        lastClearedLines = s.lastClearedLines
        isGameOver = false
        snapshot = null
        return true
    }

    /**
     * BOOST booster: detonates a 3x3 bomb centered on the board's
     * most-filled 3x3 area (biggest relief for the player), clearing
     * every cell in it. Paid with gems by the caller.
     */
    fun useBombBoost(): List<ClearedCell> {
        var bestRow = 0
        var bestCol = 0
        var bestFill = -1
        for (r in 0..BlockBlastSpec.SIZE - 3) {
            for (c in 0..BlockBlastSpec.SIZE - 3) {
                var fill = 0
                for (dr in 0 until 3) for (dc in 0 until 3) {
                    if (grid[r + dr][c + dc] != null) fill++
                }
                if (fill > bestFill) {
                    bestFill = fill
                    bestRow = r
                    bestCol = c
                }
            }
        }
        val cleared = mutableListOf<ClearedCell>()
        for (dr in 0 until 3) {
            for (dc in 0 until 3) {
                val r = bestRow + dr
                val c = bestCol + dc
                grid[r][c]?.let { cleared.add(ClearedCell(r, c, it)) }
                grid[r][c] = null
            }
        }
        isGameOver = false
        return cleared
    }

    fun reset() {
        snapshot = null
        for (r in grid.indices) java.util.Arrays.fill(grid[r], null)
        score = 0
        isGameOver = false
        lastClearedLines = 0
        refillTray()
    }

    /** "New Pieces" booster: reroll the remaining tray pieces. */
    fun rerollTray() {
        val remaining = tray.toMutableList()
        for (i in remaining.indices) {
            if (remaining[i] != null) {
                remaining[i] = BlockPiece(
                    shape = BlockBlastSpec.SHAPES[RandomSource.nextInt(BlockBlastSpec.SHAPES.size)],
                    color = BlockBlastSpec.COLORS[RandomSource.nextInt(BlockBlastSpec.COLORS.size)]
                )
            }
        }
        tray = remaining
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

        snapshot = Snapshot(
            grid = Array(BlockBlastSpec.SIZE) { r -> grid[r].copyOf() },
            score = score,
            tray = tray,
            lastClearedLines = lastClearedLines
        )

        val placedCells = mutableListOf<ClearedCell>()
        for (r in 0 until piece.rows) {
            for (c in 0 until piece.cols) {
                if (piece.shape[r][c] == 1) {
                    grid[row + r][col + c] = piece.color
                    placedCells.add(ClearedCell(row + r, col + c, piece.color))
                }
            }
        }

        val (cleared, clearedCells) = clearLines()
        score += piece.cellCount + cleared * BlockBlastSpec.POINTS_PER_LINE
        lastClearedLines = cleared

        val newTray = tray.toMutableList()
        newTray[trayIndex] = null
        tray = newTray
        if (tray.all { it == null }) refillTray()

        checkGameOver()
        return PlaceResult(piece.cellCount, cleared, clearedCells, placedCells)
    }

    private fun clearLines(): Pair<Int, List<ClearedCell>> {
        val fullRows = (0 until BlockBlastSpec.SIZE).filter { r ->
            (0 until BlockBlastSpec.SIZE).all { c -> grid[r][c] != null }
        }
        val fullCols = (0 until BlockBlastSpec.SIZE).filter { c ->
            (0 until BlockBlastSpec.SIZE).all { r -> grid[r][c] != null }
        }
        val cleared = mutableListOf<ClearedCell>()
        for (r in fullRows) for (c in 0 until BlockBlastSpec.SIZE) {
            grid[r][c]?.let { cleared.add(ClearedCell(r, c, it)) }
            grid[r][c] = null
        }
        for (c in fullCols) for (r in 0 until BlockBlastSpec.SIZE) {
            grid[r][c]?.let { cleared.add(ClearedCell(r, c, it)) }
            grid[r][c] = null
        }
        return fullRows.size + fullCols.size to cleared
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
