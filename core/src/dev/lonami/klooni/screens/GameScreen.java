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
package dev.lonami.klooni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.game.BaseScorer;
import dev.lonami.klooni.game.Board;
import dev.lonami.klooni.game.BonusParticleHandler;
import dev.lonami.klooni.game.GameLayout;
import dev.lonami.klooni.game.Missions;
import dev.lonami.klooni.game.Achievements;
import dev.lonami.klooni.game.LevelCatalog;
import dev.lonami.klooni.game.LevelDefinition;
import dev.lonami.klooni.game.LevelProgress;
import dev.lonami.klooni.game.Piece;
import dev.lonami.klooni.game.PieceHolder;
import dev.lonami.klooni.game.Scorer;
import dev.lonami.klooni.game.TimeScorer;

import java.util.Random;
import dev.lonami.klooni.serializer.BinSerializable;
import dev.lonami.klooni.serializer.BinSerializer;

// Main game screen. Here the board, piece holder and score are shown
class GameScreen implements Screen, InputProcessor, BinSerializable {

    //region Members

    private final Klooni game;
    private final BaseScorer scorer;
    private final BonusParticleHandler bonusParticleHandler;

    private final Board board;
    private final PieceHolder holder;

    private final SpriteBatch batch;
    private final Sound gameOverSound;

    private final PauseMenuStage pauseMenu;

    // TODO Perhaps make an abstract base class for the game screen and game modes
    // by implementing different "isGameOver" etc. logic instead using an integer?
    private final int gameMode;

    private boolean gameOverDone;

    // Star Puzzle juice
    private int combo;
    private float shakeTime;
    private final Matrix4 shakeMatrix = new Matrix4();
    private int lastMissionScore;

    // Star Puzzle: adventure level mode state
    private LevelDefinition level;
    private boolean dailyLevel;
    private int movesUsed;
    private int levelLines;
    private int maxCombo;
    private float timeLeft;
    private boolean levelFinished;
    private final Label objectiveLabel;

    // Star Puzzle HUD extras
    private final Label comboLabel;
    private final Label recordBanner;
    private float recordBannerTime;
    private boolean recordShown;
    private int lastLevel;

    // The last score that was saved when adding the money.
    // We use this so we don't add the same old score to the money twice,
    // but rather subtract it from the current score and then update it
    // with the current score to get the "increase" of money score.
    private int savedMoneyScore;

    //endregion

    //region Static members

    private final static int BOARD_SIZE = 10;
    private final static int HOLDER_PIECE_COUNT = 3;

    final static int GAME_MODE_SCORE = 0;
    final static int GAME_MODE_TIME = 1;
    final static int GAME_MODE_LEVEL = 2;

    private final static String SAVE_DAT_FILENAME = ".klooni.sav";

    //endregion

    //region Constructor

    // Load any previously saved file by default
    GameScreen(final Klooni game, final int gameMode) {
        this(game, gameMode, true);
    }

    // Star Puzzle: adventure level constructor
    GameScreen(final Klooni game, final LevelDefinition level) {
        this(game, level, false);
    }

    GameScreen(final Klooni game, final LevelDefinition level, final boolean daily) {
        batch = new SpriteBatch();
        this.game = game;
        this.gameMode = GAME_MODE_LEVEL;
        this.level = level;
        this.dailyLevel = daily;

        final GameLayout layout = new GameLayout();
        scorer = new Scorer(game, layout);

        board = new Board(layout, BOARD_SIZE);
        holder = new PieceHolder(layout, board, HOLDER_PIECE_COUNT, board.cellSize);
        pauseMenu = new PauseMenuStage(layout, game, scorer, gameMode, level);
        bonusParticleHandler = new BonusParticleHandler(game);

        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("sound/game_over.mp3"));

        Label.LabelStyle comboStyle = new Label.LabelStyle();
        comboStyle.font = game.skin.getFont("font_small");
        comboLabel = new Label("", comboStyle);
        comboLabel.setAlignment(Align.center);

        Label.LabelStyle bannerStyle = new Label.LabelStyle();
        bannerStyle.font = game.skin.getFont("font");
        recordBanner = new Label("NEW RECORD!", bannerStyle);
        recordBanner.setAlignment(Align.center);

        Label.LabelStyle objectiveStyle = new Label.LabelStyle();
        objectiveStyle.font = game.skin.getFont("font_small");
        objectiveLabel = new Label("", objectiveStyle);
        objectiveLabel.setAlignment(Align.center);

        // Level setup: obstacle pre-fill and special piece chance
        prefillBoard();
        Piece.specialChance = level.specialChance;

        movesUsed = 0;
        levelLines = 0;
        maxCombo = 0;
        timeLeft = level.timeLimit > 0 ? level.timeLimit : 0f;
        levelFinished = false;

        Achievements.onGameStarted();
    }

    GameScreen(final Klooni game, final int gameMode, final boolean loadSave) {
        batch = new SpriteBatch();
        this.game = game;
        this.gameMode = gameMode;
        Piece.specialChance = 5;

        final GameLayout layout = new GameLayout();
        switch (gameMode) {
            case GAME_MODE_SCORE:
                scorer = new Scorer(game, layout);
                break;
            case GAME_MODE_TIME:
                scorer = new TimeScorer(game, layout);
                break;
            default:
                throw new RuntimeException("Unknown game mode given: " + gameMode);
        }

        board = new Board(layout, BOARD_SIZE);
        holder = new PieceHolder(layout, board, HOLDER_PIECE_COUNT, board.cellSize);
        pauseMenu = new PauseMenuStage(layout, game, scorer, gameMode);
        bonusParticleHandler = new BonusParticleHandler(game);

        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("sound/game_over.mp3"));

        // Star Puzzle: floating combo indicator and record banner
        Label.LabelStyle comboStyle = new Label.LabelStyle();
        comboStyle.font = game.skin.getFont("font_small");
        comboLabel = new Label("", comboStyle);
        comboLabel.setAlignment(Align.center);

        Label.LabelStyle bannerStyle = new Label.LabelStyle();
        bannerStyle.font = game.skin.getFont("font");
        recordBanner = new Label("NEW RECORD!", bannerStyle);
        recordBanner.setAlignment(Align.center);

        Label.LabelStyle objectiveStyle = new Label.LabelStyle();
        objectiveStyle.font = game.skin.getFont("font_small");
        objectiveLabel = new Label("", objectiveStyle);
        objectiveLabel.setAlignment(Align.center);

        if (gameMode == GAME_MODE_SCORE) {
            if (loadSave) {
                // The user might have a previous game. If this is the case, load it
                if (!tryLoad()) {
                    System.err.println("failed to load previous games");
                }
            } else {
                // Ensure that there is no old save, we don't want to load it, thus delete it
                deleteSave();
            }
        }
    }

    //endregion

    //region Private methods

    // If no piece can be put, then it is considered to be game over
    private boolean isGameOver() {
        for (Piece piece : holder.getAvailablePieces())
            if (board.canPutPiece(piece))
                return false;

        return true;
    }

    private void doGameOver(final String gameOverReason) {
        if (!gameOverDone) {
            gameOverDone = true;
            Klooni.vibrate(200);

            saveMoney();
            holder.enabled = false;
            pauseMenu.showGameOver(gameOverReason, scorer instanceof TimeScorer);
            if (Klooni.soundsEnabled())
                gameOverSound.play();

            // The user should not be able to return to the game if its game over
            if (gameMode == GAME_MODE_SCORE)
                deleteSave();
        }
    }

    //endregion

    //region Screen

    @Override
    public void show() {
        if (pauseMenu.isShown() || levelFinished) // Menu shown or level result dialog
            Gdx.input.setInputProcessor(pauseMenu);
        else
            Gdx.input.setInputProcessor(this);
    }

    // Save the state, the user might leave the game in any of the following 2 methods
    private void showPauseMenu() {
        saveMoney();
        pauseMenu.show();
        save();
    }

    @Override
    public void pause() {
        save();
    }

    @Override
    public void render(float delta) {
        Klooni.theme.glClearBackground();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (scorer.isGameOver() && !pauseMenu.isShown()) {
            // TODO A bit hardcoded (timeOver = scorer instanceof TimeScorer)
            // Perhaps have a better mode to pass the required texture to overlay
            doGameOver(scorer.gameOverReason());
        }

        // Star Puzzle: adventure level timer and objective tracking
        if (level != null && !levelFinished && !pauseMenu.isShown()) {
            if (level.timeLimit > 0) {
                timeLeft -= delta;
                if (timeLeft <= 0f) {
                    timeLeft = 0f;
                    finishLevel(false, timeDetail());
                }
            }
        }

        batch.begin();

        // Star Puzzle: screen shake while there's shake time left
        if (shakeTime > 0f) {
            shakeTime -= Gdx.graphics.getDeltaTime();
            final float amplitude = Math.max(0f, shakeTime) * 90f;
            shakeMatrix.setToTranslation(
                    MathUtils.random(-amplitude, amplitude),
                    MathUtils.random(-amplitude, amplitude), 0f);
            batch.setTransformMatrix(shakeMatrix);
        } else if (shakeMatrix.val[Matrix4.M03] != 0f || shakeMatrix.val[Matrix4.M13] != 0f) {
            // The transform matrix persists between frames, so reset it once
            shakeMatrix.idt();
            batch.setTransformMatrix(shakeMatrix);
        }

        scorer.draw(batch);
        board.draw(batch);
        drawPowerUpPreview(batch);
        holder.update();
        holder.draw(batch);
        bonusParticleHandler.run(batch);

        // Star Puzzle: persistent combo indicator over the board
        if (combo >= 2) {
            comboLabel.setText("COMBO x" + combo);
            final float pulse = 0.5f + 0.5f * (float) Math.sin((double) System.nanoTime() * 2e-8);
            comboLabel.setColor(1f, 0.75f, 0.2f, Math.max(0.45f, pulse));
            comboLabel.setBounds(
                    board.pos.x, board.pos.y + board.cellCount * board.cellSize - board.cellSize * 0.8f,
                    board.cellCount * board.cellSize, board.cellSize * 0.8f);
            comboLabel.draw(batch, 1f);
        }

        // Star Puzzle: adventure objective HUD, drawn above the board
        if (level != null && !levelFinished) {
            objectiveLabel.setText(objectiveText());
            objectiveLabel.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                    Klooni.theme.foreground.b, 0.85f);
            objectiveLabel.setBounds(
                    0f, board.pos.y + board.cellCount * board.cellSize + board.cellSize * 0.15f,
                    Gdx.graphics.getWidth(), board.cellSize * 0.7f);
            objectiveLabel.draw(batch, 1f);
        }

        // Star Puzzle: new record celebration banner
        if (recordBannerTime > 0f) {
            recordBannerTime -= Gdx.graphics.getDeltaTime();
            final float alpha = Math.min(1f, recordBannerTime * 2f);
            final float pulse = 1.05f + 0.05f * (float) Math.sin((double) System.nanoTime() * 3e-8);
            recordBanner.setColor(1f, 0.85f, 0.25f, alpha);
            recordBanner.setBounds(
                    0f, board.pos.y + board.cellCount * board.cellSize * 0.4f,
                    Gdx.graphics.getWidth(), board.cellSize * pulse);
            recordBanner.draw(batch, 1f);
        }

        batch.end();

        // Star Puzzle: fire the celebration the first moment we beat the record
        if (!recordShown && scorer.getCurrentScore() > 0 && scorer.isRecord()) {
            recordShown = true;
            recordBannerTime = 3f;
            shakeTime = 0.4f;
            Klooni.playStarSound();
            Klooni.vibrate(150);
        }

        if (pauseMenu.isShown() || pauseMenu.isHiding()) {
            pauseMenu.act(delta);
            pauseMenu.draw();
        }
    }

    @Override
    public void dispose() {
        pauseMenu.dispose();
    }

    //endregion

    //region Input

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.P || keycode == Input.Keys.BACK) { // Pause
            if (!levelFinished)
                showPauseMenu();
        }

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return holder.pickPiece();
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        PieceHolder.DropResult result = holder.dropPiece();
        if (!result.dropped)
            return false;

        if (result.onBoard) {
            scorer.addPieceScore(result.area);

            // Star Puzzle: handle special pieces (star / bomb / lightning)
            if (result.pieceColorIndex >= Piece.SPECIAL_STAR)
                handleSpecialPiece(result);

            final int cleared = board.clearComplete(game.effect);
            final int bonus = scorer.addBoardScore(cleared, board.cellCount);
            if (cleared > 0) {
                // Star Puzzle: combo system — consecutive clearing pieces
                combo++;
                if (combo > maxCombo)
                    maxCombo = combo;
                Achievements.onCombo(combo);
                if (combo >= 2) {
                    final int comboBonus = bonus * (combo - 1);
                    scorer.addPieceScore(comboBonus);
                    bonusParticleHandler.addMessage(result.pieceCenter,
                            "COMBO x" + combo + "  +" + comboBonus);
                    Klooni.playComboSound(combo);
                    Klooni.vibrate(60);
                }

                shakeTime = Math.min(0.12f + cleared * 0.08f, 0.5f);
                bonusParticleHandler.addBonus(result.pieceCenter, bonus);
                if (Klooni.soundsEnabled()) {
                    Klooni.playLineClearSound();
                    game.playEffectSound();
                }
                Klooni.vibrate(35);

                Missions.onLinesCleared(cleared);
                Achievements.onLinesCleared(cleared);
                if (level != null)
                    levelLines += cleared;
            } else {
                combo = 0;
            }

            // Star Puzzle: level system, a new banner every 500 points
            final int scoreLevel = scorer.getCurrentScore() / 500 + 1;
            if (scoreLevel > lastLevel) {
                if (lastLevel > 0) {
                    bonusParticleHandler.addMessage(
                            board.cellCenter(board.cellCount / 2, board.cellCount / 2),
                            "LEVEL " + scoreLevel);
                    Klooni.playComboSound(scoreLevel);
                    Klooni.vibrate(60);
                }
                lastLevel = scoreLevel;
            }

            // Star Puzzle: daily missions progress
            Missions.onPiecesPlaced(result.area);
            Missions.onScore(scorer.getCurrentScore(), lastMissionScore);
            lastMissionScore = scorer.getCurrentScore();
            Achievements.onPiecesPlaced(result.area);

            // Star Puzzle: adventure level progression
            if (level != null && !levelFinished) {
                movesUsed++;

                if (isLevelObjectiveComplete()) {
                    finishLevel(true, objectiveDetail());
                } else if (level.maxMoves > 0 && movesUsed >= level.maxMoves) {
                    finishLevel(false, objectiveDetail());
                } else if (isGameOver()) {
                    finishLevel(false, "No space left on the board");
                }
            }
            // After the piece was put, check if it's game over
            else if (isGameOver()) {
                doGameOver("no moves left");
            }
        }
        return true;
    }

    //endregion

    //region Unused methods

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() { /* Hide can only be called if the menu was shown. Place logic there. */ }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    //endregion

    //region Star Puzzle specials

    // Preview colors: golden tint = the drop will work, soft red = it won't
    private static final Color PREVIEW_VALID = new Color(1f, 0.85f, 0.35f, 0.28f);
    private static final Color PREVIEW_INVALID = new Color(0.9f, 0.25f, 0.2f, 0.22f);

    // Star Puzzle: power-up targeting preview. While a special piece is
    // dragged over the board, the exact cells it will affect are highlighted
    // BEFORE the finger is lifted, so the player always knows what will
    // happen: 3x3 area for the bomb, full row + column for the lightning,
    // single cell for the star. The preview uses the SAME screen-to-cell
    // conversion as the actual drop, so it can never lie.
    private void drawPowerUpPreview(final SpriteBatch batch) {
        final Piece held = holder.getHeldPiece();
        if (held == null || !held.isSpecial())
            return;

        final int x = board.screenToCellX(held);
        final int y = board.screenToCellY(held);
        final int n = board.cellCount;
        if (x < 0 || y < 0 || x >= n || y >= n)
            return; // outside the board: nothing to preview

        final boolean valid = board.isEmpty(x, y);
        batch.setColor(valid ? PREVIEW_VALID : PREVIEW_INVALID);

        final float px = board.pos.x, py = board.pos.y, cs = board.cellSize;
        switch (held.colorIndex) {
            case Piece.SPECIAL_BOMB: {
                for (int i = Math.max(0, y - 1); i <= Math.min(n - 1, y + 1); ++i)
                    for (int j = Math.max(0, x - 1); j <= Math.min(n - 1, x + 1); ++j)
                        batch.draw(Klooni.theme.cellTexture, px + j * cs, py + i * cs, cs, cs);
                break;
            }
            case Piece.SPECIAL_LIGHTNING: {
                for (int j = 0; j < n; ++j)
                    batch.draw(Klooni.theme.cellTexture, px + j * cs, py + y * cs, cs, cs);
                for (int i = 0; i < n; ++i)
                    batch.draw(Klooni.theme.cellTexture, px + x * cs, py + i * cs, cs, cs);
                break;
            }
            default: { // star: just the landing cell
                batch.draw(Klooni.theme.cellTexture, px + x * cs, py + y * cs, cs, cs);
                break;
            }
        }

        // Selected-state glow: soft pulsing halo behind the dragged piece
        final float pulse = 0.5f + 0.5f * (float) Math.sin((double) System.nanoTime() * 2e-8);
        batch.setColor(1f, 0.85f, 0.35f, 0.10f + 0.08f * pulse);
        final Vector2 hp = held.getPos();
        final float hcs = held.getCellSize();
        batch.draw(Klooni.theme.cellTexture,
                hp.x - hcs * 0.35f, hp.y - hcs * 0.35f, hcs * 1.7f, hcs * 1.7f);

        batch.setColor(Color.WHITE);
    }

    // Executes a dropped power-up piece on the cell where it ACTUALLY landed.
    // The landing cell comes from Board.putPiece (board.lastPutCellX/Y), the
    // same code path that validated the drop — the target and the placement
    // can never disagree. The old code recomputed the cell from the dragged
    // piece's on-screen center, which lags behind the finger and produced an
    // off-by-one target: out-of-bounds crashes at the edges and effects that
    // hit the wrong cells.
    private void handleSpecialPiece(final PieceHolder.DropResult result) {
        final int x = board.lastPutCellX;
        final int y = board.lastPutCellY;
        if (x < 0 || y < 0)
            return; // no landing cell recorded: nothing was placed

        switch (result.pieceColorIndex) {
            case Piece.SPECIAL_STAR: {
                // +150 points, the cell becomes a normal colored cell
                board.setCell(x, y, MathUtils.random(7));
                Achievements.onSpecialUsed();
                scorer.addPieceScore(150);
                bonusParticleHandler.addMessage(board.cellCenter(x, y), "+150");
                Klooni.playStarSound();
                Klooni.vibrate(40);
                break;
            }
            case Piece.SPECIAL_BOMB: {
                final int clearedCells = board.clearArea(x, y, 1, game.effect);
                Achievements.onSpecialUsed();
                scorer.addPieceScore(clearedCells * 2);
                bonusParticleHandler.addMessage(board.cellCenter(x, y), "BOOM!");
                Klooni.playBombSound();
                // Satisfying but never violent: short, small-amplitude shake
                shakeTime = 0.3f;
                Klooni.vibrate(90);
                break;
            }
            case Piece.SPECIAL_LIGHTNING: {
                final int clearedCells = board.clearCross(x, y, game.effect);
                Achievements.onSpecialUsed();
                scorer.addPieceScore(clearedCells * 2);
                bonusParticleHandler.addMessage(board.cellCenter(x, y), "ZAP!");
                Klooni.playComboSound(3);
                shakeTime = 0.25f;
                Klooni.vibrate(70);
                break;
            }
        }
    }

    //endregion

    //region Star Puzzle adventure levels

    // Fills the board with obstacle cells based on the level's seed and
    // density. Full rows and columns are avoided so the level is always
    // playable from the start.
    private void prefillBoard() {
        if (level == null || level.prefillDensity <= 0)
            return;

        final Random random = new Random(level.seed);
        final int cellCount = board.cellCount;
        final int targetCells = cellCount * cellCount * level.prefillDensity / 100;

        int placed = 0;
        while (placed < targetCells) {
            final int x = random.nextInt(cellCount);
            final int y = random.nextInt(cellCount);
            if (board.isEmpty(x, y)) {
                board.setCell(x, y, random.nextInt(8));
                ++placed;
            }
            // Avoid creating pre-cleared lines: clear them and stop filling
            // when a complete line appears (defensive, next loop breaks it)
            if (board.clearComplete(game.effect) > 0)
                placed = Math.max(0, placed - cellCount);
        }

        // Never leave a fully blocked corner: clear a 2x2 area at the center
        board.setCell(cellCount / 2, cellCount / 2, -1);
        board.setCell(cellCount / 2 - 1, cellCount / 2, -1);
        board.setCell(cellCount / 2, cellCount / 2 - 1, -1);
        board.setCell(cellCount / 2 - 1, cellCount / 2 - 1, -1);
    }

    private boolean isLevelObjectiveComplete() {
        switch (level.objectiveType) {
            case LevelDefinition.TYPE_LINES:
                return levelLines >= level.targetLines;
            case LevelDefinition.TYPE_COMBO:
                return maxCombo >= level.targetCombo;
            default:
                return scorer.getCurrentScore() >= level.targetScore;
        }
    }

    // Fraction of the move/time allowance left, used for stars
    private float allowanceLeftFraction() {
        if (level.timeLimit > 0)
            return Math.max(0f, timeLeft / (float) level.timeLimit);
        if (level.maxMoves > 0)
            return Math.max(0f, (level.maxMoves - movesUsed) / (float) level.maxMoves);
        return 0f;
    }

    private String objectiveText() {
        final String allowance = level.timeLimit > 0
                ? ((int) timeLeft + "s")
                : (movesUsed + "/" + level.maxMoves + " moves");
        switch (level.objectiveType) {
            case LevelDefinition.TYPE_LINES:
                return levelLines + "/" + level.targetLines + " lines  |  " + allowance;
            case LevelDefinition.TYPE_COMBO:
                return "combo x" + maxCombo + "/x" + level.targetCombo + "  |  " + allowance;
            default:
                return scorer.getCurrentScore() + "/" + level.targetScore + "  |  " + allowance;
        }
    }

    private String objectiveDetail() {
        return objectiveText();
    }

    private String timeDetail() {
        return "Time is up!  " + scorer.getCurrentScore() + "/" + level.targetScore;
    }

    // Completes (or fails) the adventure level, awarding stars and coins
    // through the real progression system, then shows the result dialog.
    private void finishLevel(final boolean won, final String detail) {
        if (levelFinished)
            return;
        levelFinished = true;
        gameOverDone = true; // Prevent the endless game-over path
        holder.enabled = false;

        int stars = 0;
        int coins = 0;
        if (won) {
            stars = level.starsFor(allowanceLeftFraction());

            // Coins are paid only when progress improves, so levels
            // cannot be farmed for infinite coins.
            final boolean improved = LevelProgress.setStars(level.id, stars);
            if (improved)
                coins = Math.round(level.rewardCoins * stars / 3f);

            if (dailyLevel && !LevelProgress.isDailyDone()) {
                LevelProgress.markDailyDone();
                coins += 25; // daily challenge bonus
            }

            if (coins > 0)
                Klooni.addMoney(coins);

            if (Klooni.soundsEnabled())
                Klooni.playStarSound();
            Klooni.vibrate(120);
            Achievements.onLevelCompleted();
        } else {
            if (Klooni.soundsEnabled())
                gameOverSound.play();
            Klooni.vibrate(200);
        }

        final ResultDialog dialog = new ResultDialog(
                game, game.skin, level, won, stars, coins, detail, dailyLevel);
        dialog.pack();
        dialog.show(pauseMenu);
        dialog.setPosition((pauseMenu.getWidth() - dialog.getWidth()) * 0.5f,
                (pauseMenu.getHeight() - dialog.getHeight()) * 0.5f);
        Gdx.input.setInputProcessor(pauseMenu);
    }

    //endregion

    //region Saving and loading

    private void saveMoney() {
        // Adventure levels pay coins through level rewards only
        if (level != null)
            return;

        // Calculate new money since the previous saving
        int nowScore = scorer.getCurrentScore();
        int newMoneyScore = nowScore - savedMoneyScore;
        savedMoneyScore = nowScore;
        Klooni.addMoneyFromScore(newMoneyScore);
    }

    private void save() {
        // Only save if the game is not over and the game mode is not the time mode. It
        // makes no sense to save the time game mode since it's supposed to be something quick.
        // Don't save either if the score is 0, which means the player did nothing.
        if (gameOverDone || gameMode != GAME_MODE_SCORE || scorer.getCurrentScore() == 0)
            return;

        final FileHandle handle = Gdx.files.local(SAVE_DAT_FILENAME);
        try {
            BinSerializer.serialize(this, handle.write(false));
        } catch (IOException e) {
            // Should never happen but what else could be done if the game wasn't saved?
            e.printStackTrace();
        }
    }

    private static void deleteSave() {
        final FileHandle handle = Gdx.files.local(SAVE_DAT_FILENAME);
        if (handle.exists())
            handle.delete();
    }

    static boolean hasSavedData() {
        return Gdx.files.local(SAVE_DAT_FILENAME).exists();
    }

    private boolean tryLoad() {
        final FileHandle handle = Gdx.files.local(SAVE_DAT_FILENAME);
        if (handle.exists()) {
            try {
                BinSerializer.deserialize(this, handle.read());
                // No cheating! We need to load the previous money
                // or it would seem like we earned it on this game
                savedMoneyScore = scorer.getCurrentScore();

                // After it's been loaded, delete the save file
                deleteSave();
                return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        // gameMode, board, holder, scorer
        out.writeInt(gameMode);
        board.write(out);
        holder.write(out);
        scorer.write(out);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        int savedGameMode = in.readInt();
        if (savedGameMode != gameMode)
            throw new IOException("A different game mode was saved. Cannot load the save data.");

        board.read(in);
        holder.read(in);
        scorer.read(in);
    }

    //endregion
}
