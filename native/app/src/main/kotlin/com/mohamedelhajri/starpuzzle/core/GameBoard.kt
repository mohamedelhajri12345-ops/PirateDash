package com.mohamedelhajri.starpuzzle.core

// The 10x10 board. Pure Kotlin, fully unit tested.
//
// Invariants that are guarded by tests (these were the v1.x crash causes):
//  - the board only ever stores color indices -1..7 (never special 100+)
//  - power-ups act on the TRUE landing cell recorded by putPiece, never on
//    a re-derived screen position
//  - every clear helper clamps its target: no index can leave the grid
class GameBoard(val size: Int = 10) {

    private val cells: Array<IntArray> = Array(size) { IntArray(size) { -1 } }

    // True landing cell of the last placed piece (single source of truth
    // for power-up targeting)
    var lastPutX = -1
        private set
    var lastPutY = -1
        private set

    fun isEmpty(x: Int, y: Int): Boolean = inBounds(x, y) && cells[y][x] < 0

    fun colorAt(x: Int, y: Int): Int =
        if (inBounds(x, y)) cells[y][x] else -1

    fun setCell(x: Int, y: Int, colorIndex: Int) {
        cells[clamp(y)][clamp(x)] = colorIndex
    }

    private fun inBounds(x: Int, y: Int) = x in 0 until size && y in 0 until size
    private fun clamp(v: Int) = v.coerceIn(0, size - 1)

    /** True if [piece] fits fully inside the board without overlapping. */
    fun canPut(piece: Piece, x: Int, y: Int): Boolean {
        if (!inBounds(x, y)) return false
        if (x + piece.cellCols > size || y + piece.cellRows > size) return false
        for (i in 0 until piece.cellRows)
            for (j in 0 until piece.cellCols)
                if (piece.filled(i, j) && cells[y + i][x + j] >= 0) return false
        return true
    }

    /**
     * Places [piece] at (x, y). Returns false if it doesn't fit.
     * Special power-up pieces do NOT write their special id into the board:
     * the cell stays empty and the caller executes the effect at lastPutX/Y.
     */
    fun putPiece(piece: Piece, x: Int, y: Int): Boolean {
        if (!canPut(piece, x, y)) return false
        lastPutX = x
        lastPutY = y
        if (!piece.isSpecial) {
            for (i in 0 until piece.cellRows)
                for (j in 0 until piece.cellCols)
                    if (piece.filled(i, j)) cells[y + i][x + j] = piece.colorIndex
        }
        return true
    }

    /** All cells filled on row [y] / column [x] (for line detection). */
    private fun rowComplete(y: Int): Boolean = (0 until size).all { cells[y][it] >= 0 }
    private fun colComplete(x: Int): Boolean = (0 until size).all { cells[it][x] >= 0 }

    /** Clears every completed row/column. Returns lines cleared + the cells. */
    fun clearComplete(): ClearResult {
        val clearedRows = (0 until size).filter { rowComplete(it) }
        val clearedCols = (0 until size).filter { colComplete(it) }
        val clearedCells = mutableListOf<Pair<Int, Int>>()
        for (y in clearedRows) for (x in 0 until size) {
            if (cells[y][x] >= 0) clearedCells.add(x to y)
            cells[y][x] = -1
        }
        for (x in clearedCols) for (y in 0 until size) {
            if (cells[y][x] >= 0) { clearedCells.add(x to y); cells[y][x] = -1 }
        }
        return ClearResult(clearedRows.size + clearedCols.size, clearedCells)
    }

    /** Bomb: clears the (2r+1)^2 area centered on the true landing cell. */
    fun clearArea(x: Int, y: Int, radius: Int): List<Pair<Int, Int>> {
        val cx = clamp(x); val cy = clamp(y)
        val out = mutableListOf<Pair<Int, Int>>()
        for (i in maxOf(0, cy - radius)..minOf(size - 1, cy + radius))
            for (j in maxOf(0, cx - radius)..minOf(size - 1, cx + radius))
                if (cells[i][j] >= 0) { out.add(j to i); cells[i][j] = -1 }
        return out
    }

    /** Lightning: clears the full row + column of the true landing cell. */
    fun clearCross(x: Int, y: Int): List<Pair<Int, Int>> {
        val cx = clamp(x); val cy = clamp(y)
        val out = mutableListOf<Pair<Int, Int>>()
        for (j in 0 until size) if (cells[cy][j] >= 0) { out.add(j to cy); cells[cy][j] = -1 }
        for (i in 0 until size) if (cells[i][cx] >= 0) { out.add(cx to i); cells[i][cx] = -1 }
        return out
    }

    fun clearAll() {
        for (y in 0 until size) for (x in 0 until size) cells[y][x] = -1
    }

    /**
     * Deterministic obstacle prefill (level mode). Returns the dirt cells
     * that actually remain on the board (the CLEANUP objective target).
     * Pattern 0 is the classic random scatter; 1-5 draw geometric shapes
     * so every level band looks distinct at a glance.
     */
    fun prefill(seed: Long, densityPercent: Int, pattern: Int = 0): List<Pair<Int, Int>> {
        if (densityPercent <= 0) return emptyList()
        val rng = java.util.Random(seed)
        val target = size * size * densityPercent / 100

        val candidates: List<Pair<Int, Int>> = when (pattern % 6) {
            1 -> { // BORDER: the outer ring
                (0 until size).flatMap { x -> listOf(x to 0, x to size - 1) } +
                        (1 until size - 1).flatMap { y -> listOf(0 to y, size - 1 to y) }
            }
            2 -> { // CHECKERBOARD
                (0 until size).flatMap { y -> (0 until size).map { x -> x to y } }
                        .filter { (x, y) -> (x + y) % 2 == 0 }
            }
            3 -> { // CORNERS: four blocks
                val c = size / 3
                (0 until size).flatMap { y -> (0 until size).map { x -> x to y } }
                        .filter { (x, y) -> (x < c || x >= size - c) && (y < c || y >= size - c) }
            }
            4 -> { // CROSS: middle row + column
                val m = size / 2
                (0 until size).map { x -> x to m } + (0 until size).map { y -> m to y }
            }
            5 -> { // DIAGONALS
                (0 until size).flatMap { i -> listOf(i to i, (size - 1 - i) to i) }
            }
            else -> emptyList() // 0: classic random scatter
        }

        val dirt = mutableListOf<Pair<Int, Int>>()
        fun put(x: Int, y: Int) {
            cells[y][x] = rng.nextInt(8)
            dirt.add(x to y)
        }
        if (candidates.isEmpty()) {
            var guard = 0
            while (dirt.size < target && guard++ < 1000) {
                val x = rng.nextInt(size); val y = rng.nextInt(size)
                if (cells[y][x] < 0) put(x, y)
                if (rowComplete(y) || colComplete(x)) break // never start near a full line
            }
        } else {
            for ((x, y) in candidates) {
                if (dirt.size >= target) break
                if (cells[y][x] < 0) put(x, y)
                if (rowComplete(y) || colComplete(x)) break
            }
            var guard = 0 // pattern may run short — top up randomly
            while (dirt.size < target && guard++ < 1000) {
                val x = rng.nextInt(size); val y = rng.nextInt(size)
                if (cells[y][x] < 0) put(x, y)
            }
        }
        // Never start with an already-completed line waiting on the board
        clearComplete()
        // Always leave a free 2x2 center so the level is playable from move 1
        val c = size / 2
        setCell(c, c, -1); setCell(c - 1, c, -1); setCell(c, c - 1, -1); setCell(c - 1, c - 1, -1)
        // report only the dirt that truly survived normalization
        return dirt.filter { cells[it.second][it.first] >= 0 }
    }

    fun anyPieceFits(pieces: List<Piece>): Boolean = pieces.any { p ->
        (0 until size).any { y -> (0 until size).any { x -> canPut(p, x, y) } }
    }

    data class ClearResult(val lines: Int, val cells: List<Pair<Int, Int>>)
}
