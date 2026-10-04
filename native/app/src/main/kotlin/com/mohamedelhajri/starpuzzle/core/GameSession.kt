package com.mohamedelhajri.starpuzzle.core

/**
 * One gameplay session (endless or a level from the catalog).
 * Pure Kotlin: the UI drives it and observes its state. All the v1.x
 * crash-lessons are baked in:
 *  - power-ups execute on the TRUE landing cell (board.lastPutX/Y)
 *  - the board never stores special ids
 *  - drops are validated before anything executes
 */
class GameSession(
    val level: LevelDefinition?,
    seed: Long = System.nanoTime()
) {
    val board = GameBoard()
    private val rng = java.util.Random(seed)

    // Tray
    var tray = mutableListOf<Piece>()
        private set

    // Score / combo
    var score = 0
        private set
    var combo = 0
        private set
    var maxCombo = 0
        private set
    var linesCleared = 0
        private set
    var movesUsed = 0
        private set

    // Level objective allowance
    var timeLeft = level?.timeLimit?.toFloat() ?: 0f
        private set
    var movesLeft: Int get() = if (level != null && level.maxMoves > 0) level.maxMoves - movesUsed else -1
        private set(_) {}

    enum class Status { PLAYING, WON, LOST }

    var status = Status.PLAYING
        private set

    // CLEANUP objective: dirt cells placed by the level prefill
    private val dirtRemaining = mutableSetOf<Pair<Int, Int>>()
    private var dirtInitial = 0
    val dirtCleared: Int get() = dirtInitial - dirtRemaining.size

    // MOVE booster: the most recent normal placement (null after specials)
    private var lastPlaced: Piece? = null
    private var lastPlacedX = -1
    private var lastPlacedY = -1
    private var lastPlacedPoints = 0

    // Events the UI can play feedback for
    data class PlaceEvent(
        val placedCells: List<Pair<Int, Int>>,
        val clearedCells: List<Pair<Int, Int>>,
        val powerUpCells: List<Pair<Int, Int>>,
        val powerUpKind: Int, // 0 none, else Piece.SPECIAL_*
        val combo: Int,
        val points: Int,
        val lines: Int,
        val blocked: Boolean
    )

    init {
        if (level != null && level.prefillDensity > 0)
            dirtRemaining.addAll(
                board.prefill(level.seed, level.prefillDensity, level.prefillPattern)
            )
        dirtInitial = dirtRemaining.size
        refillTray()
    }

    fun specialChance(): Int = level?.specialChance ?: 5

    private fun refillTray() {
        while (tray.size < TRAY_SIZE) tray.add(Piece.random(specialChance(), rng))
    }

    /** Called by the UI every frame with the elapsed seconds. */
    fun tick(dt: Float) {
        if (status != Status.PLAYING || level == null) return
        if (level.timeLimit > 0) {
            timeLeft -= dt
            if (timeLeft <= 0f) {
                timeLeft = 0f
                status = if (objectiveComplete()) Status.WON else Status.LOST
            }
        }
    }

    fun objectiveProgress(): Int = when (level?.objectiveType) {
        LevelDefinition.TYPE_LINES -> linesCleared
        LevelDefinition.TYPE_COMBO -> maxCombo
        LevelDefinition.TYPE_SURVIVE -> movesUsed
        LevelDefinition.TYPE_CLEANUP -> dirtCleared
        else -> score
    }

    private fun objectiveComplete(): Boolean = when (level?.objectiveType) {
        null -> false
        LevelDefinition.TYPE_LINES -> linesCleared >= level.targetLines
        LevelDefinition.TYPE_COMBO -> maxCombo >= level.targetCombo
        LevelDefinition.TYPE_SURVIVE -> movesUsed >= level.targetCount
        LevelDefinition.TYPE_CLEANUP -> dirtCleared >= level.targetCount
        else -> score >= level.targetScore
    }

    /** Fraction of the moves/time allowance left when the objective completes. */
    fun allowanceLeftFraction(): Float {
        val l = level ?: return 0f
        if (l.timeLimit > 0) return (timeLeft / l.timeLimit).coerceAtLeast(0f)
        if (l.maxMoves > 0) return (movesLeft.coerceAtLeast(0)).toFloat() / l.maxMoves
        return 0f
    }

    /**
     * Tries to place tray piece [trayIndex] at grid (x, y).
     * Runs the whole pipeline: validation -> place -> power-up -> line clear
     * -> score -> combo -> objective. Never throws, never corrupts state.
     */
    fun placePiece(trayIndex: Int, x: Int, y: Int): PlaceEvent? {
        if (status != Status.PLAYING) return null
        if (trayIndex !in tray.indices) return null
        val piece = tray[trayIndex]
        if (!board.canPut(piece, x, y)) return null

        // -- place
        val placedCells = mutableListOf<Pair<Int, Int>>()
        if (!piece.isSpecial) {
            for (i in 0 until piece.cellRows)
                for (j in 0 until piece.cellCols)
                    if (piece.filled(i, j)) placedCells.add(x + j to y + i)
        }
        require(board.putPiece(piece, x, y))
        if (piece.isSpecial) {
            lastPlaced = null // specials vanish into their effect — nothing to lift
        } else {
            lastPlaced = piece
            lastPlacedX = x
            lastPlacedY = y
        }
        tray.removeAt(trayIndex)
        refillTray()
        movesUsed++

        var points = piece.area()
        var powerUpKind = 0
        val powerUpCells = mutableListOf<Pair<Int, Int>>()

        // -- power-up effect on the TRUE landing cell
        val tx = board.lastPutX
        val ty = board.lastPutY
        if (piece.isSpecial && tx >= 0 && ty >= 0) {
            when (piece.colorIndex) {
                Piece.SPECIAL_STAR -> {
                    board.setCell(tx, ty, rng.nextInt(8))
                    points += 150
                    powerUpKind = Piece.SPECIAL_STAR
                    powerUpCells.add(tx to ty)
                }
                Piece.SPECIAL_BOMB -> {
                    powerUpCells.addAll(board.clearArea(tx, ty, 1))
                    points += powerUpCells.size * 2
                    powerUpKind = Piece.SPECIAL_BOMB
                }
                Piece.SPECIAL_LIGHTNING -> {
                    powerUpCells.addAll(board.clearCross(tx, ty))
                    points += powerUpCells.size * 2
                    powerUpKind = Piece.SPECIAL_LIGHTNING
                }
            }
        }

        // -- line clear
        val clear = board.clearComplete()
        val lines = clear.lines
        linesCleared += lines
        points += lines * 10

        // -- cleanup objective: dirt cells consumed by lines or power-ups
        if (dirtRemaining.isNotEmpty()) {
            dirtRemaining.removeAll(clear.cells)
            dirtRemaining.removeAll(powerUpCells)
        }

        // -- combo (consecutive pieces that cleared lines)
        if (lines > 0) {
            combo++
            if (combo > maxCombo) maxCombo = combo
            if (combo >= 2) points += (combo - 1) * 10
        } else {
            combo = 0
        }

        score += points
        if (lastPlaced != null) lastPlacedPoints = points

        // -- objective / failure
        val objectiveDone = level != null && objectiveComplete()
        val allowanceUsed = level != null &&
                ((level.maxMoves > 0 && movesUsed >= level.maxMoves) || level.timeLimit > 0 && timeLeft <= 0f)
        val blocked = !board.anyPieceFits(tray)

        when {
            objectiveDone -> status = Status.WON
            level == null -> { /* endless: blocked = game over, reported via event */ }
            allowanceUsed -> status = Status.LOST
            blocked -> status = Status.LOST
        }

        return PlaceEvent(
            placedCells, clear.cells, powerUpCells, powerUpKind,
            combo, points, lines, blocked
        )
    }

    /**
     * Board-targeted store booster (bomb / lightning / star). Returns the
     * affected cells. Removals can never complete a line, so no clear
     * pass is needed. Never throws.
     */
    fun applyBooster(kind: GameProgress.BoosterKind, x: Int, y: Int): List<Pair<Int, Int>> {
        if (status != Status.PLAYING) return emptyList()
        if (x !in 0 until board.size || y !in 0 until board.size) return emptyList()
        return when (kind) {
            GameProgress.BoosterKind.BOMB -> {
                val cells = board.clearArea(x, y, 1)
                score += cells.size * 2
                // CLEANUP: booster removals consume dirt too
                if (dirtRemaining.isNotEmpty()) dirtRemaining.removeAll(cells)
                cells
            }
            GameProgress.BoosterKind.LIGHTNING -> {
                val cells = board.clearCross(x, y)
                score += cells.size * 2
                if (dirtRemaining.isNotEmpty()) dirtRemaining.removeAll(cells)
                cells
            }
            GameProgress.BoosterKind.STAR -> {
                board.setCell(x, y, rng.nextInt(8))
                score += 50
                listOf(x to y)
            }
            GameProgress.BoosterKind.MOVE -> emptyList() // handled by takeBackLast
        }
    }

    /**
     * MOVE booster: is the last placed piece still liftable intact?
     * Every one of its cells must still hold its exact color — if a line
     * clear or power-up touched them, it can never be lifted (no guessing).
     */
    fun canTakeBack(): Boolean {
        if (status != Status.PLAYING) return false
        val p = lastPlaced ?: return false
        for (i in 0 until p.cellRows)
            for (j in 0 until p.cellCols)
                if (p.filled(i, j)) {
                    val x = lastPlacedX + j
                    val y = lastPlacedY + i
                    if (x !in 0 until board.size || y !in 0 until board.size) return false
                    if (board.colorAt(x, y) != p.colorIndex) return false
                }
        return true
    }

    /**
     * MOVE booster: lifts the last placed piece back into the tray and
     * refunds its move and points. Never throws; null when unavailable.
     */
    fun takeBackLast(): Piece? {
        if (!canTakeBack()) return null
        val p = lastPlaced ?: return null
        for (i in 0 until p.cellRows)
            for (j in 0 until p.cellCols)
                if (p.filled(i, j)) board.setCell(lastPlacedX + j, lastPlacedY + i, -1)
        lastPlaced = null
        movesUsed--
        score -= lastPlacedPoints
        if (score < 0) score = 0
        tray.add(p)
        return p
    }

    companion object {
        const val TRAY_SIZE = 3
    }
}
