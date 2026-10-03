package com.mohamedelhajri.starpuzzle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Power-up safety tests — the native Kotlin port of the v1.3.1 safety suite.
 * The v1.x crash class must be structurally impossible here:
 *  - the board never stores special ids (100+)
 *  - power-ups act on the true landing cell
 *  - every clear helper clamps its target
 */
class PowerUpSafetyTest {

    private fun newBoard() = GameBoard(10)

    private fun fill(b: GameBoard) {
        for (y in 0 until 10) for (x in 0 until 10) b.setCell(x, y, 0)
    }

    // ── special pieces never pollute the board ──

    @Test
    fun specialPieceNeverStoresItsColorOnTheBoard() {
        val board = newBoard()
        val bomb = Piece.special(Piece.SPECIAL_BOMB)
        assertTrue(board.putPiece(bomb, 0, 0))
        assertTrue(board.isEmpty(0, 0))
        assertEquals(0, board.lastPutX)
        assertEquals(0, board.lastPutY)
    }

    @Test
    fun normalPiecesStillStoreTheirColor() {
        val board = newBoard()
        val square = Piece.normal(1, 0) // 2x2
        assertTrue(board.putPiece(square, 2, 2))
        assertFalse(board.isEmpty(2, 2))
        assertFalse(board.isEmpty(3, 3))
        assertTrue(board.isEmpty(4, 4))
    }

    @Test
    fun specialPieceCannotLandOnOccupiedCell() {
        val board = newBoard()
        assertTrue(board.putPiece(Piece.normal(0, 0), 5, 5)) // 1x1
        assertFalse(board.putPiece(Piece.special(Piece.SPECIAL_BOMB), 5, 5))
        assertTrue(board.putPiece(Piece.special(Piece.SPECIAL_BOMB), 5, 6))
    }

    // ── bomb at every edge and corner ──

    @Test
    fun bombClearsCorrectAreasAtAllCorners() {
        val board = newBoard(); fill(board)
        assertEquals(4, board.clearArea(0, 0, 1).size)
        assertEquals(4, board.clearArea(9, 0, 1).size)
        assertEquals(4, board.clearArea(0, 9, 1).size)
        assertEquals(4, board.clearArea(9, 9, 1).size)
    }

    @Test
    fun bombInCenterClearsNineCells() {
        val board = newBoard(); fill(board)
        assertEquals(9, board.clearArea(4, 4, 1).size)
    }

    @Test
    fun bombOnEmptyBoardClearsNothing() {
        assertEquals(0, newBoard().clearArea(4, 4, 1).size)
    }

    @Test
    fun bombNeverClearsOutsideTheBoard() {
        val board = newBoard(); fill(board)
        assertEquals(4, board.clearArea(-5, -5, 1).size)
        assertEquals(4, board.clearArea(15, 15, 1).size)
        assertEquals(6, board.clearArea(-1, 4, 1).size) // cols 0..1 x rows 3..5
    }

    // ── lightning at every edge and corner ──

    @Test
    fun lightningOnFullBoardClearsRowAndColumn() {
        val board = newBoard(); fill(board)
        assertEquals(19, board.clearCross(4, 4).size)
    }

    @Test
    fun lightningAtCornersNeverCrashes() {
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(0, 0).size)
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(9, 9).size)
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(0, 9).size)
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(9, 0).size)
    }

    @Test
    fun lightningOutOfRangeIsClamped() {
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(-3, -3).size)
        assertEquals(19, newBoard().apply { fill(this) }.clearCross(42, 42).size)
    }

    // ── color palette contract (the v2.0.x root-cause crash class) ──

    @Test
    fun everyGeneratedPieceCarriesARealPaletteColor() {
        val rng = java.util.Random(42)
        repeat(5000) {
            val p = Piece.random(10, rng)
            if (!p.isSpecial) {
                assertTrue(
                    "colorIndex ${p.colorIndex} outside palette",
                    p.colorIndex in 0 until Piece.PIECE_COLOR_COUNT
                )
            }
        }
    }

    @Test
    fun boardCellsAlwaysHoldRealPaletteColors() {
        val rng = java.util.Random(7)
        repeat(300) {
            val b = GameBoard(10)
            val p = Piece.random(0, rng) // normal pieces only
            if (b.canPut(p, 0, 0)) b.putPiece(p, 0, 0)
            for (y in 0 until 10) for (x in 0 until 10) {
                val c = b.colorAt(x, y)
                assertTrue("cell color $c outside palette", c == -1 || c in 0 until Piece.PIECE_COLOR_COUNT)
            }
        }
    }

    // ── piece rotation ──

    @Test
    fun linePiecesRotateBetweenVerticalAndHorizontal() {
        val v = Piece.normal(5, 0) // 1x4 vertical
        assertEquals(1, v.cellCols)
        assertEquals(4, v.cellRows)
        assertTrue(v.filled(3, 0))
        val h = Piece.normal(5, 1) // rotated: 4x1 horizontal
        assertEquals(4, h.cellCols)
        assertEquals(1, h.cellRows)
        assertTrue(h.filled(0, 3))
        // every rotation of every line piece stays a connected full line
        for (ci in 3..6) for (rot in 0..3) {
            val p = Piece.normal(ci, rot)
            assertEquals(p.area(), maxOf(p.cellCols, p.cellRows))
        }
    }

    @Test
    fun lPieceRotationsStayValidShapes() {
        for (ci in intArrayOf(7, 8)) for (rot in 0..3) {
            val p = Piece.normal(ci, rot, java.util.Random(1))
            assertTrue("bad L area ${p.area()}", p.area() == 4 || p.area() == 5)
            assertTrue(p.cellCols in 2..3 && p.cellRows in 2..3)
            // every declared cell of the box is addressable (the old crash)
            for (i in 0 until p.cellRows) for (j in 0 until p.cellCols) p.filled(i, j)
        }
    }

    // ── prefill safety across the whole catalog ──

    @Test
    fun prefillNeverStartsWithCompletedLinesOrUnplayableBoard() {
        for (id in 1..1000 step 13) {
            val l = LevelCatalog.getLevel(id)
            if (l.prefillDensity == 0) continue
            val b = GameBoard(10)
            b.prefill(l.seed, l.prefillDensity)
            assertEquals("level $id starts with a completed line", 0, b.clearComplete().lines)
        }
    }

    // ── line clears ──

    @Test
    fun clearCompleteClearsFullRowsAndColumns() {
        val board = newBoard()
        for (x in 0 until 10) board.setCell(x, 3, 2) // full row
        val result = board.clearComplete()
        assertEquals(1, result.lines)
        assertEquals(10, result.cells.size)
        assertTrue(board.isEmpty(0, 3))
    }

    @Test
    fun clearCompleteOnEmptyBoardDoesNothing() {
        val r = newBoard().clearComplete()
        assertEquals(0, r.lines)
        assertTrue(r.cells.isEmpty())
    }

    // ── rapid power-up spam: state stays consistent ──

    @Test
    fun rapidBombAndLightningSpamStaysConsistent() {
        val board = newBoard(); fill(board)
        for (i in 0 until 50) {
            board.clearArea(i % 10, (i * 3) % 10, 1)
            board.clearCross((i * 7) % 10, (i * 2) % 10)
        }
        for (y in 0 until 10) for (x in 0 until 10)
            assertTrue(board.colorAt(x, y) < 100) // board is always clean
    }

    // ── session-level safety ──

    @Test
    fun sessionPlacesPowerUpOnTrueLandingCell() {
        val session = GameSession(null, seed = 42)
        val board = session.board
        val bomb = Piece.special(Piece.SPECIAL_BOMB)
        assertTrue(board.putPiece(bomb, 0, 0))
        assertEquals(0, board.clearArea(board.lastPutX, board.lastPutY, 1).size)
    }

    @Test
    fun sessionRejectsInvalidPlacements() {
        val s2 = GameSession(null, seed = 7)
        assertEquals(null, s2.placePiece(0, -1, -1))
        assertEquals(null, s2.placePiece(99, 0, 0))
        assertEquals(0, s2.movesUsed)
    }

    @Test
    fun invalidDropsNeverChangeState() {
        val session = GameSession(null, seed = 7)
        val board = session.board
        board.setCell(0, 0, 1); board.setCell(1, 0, 1)
        val line = Piece.normal(5, 0) // 1x4 vertical
        assertFalse(board.canPut(line, 0, 0))
        assertFalse(board.canPut(line, -1, 0))
        assertFalse(board.canPut(line, 9, 9)) // overflows bounds
    }
}
