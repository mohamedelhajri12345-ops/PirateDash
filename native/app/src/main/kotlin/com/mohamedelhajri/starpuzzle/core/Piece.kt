package com.mohamedelhajri.starpuzzle.core

// A playable piece: a shape of filled cells plus a color.
// Special power-up pieces are 1x1 and carry no color (SPECIAL_* ids).
class Piece private constructor(
    val colorIndex: Int,
    val cellCols: Int,
    val cellRows: Int,
    rotation: Int,
    private val shape: Array<BooleanArray>
) {
    companion object {
        const val SPECIAL_STAR = 100
        const val SPECIAL_BOMB = 101
        const val SPECIAL_LIGHTNING = 102

        private const val SPECIAL_CHANCE = 5

        // The 9 classic shapes: 3 squares, 4 lines, 2 Ls
        fun random(specialChance: Int = SPECIAL_CHANCE, rng: java.util.Random = java.util.Random()): Piece {
            if (rng.nextInt(100) < specialChance) {
                return when (rng.nextInt(3)) {
                    0 -> special(SPECIAL_STAR)
                    1 -> special(SPECIAL_BOMB)
                    else -> special(SPECIAL_LIGHTNING)
                }
            }
            return normal(rng.nextInt(9), rng.nextInt(4), rng)
        }

        fun special(specialColorIndex: Int): Piece =
            Piece(specialColorIndex, 1, 1, 0, arrayOf(booleanArrayOf(true)))

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
            // L rotation: rotate the bounding box
            if (colorIndex == 7 || colorIndex == 8) {
                repeat(rotateCount and 3) {
                    val t = cols; cols = rows; rows = t
                }
            }
            val shape = Array(rows) { BooleanArray(cols) }
            when (colorIndex) {
                in 0..2 -> for (i in 0 until rows) for (j in 0 until cols) shape[i][j] = true
                in 3..6 -> for (i in 0 until rows) shape[i][0] = true
                else -> { // L shapes (7: 2x3, 8: 3x3 L-corner)
                    val lSize = if (colorIndex == 7) 2 else 3
                    // Build the L in its unrotated orientation then rotate it
                    val base = Array(lSize) { BooleanArray(lSize) }
                    if (colorIndex == 7) {
                        base[0][0] = true; base[1][0] = true
                        base[0][1] = true
                    } else {
                        for (i in 0 until lSize) base[i][0] = true
                        base[0][1] = true
                    }
                    var rotated = base
                    repeat(rotateCount and 3) {
                        rotated = rotateCW(rotated)
                    }
                    for (i in 0 until rows) for (j in 0 until cols) shape[i][j] = rotated[i][j]
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
