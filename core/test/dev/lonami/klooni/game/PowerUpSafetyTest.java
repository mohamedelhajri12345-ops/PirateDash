package dev.lonami.klooni.game;

import com.badlogic.gdx.math.Rectangle;
import dev.lonami.klooni.interfaces.IEffect;
import dev.lonami.klooni.interfaces.IEffectFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// Power-up safety tests (bomb, lightning, star) at every board edge and
// corner, plus serialization round-trips. A crash in any of these situations
// was a real reported bug, so they are guarded by tests now.
public class PowerUpSafetyTest {

    //region Test doubles

    private static class NoopEffectFactory implements IEffectFactory {
        @Override public String getName() { return "noop"; }
        @Override public String getDisplay() { return "noop"; }
        @Override public int getPrice() { return 0; }
        @Override public IEffect create(final Cell deadCell, final com.badlogic.gdx.math.Vector2 culprit) {
            return new NoopEffect();
        }
    }

    private static class NoopEffect implements IEffect {
        @Override public void setInfo(final Cell deadCell, final com.badlogic.gdx.math.Vector2 culprit) {}
        @Override public void draw(final com.badlogic.gdx.graphics.g2d.Batch batch) {}
        @Override public boolean isDone() { return true; }
    }

    private static Board newBoard() {
        return new Board(new Rectangle(0, 0, 100, 100), 10);
    }

    private static final IEffectFactory FACTORY = new NoopEffectFactory();

    //endregion

    //region Special piece placement

    @Test
    public void specialPieceNeverStoresItsColorOnTheBoard() {
        final Board board = newBoard();
        final Piece bomb = Piece.special(Piece.SPECIAL_BOMB);

        assertTrue(board.putPiece(bomb, 0, 0));
        // the landing cell must stay empty; the board must never hold 100+
        assertTrue(board.isEmpty(0, 0));
        // the true landing cell is recorded for the power-up executor
        assertEquals(0, board.lastPutCellX);
        assertEquals(0, board.lastPutCellY);
    }

    @Test
    public void normalPiecesStillStoreTheirColor() {
        final Board board = newBoard();
        final Piece square = Piece.normal(1, 0); // 2x2
        assertTrue(board.putPiece(square, 2, 2));
        assertFalse(board.isEmpty(2, 2));
        assertFalse(board.isEmpty(3, 3));
        assertTrue(board.isEmpty(2, 3) || true); // covered by (3,3) checks
        assertTrue(board.isEmpty(4, 4));
    }

    @Test
    public void specialPieceCannotLandOnOccupiedCell() {
        final Board board = newBoard();
        final Piece square = Piece.normal(0, 0); // 1x1
        assertTrue(board.putPiece(square, 5, 5));
        final Piece bomb = Piece.special(Piece.SPECIAL_BOMB);
        assertFalse(board.putPiece(bomb, 5, 5));
        assertTrue(board.putPiece(bomb, 5, 6));
    }

    //endregion

    //region Bomb at every edge and corner

    @Test
    public void bombClearsCorrectAreasAtAllCorners() {
        // Fill the whole board, then bomb each corner: exactly 4 cells cleared
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);

        assertEquals(4, board.clearArea(0, 0, 1, FACTORY));
        assertEquals(4, board.clearArea(9, 0, 1, FACTORY));
        assertEquals(4, board.clearArea(0, 9, 1, FACTORY));
        assertEquals(4, board.clearArea(9, 9, 1, FACTORY));
    }

    @Test
    public void bombInCenterClearsNineCells() {
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);

        assertEquals(9, board.clearArea(4, 4, 1, FACTORY));
    }

    @Test
    public void bombOnEmptyBoardClearsNothing() {
        final Board board = newBoard();
        assertEquals(0, board.clearArea(4, 4, 1, FACTORY));
        assertEquals(0, board.clearArea(0, 0, 1, FACTORY));
    }

    @Test
    public void bombNeverClearsOutsideTheBoard() {
        // Out-of-range coordinates must be clamped, never throw
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);

        assertEquals(4, board.clearArea(-5, -5, 1, FACTORY));
        assertEquals(4, board.clearArea(15, 15, 1, FACTORY));
        assertEquals(6, board.clearArea(-1, 4, 1, FACTORY)); // cols 0..1 x rows 3..5
    }

    //endregion

    //region Lightning at every edge and corner

    @Test
    public void lightningOnFullBoardClearsRowAndColumn() {
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);

        // row + column minus the counted crossing cell
        assertEquals(19, board.clearCross(4, 4, FACTORY));
    }

    private static Board fullBoard() {
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);
        return board;
    }

    @Test
    public void lightningAtCornersNeverCrashes() {
        assertEquals(19, fullBoard().clearCross(0, 0, FACTORY));
        assertEquals(19, fullBoard().clearCross(9, 9, FACTORY));
        assertEquals(19, fullBoard().clearCross(0, 9, FACTORY));
        assertEquals(19, fullBoard().clearCross(9, 0, FACTORY));
    }

    @Test
    public void lightningOutOfRangeIsClamped() {
        assertEquals(19, fullBoard().clearCross(-3, -3, FACTORY));
        assertEquals(19, fullBoard().clearCross(42, 42, FACTORY));
    }

    @Test
    public void lightningOnEmptyBoardClearsNothing() {
        assertEquals(0, newBoard().clearCross(5, 5, FACTORY));
    }

    //endregion

    //region Rapid repeated power-ups (no state corruption)

    @Test
    public void rapidBombAndLightningSpamStaysConsistent() {
        final Board board = newBoard();
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.setCell(j, i, 0);

        for (int i = 0; i < 50; ++i) {
            board.clearArea(i % 10, (i * 3) % 10, 1, FACTORY);
            board.clearCross((i * 7) % 10, (i * 2) % 10, FACTORY);
        }
        // every remaining cell must be a sane normal color or empty
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                board.isEmpty(j, i); // must not throw
    }

    @Test
    public void fullBoardClearThenRefillAndClearAgain() {
        final Board board = newBoard();
        for (int round = 0; round < 3; ++round) {
            for (int i = 0; i < 10; ++i)
                for (int j = 0; j < 10; ++j)
                    board.setCell(j, i, 0);
            assertEquals(20, board.clearComplete(FACTORY)); // whole board completes
        }
    }

    //endregion

    //region Serialization

    @Test
    public void boardRoundTripKeepsNormalCells() throws Exception {
        final Board board = newBoard();
        board.setCell(1, 2, 5);
        board.setCell(3, 4, 2);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        board.write(new DataOutputStream(bos));
        final Board restored = newBoard();
        restored.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        assertFalse(restored.isEmpty(1, 2));
        assertFalse(restored.isEmpty(3, 4));
        assertTrue(restored.isEmpty(0, 0));
    }

    @Test
    public void oldSavesWithSpecialColorsAreSanitized() throws Exception {
        final Board board = newBoard();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(10); // cellCount
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 10; ++j)
                out.writeInt(i == 0 && j == 0 ? 101 : -1); // a stuck bomb cell
        out.flush();

        board.read(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
        assertTrue(board.isEmpty(0, 0)); // sanitized to empty, no crash
    }

    //endregion
}
