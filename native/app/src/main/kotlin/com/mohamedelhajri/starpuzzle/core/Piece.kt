package com.mohamedelhajri.starpuzzle.core

// A playable piece: a shape of filled cells plus a color.
// Special power-up pieces are 1x1 and carry no color (SPECIAL_* ids).
//
// v8.0.0 Block Blast edition: the tray now draws from the 26-shape
// Block Blast catalog (ported 1:1 from the owner's reference clone),
// with the color chosen independently of the shape, exactly like the
// reference game. Colors stay in 0..8 so every material skin keeps
// working unchanged.
class Piece private constructor(
    val colorIndex: Int,
    val cellCols: Int,
    val cellRows: Int,
    rotation: Int,
    private val shape: Array<BooleanArray>
) {
    companion object {
        // Contract: the UI palette MUST provide exactly this many colors.
        const val PIECE_COLOR_COUNT = 9

        const val SPECIAL_STAR = 100
        const val SPECIAL_BOMB = 101
        const val SPECIAL_LIGHTNING = 102

        private const val SPECIAL_CHANCE = 5

        // ------------------------------------------------------------------
        // The 26 Block Blast shapes, ported 1:1 from the owner's reference
        // clone (block-blast-clone/game.js SHAPES). '1' = filled cell.
        // ------------------------------------------------------------------
        private fun grid(vararg rows: String): Array<BooleanArray> =
            Array(rows.size) { r -> BooleanArray(rows[r].length) { c -> rows[r][c] == '1' } }

        private val BB_SHAPES: Array<Array<BooleanArray>> = arrayOf(
            grid("1"),                              // 0: 1x1
            grid("11"),                            // 1: line 2
            grid("111"),                           // 2: line 3
            grid("1111"),                          // 3: line 4
            grid("11111"),                         // 4: line 5
            grid("1", "1"),                        // 5: column 2
            grid("1", "1", "1"),                  // 6: column 3
            grid("1", "1", "1", "1"),             // 7: column 4
            grid("1", "1", "1", "1", "1"),       // 8: column 5
            grid("11", "11"),                     // 9: square 2x2
            grid("111", "111", "111"),            // 10: square 3x3
            grid("10", "10", "11"),               // 11: L right
            grid("01", "01", "11"),               // 12: L left
            grid("111", "010"),                   // 13: T
            grid("011", "110"),                   // 14: S
            grid("110", "011"),                   // 15: Z
            grid("10", "11"),                     // 16: corner
            grid("01", "11"),                     // 17: corner
            grid("11", "10"),                     // 18: corner
            grid("11", "01"),                     // 19: corner
            grid("111", "100"),                   // 20: L pentomino
            grid("111", "001"),                   // 21: L pentomino
            grid("100", "111"),                   // 22: L pentomino
            grid("001", "111"),                   // 23: L pentomino
            grid("11", "11", "11"),               // 24: block 2x3
            grid("111", "111")                    // 25: block 3x2
        )

        // ------------------------------------------------------------------
        // Random piece: Block Blast catalog + independent color, plus the
        // owner's power-up specials (star / bomb / lightning).
        // ------------------------------------------------------------------
        fun random(specialChance: Int = SPECIAL_CHANCE, rng: java.util.Random = java.util.Random()): Piece {
            if (rng.nextInt(100) < specialChance) {
                return when (rng.nextInt(3)) {
                    0 -> special(SPECIAL_STAR)
                    1 -> special(SPECIAL_BOMB)
                    else -> special(SPECIAL_LIGHTNING)
                }
            }
            val shape = BB_SHAPES[rng.nextInt(BB_SHAPES.size)]
            val color = rng.nextInt(PIECE_COLOR_COUNT)
            return Piece(
                colorIndex = color,
                cellCols = shape[0].size,
                cellRows = shape.size,
                rotation = 0,
                shape = shape.map { it.clone() }.toTypedArray()
            )
        }

        fun special(specialColorIndex: Int): Piece =
            Piece(specialColorIndex, 1, 1, 0, arrayOf(booleanArrayOf(true)))

        // Legacy constructor kept for the old 9-shape catalog; still used by
        // unit tests and by code that builds pieces from level definitions.
        fun normal(colorIndex: Int, rotateCount: Int, rng: java.util.Random = java.util.Random()): Piece {
            var cols: Int; var rows: Int
            when (colorIndex) {
                0 -> { cols = 1; rows = 1 }
                1 -> { cols = 2; rows = 2 }
                2 -> { cols = 3; rows = 3 }
                3 -> { cols = 1; rows = 2 }
                4 -> { cols = 1; rows = 3 }
                5 -> { cols = 1; rows = 4 }
                6 -> { cols = 1; rows = 5 }
                7 -> { cols = 2; rows = 3 }
                8 -> { cols = 3; rows = 3 }
                else -> throw IllegalArgumentException("bad piece color index $colorIndex")
            }
            // line pieces: odd rotations make the line horizontal
            if (colorIndex in 3..6 && (rotateCount and 1) == 1) {
                val len = rows
                cols = len; rows = 1
            }
            // L rotation: rotate the bounding box
            if (colorIndex == 7 || colorIndex == 8) {
                repeat(rotateCount and 3) {
                    val t = cols; cols = rows; rows = t
                }
            }
            val shape = Array(rows) { BooleanArray(cols) }
            when (colorIndex) {
                in 0..2 -> for (i in 0 until rows) for (j in 0 until cols) shape[i][j] = true
                in 3..6 -> if (rows == 1) for (j in 0 until cols) shape[0][j] = true
                            else for (i in 0 until rows) shape[i][0] = true
                else -> { // L shapes: 7 = L-tetromino in a 2x3 box, 8 = L in a 3x3 box
                    var g = Array(3) { BooleanArray(if (colorIndex == 7) 2 else 3) }
                    for (i in 0 until 3) g[i][0] = true
                    g[0][1] = true
                    if (colorIndex == 8) g[0][2] = true
                    repeat(rotateCount and 3) { g = rotateCW(g) }
                    require(g.size == rows && g[0].size == cols) { "L shape/box mismatch" }
                    for (i in 0 until rows) for (j in 0 until cols) shape[i][j] = g[i][j]
                }
            }
            return Piece(colorIndex, cols, rows, rotateCount, shape)
        }

        private fun rotateCW(m: Array<BooleanArray>): Array<BooleanArray> {
            val rows = m.size; val cols = m[0].size
            val r = Array(cols) { BooleanArray(rows) }
            for (i in 0 until rows) for (j in 0 until cols) r[j][rows - 1 - i] = m[i][j]
            return r
        }
    }

    val isSpecial: Boolean get() = colorIndex >= SPECIAL_STAR

    fun filled(i: Int, j: Int): Boolean = shape[i][j]

    fun area(): Int {
        var a = 0
        for (row in shape) for (c in row) if (c) a++
        return a
    }
}
