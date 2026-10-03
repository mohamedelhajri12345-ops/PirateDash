/*
    1010! Klooni, a free customizable puzzle game for Android and Desktop
    Copyright (C) 2017-2019  Lonami Exo @ lonami.dev

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package dev.lonami.klooni.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import dev.lonami.klooni.interfaces.IEffect;
import dev.lonami.klooni.interfaces.IEffectFactory;
import dev.lonami.klooni.serializer.BinSerializable;

// Represents the on screen board, with all the put cells
// and functions to determine when it is game over given a PieceHolder
public class Board implements BinSerializable {

    //region Members

    public final int cellCount;
    public float cellSize;
    private Cell[][] cells;
    private final Array<IEffect> effects = new Array<IEffect>(); // Particle effects once they vanish

    public final Vector2 pos = new Vector2();

    // Used to animate cleared cells vanishing
    private final Vector2 lastPutPiecePos = new Vector2();

    // Star Puzzle: grid coordinates of the cell where the last piece was
    // placed. This is the single source of truth for power-up targeting:
    // recomputing targets from screen positions is forbidden (it caused
    // off-by-one targets and out-of-bounds crashes).
    public int lastPutCellX = -1;
    public int lastPutCellY = -1;

    //endregion

    //region Constructor

    public Board(final GameLayout layout, int cellCount) {
        this.cellCount = cellCount;

        // Cell size depends on the layout to be updated first
        layout.update(this);
        createCells();
    }

    public Board(final Rectangle area, int cellCount) {
        this.cellCount = cellCount;

        // Cell size depends on the layout to be updated first
        pos.set(area.x, area.y);
        cellSize = Math.min(area.height, area.width) / cellCount;
        createCells();
    }

    private void createCells() {
        cells = new Cell[this.cellCount][this.cellCount];
        for (int i = 0; i < this.cellCount; ++i) {
            for (int j = 0; j < this.cellCount; ++j) {
                cells[i][j] = new Cell(j * cellSize, i * cellSize, cellSize);
            }
        }
    }

    //endregion

    //region Private methods

    // True if the given cell coordinates are inside the bounds of the board
    private boolean inBounds(int x, int y) {
        return x >= 0 && x < cellCount && y >= 0 && y < cellCount;
    }

    // True if the given piece at the given coordinates is not outside the bounds of the board
    private boolean inBounds(Piece piece, int x, int y) {
        return inBounds(x, y) && inBounds(x + piece.cellCols - 1, y + piece.cellRows - 1);
    }

    // This only tests for the piece on the given coordinates, not the whole board
    private boolean canPutPiece(Piece piece, int x, int y) {
        if (!inBounds(piece, x, y))
            return false;

        for (int i = 0; i < piece.cellRows; ++i)
            for (int j = 0; j < piece.cellCols; ++j)
                if (!cells[y + i][x + j].isEmpty() && piece.filled(i, j))
                    return false;

        return true;
    }

    // Returns true iff the piece was put on the board.
    // Star Puzzle: special power-up pieces never write their special color
    // index into the board. The board only ever stores normal color indices
    // (-1..7); storing 100+ made Theme.getCellColor throw
    // ArrayIndexOutOfBoundsException on the next draw and crashed the game.
    // The exact landing cell is recorded instead, so power-up effects act on
    // the real target cell (see handleSpecialPiece).
    public boolean putPiece(Piece piece, int x, int y) {
        if (!canPutPiece(piece, x, y))
            return false;

        lastPutPiecePos.set(piece.calculateGravityCenter());
        lastPutCellX = x;
        lastPutCellY = y;

        if (!piece.isSpecial()) {
            for (int i = 0; i < piece.cellRows; ++i)
                for (int j = 0; j < piece.cellCols; ++j)
                    if (piece.filled(i, j))
                        cells[y + i][x + j].set(piece.colorIndex);
        }

        return true;
    }

    //endregion

    //region Public methods

    public void draw(final Batch batch) {
        batch.setTransformMatrix(batch.getTransformMatrix().translate(pos.x, pos.y, 0));

        for (int i = 0; i < cellCount; ++i)
            for (int j = 0; j < cellCount; ++j)
                cells[i][j].draw(batch);

        for (int i = effects.size; i-- != 0; ) {
            effects.get(i).draw(batch);
            if (effects.get(i).isDone())
                effects.removeIndex(i);
        }

        batch.setTransformMatrix(batch.getTransformMatrix().translate(-pos.x, -pos.y, 0));
    }

    public boolean canPutPiece(Piece piece) {
        for (int i = 0; i < cellCount; ++i)
            for (int j = 0; j < cellCount; ++j)
                if (canPutPiece(piece, j, i))
                    return true;

        return false;
    }

    public boolean putScreenPiece(final Piece piece) {
        return putPiece(piece, screenToCellX(piece), screenToCellY(piece));
    }

    // Star Puzzle: canonical screen -> grid conversion. Every piece of code
    // that needs "which cell is this piece over" must use these, so the drop
    // and the power-up target can never disagree again.
    public int screenToCellX(final Piece piece) {
        return MathUtils.round((piece.pos.x - pos.x) / piece.cellSize);
    }

    public int screenToCellY(final Piece piece) {
        return MathUtils.round((piece.pos.y - pos.y) / piece.cellSize);
    }

    Vector2 snapToGrid(final Piece piece, final Vector2 position) {
        // Snaps the given position (e.g. mouse) to the grid,
        // assuming piece wants to be put at the specified position.
        // If the piece was not on the grid, the original position is returned
        //
        // Logic to determine the x and y is a copy-paste from putScreenPiece
        final Vector2 local = position.cpy().sub(pos);
        int x = MathUtils.round(local.x / piece.cellSize);
        int y = MathUtils.round(local.y / piece.cellSize);
        if (canPutPiece(piece, x, y))
            return new Vector2(pos.x + x * piece.cellSize, pos.y + y * piece.cellSize);
        else
            return position;
    }

    // This will clear both complete rows and columns, all at once.
    // The reason why we can't check first rows and then columns
    // (or vice versa) is because the following case (* filled, _ empty):
    //
    // 4x4 boardHeight    piece
    // _ _ * *      * *
    // _ * * *      *
    // * * _ _
    // * * _ _
    //
    // If the piece is put on the top left corner, all the cells will be cleared.
    // If we first cleared the columns, then the rows wouldn't have been cleared.
    public int clearComplete(final IEffectFactory effect) {
        int clearCount = 0;
        boolean[] clearedRows = new boolean[cellCount];
        boolean[] clearedCols = new boolean[cellCount];

        // Analyze rows and columns that will be cleared
        for (int i = 0; i < cellCount; ++i) {
            clearedRows[i] = true;
            for (int j = 0; j < cellCount; ++j) {
                if (cells[i][j].isEmpty()) {
                    clearedRows[i] = false;
                    break;
                }
            }
            if (clearedRows[i])
                clearCount++;
        }
        for (int j = 0; j < cellCount; ++j) {
            clearedCols[j] = true;
            for (int i = 0; i < cellCount; ++i) {
                if (cells[i][j].isEmpty()) {
                    clearedCols[j] = false;
                    break;
                }
            }
            if (clearedCols[j])
                clearCount++;
        }
        if (clearCount > 0) {
            // Do clear those rows and columns
            for (int i = 0; i < cellCount; ++i) {
                if (clearedRows[i]) {
                    for (int j = 0; j < cellCount; ++j) {
                        effects.add(effect.create(cells[i][j], lastPutPiecePos));
                        cells[i][j].set(-1);
                    }
                }
            }

            for (int j = 0; j < cellCount; ++j) {
                if (clearedCols[j]) {
                    for (int i = 0; i < cellCount; ++i) {
                        effects.add(effect.create(cells[i][j], lastPutPiecePos));
                        cells[i][j].set(-1);
                    }
                }
            }
        }

        return clearCount;
    }

    // Star Puzzle: clears a square area of (2*radius + 1) cells centered on (x, y).
    // Returns the amount of cells that were cleared.
    public int clearArea(final int x, final int y, final int radius, final IEffectFactory effect) {
        int cleared = 0;
        final int tx = MathUtils.clamp(x, 0, cellCount - 1);
        final int ty = MathUtils.clamp(y, 0, cellCount - 1);
        final int minX = Math.max(0, tx - radius), maxX = Math.min(cellCount - 1, tx + radius);
        final int minY = Math.max(0, ty - radius), maxY = Math.min(cellCount - 1, ty + radius);

        for (int i = minY; i <= maxY; ++i) {
            for (int j = minX; j <= maxX; ++j) {
                if (!cells[i][j].isEmpty()) {
                    effects.add(effect.create(cells[i][j], lastPutPiecePos));
                    cells[i][j].set(-1);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    // Star Puzzle: clears the whole row and column crossing on (x, y).
    // Returns the amount of cells that were cleared.
    // Defensive: targets are clamped into the grid, an out-of-range call can
    // never throw (ArrayIndexOutOfBoundsException crash guard).
    public int clearCross(final int x, final int y, final IEffectFactory effect) {
        final int cx = MathUtils.clamp(x, 0, cellCount - 1);
        final int cy = MathUtils.clamp(y, 0, cellCount - 1);
        int cleared = 0;
        for (int j = 0; j < cellCount; ++j) {
            if (!cells[cy][j].isEmpty()) {
                effects.add(effect.create(cells[cy][j], lastPutPiecePos));
                cells[cy][j].set(-1);
                cleared++;
            }
        }
        for (int i = 0; i < cellCount; ++i) {
            if (!cells[i][cx].isEmpty()) {
                effects.add(effect.create(cells[i][cx], lastPutPiecePos));
                cells[i][cx].set(-1);
                cleared++;
            }
        }
        return cleared;
    }

    // Star Puzzle: converts a special cell (star) back to a normal colored cell
    public void setCell(final int x, final int y, final int colorIndex) {
        cells[MathUtils.clamp(y, 0, cellCount - 1)][MathUtils.clamp(x, 0, cellCount - 1)]
                .set(colorIndex);
    }

    // Star Puzzle: true when the given cell is empty (level pre-fill support)
    public boolean isEmpty(final int x, final int y) {
        return inBounds(x, y) && cells[y][x].isEmpty();
    }

    // Star Puzzle: screen coordinates of the center of the given cell
    public Vector2 cellCenter(final int x, final int y) {
        final int cx = MathUtils.clamp(x, 0, cellCount - 1);
        final int cy = MathUtils.clamp(y, 0, cellCount - 1);
        return new Vector2(pos.x + (cx + 0.5f) * cellSize, pos.y + (cy + 0.5f) * cellSize);
    }

    public void clearAll(final int clearFromX, final int clearFromY, final IEffectFactory effect) {
        final Vector2 culprit = cells[clearFromY][clearFromX].pos;

        for (int i = 0; i < cellCount; ++i) {
            for (int j = 0; j < cellCount; ++j) {
                if (!cells[i][j].isEmpty()) {
                    effects.add(effect.create(cells[i][j], culprit));
                    cells[i][j].set(-1);
                }
            }
        }
    }

    public boolean effectsDone() {
        return effects.size == 0;
    }

    //endregion

    //region Serialization

    @Override
    public void write(DataOutputStream out) throws IOException {
        // Cell count, cells in row-major order
        out.writeInt(cellCount);
        for (int i = 0; i < cellCount; ++i)
            for (int j = 0; j < cellCount; ++j)
                cells[i][j].write(out);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        // If the saved cell count does not match the current cell count,
        // then an IOException is thrown since the data saved was invalid
        final int savedCellCount = in.readInt();
        if (savedCellCount != cellCount)
            throw new IOException("Invalid cellCount saved.");

        for (int i = 0; i < cellCount; ++i)
            for (int j = 0; j < cellCount; ++j)
                cells[i][j].read(in);
    }

    //endregion
}
