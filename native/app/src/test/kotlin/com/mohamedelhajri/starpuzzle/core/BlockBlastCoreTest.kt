package com.mohamedelhajri.starpuzzle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BlockBlastCoreTest — mirrors the parity smoke suite of the owner's
 * original reference file (26 shapes, +1/cell, +16/line, tray refill,
 * game-over detection, occupied-cell rejection).
 */
class BlockBlastCoreTest {

    /** Pins the RNG: every tray piece is the single-cell shape, color #FF6B6B. */
    private fun newCore(): BlockBlastCore {
        RandomSource.nextInt = { _ -> 0 }
        return BlockBlastCore().also { it.reset() }
    }

    @Test
    fun `spec matches the original file`() {
        assertEquals(8, BlockBlastSpec.SIZE)
        assertEquals(26, BlockBlastSpec.SHAPES.size)
        assertEquals(8, BlockBlastSpec.COLORS.size)
        // 4 vibrant shop skins, 8 colors each
        assertEquals(4, BlockBlastSpec.SKINS.size)
        assertTrue(BlockBlastSpec.SKINS.all { it.colors.size == 8 })
        assertEquals(16, BlockBlastSpec.POINTS_PER_LINE)
        assertEquals(3, BlockBlastSpec.TRAY_SIZE)
        assertEquals(5, BlockBlastSpec.COINS_PER_LINE)
        // the 3x3 square (shape index 10) has 9 filled cells
        val square = BlockBlastSpec.SHAPES[10]
        assertEquals(3, square.size)
        assertEquals(9, square.sumOf { row -> row.count { it == 1 } })
        // the L shape (index 11) matches the original [[1,0],[1,0],[1,1]]
        val l = BlockBlastSpec.SHAPES[11]
        assertEquals(listOf(1, 0), l[0].toList())
        assertEquals(listOf(1, 0), l[1].toList())
        assertEquals(listOf(1, 1), l[2].toList())
    }

    @Test
    fun `reroll keeps placed slots empty and unplaced slots filled`() {
        val core = newCore()
        core.place(0, 0, 0) // slot 0 used
        core.rerollTray()
        assertNull(core.tray[0])
        assertTrue(core.tray[1] != null && core.tray[2] != null)
    }

    @Test
    fun `clear reports cells with colors for the burst effect`() {
        val core = newCore()
        var cleared: PlaceResult? = null
        repeat(8) { c ->
            val idx = core.tray.indexOfFirst { p -> p != null }
            val result = core.place(idx, 0, c)
            if (result != null && result.clearedLines > 0) cleared = result
        }
        // the final placement cleared 8 cells, each with a color
        assertEquals(8, cleared!!.clearedCells.size)
        assertEquals(1, cleared!!.clearedLines)
        assertTrue(cleared!!.clearedCells.all { it.color == BlockBlastSpec.SKINS[0].colors[0] })
    }

    @Test
    fun `placement adds one point per cell`() {
        val core = newCore()
        val result = core.place(0, 0, 0)
        assertEquals(1, result!!.cellsPlaced)
        assertEquals(1, core.score)
        assertEquals(0, result.clearedLines)
    }

    @Test
    fun `full row clears and awards 16`() {
        val core = newCore()
        // fill row 0 with 8 single-cell pieces, always into the first free slot
        repeat(8) {
            val idx = core.tray.indexOfFirst { p -> p != null }
            val col = (0 until BlockBlastSpec.SIZE).first { c -> core.grid[0][c] == null }
            core.place(idx, 0, col)
        }
        // 8 cells placed (+1 each) and the row cleared once (+16)
        assertEquals(8 * 1 + 16, core.score)
        assertTrue((0 until BlockBlastSpec.SIZE).all { c -> core.grid[0][c] == null })
    }

    @Test
    fun `two lines at once award 32 and report a combo`() {
        val core = newCore()
        // row 0 missing only (0,7); column 7 missing only (0,7).
        // The final single cell at (0,7) completes BOTH -> combo.
        repeat(7) { c -> // row 0, cols 0..6
            val idx = core.tray.indexOfFirst { p -> p != null }
            core.place(idx, 0, c)
        }
        repeat(7) { r -> // column 7, rows 1..7
            val idx = core.tray.indexOfFirst { p -> p != null }
            core.place(idx, r + 1, 7)
        }
        val idx = core.tray.indexOfFirst { p -> p != null }
        core.place(idx, 0, 7) // completes row 0 + column 7 in one move
        // 15 cells placed + 2 lines x 16
        assertEquals(15 + 32, core.score)
        assertEquals(2, core.lastClearedLines)
    }

    @Test
    fun `tray refills when all three pieces are placed`() {
        val core = newCore()
        assertTrue(core.tray.all { it != null })
        core.place(0, 0, 0)
        assertNull(core.tray[0])
        core.place(1, 7, 7)
        core.place(2, 6, 6)
        assertTrue(core.tray.all { it != null }) // refilled after the third
    }

    @Test
    fun `illegal placement returns null`() {
        val core = newCore()
        assertNull(core.place(0, -1, -1))
        assertNull(core.place(0, 8, 8))
        assertEquals(0, core.score)
    }

    @Test
    fun `cannot place on occupied cells`() {
        val core = newCore()
        assertTrue(core.canPlace(core.tray[0]!!, 0, 0))
        core.place(0, 0, 0) // occupies (0,0)
        core.refillTray()
        assertFalse(core.canPlace(core.tray[0]!!, 0, 0))
        assertTrue(core.canPlace(core.tray[0]!!, 0, 1))
    }

    @Test
    fun `game over when nothing fits`() {
        val core = newCore()
        for (r in 0 until BlockBlastSpec.SIZE) {
            for (c in 0 until BlockBlastSpec.SIZE) core.grid[r][c] = BlockBlastSpec.COLORS[0]
        }
        core.grid[0][0] = null
        assertFalse(core.checkGameOver()) // single cell still fits at (0,0)
        core.grid[0][0] = BlockBlastSpec.COLORS[1]
        assertTrue(core.checkGameOver()) // board full, nothing fits
    }
}
