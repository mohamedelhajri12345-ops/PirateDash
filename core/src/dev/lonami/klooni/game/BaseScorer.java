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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;
import dev.lonami.klooni.serializer.BinSerializable;

public abstract class BaseScorer implements BinSerializable {

    //region Members

    int currentScore;

    final Label currentScoreLabel;
    final Label highScoreLabel;
    final Label scoreCaption;
    final Label bestCaption;

    final Texture cupTexture;
    final Texture panelTexture;
    final Rectangle cupArea;
    final Rectangle hudArea;

    private final Color cupColor;

    // To interpolate between shown score -> real score
    private float shownScore;

    //endregion

    //region Constructor

    // The board size is required when calculating the score
    BaseScorer(final Klooni game, GameLayout layout, int highScore) {
        cupTexture = SkinLoader.loadPng("cup.png");
        cupColor = Klooni.theme.currentScore.cpy();
        cupArea = new Rectangle();

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = game.skin.getFont("font");

        currentScoreLabel = new Label("0", labelStyle);
        currentScoreLabel.setAlignment(Align.left);

        highScoreLabel = new Label(Integer.toString(highScore), labelStyle);
        highScoreLabel.setAlignment(Align.right);

        Label.LabelStyle captionStyle = new Label.LabelStyle();
        captionStyle.font = game.skin.getFont("font_small");

        scoreCaption = new Label("SCORE", captionStyle);
        scoreCaption.setAlignment(Align.left);

        bestCaption = new Label("BEST", captionStyle);
        bestCaption.setAlignment(Align.right);

        panelTexture = SkinLoader.loadPng("pixel");
        hudArea = new Rectangle();

        layout.update(this);
    }

    //endregion

    //region Private methods

    // The original game seems to work as follows:
    // If < 1 were cleared, score = 0
    // If = 1  was cleared, score = cells cleared
    // If > 1 were cleared, score = cells cleared + score(cleared - 1)
    final int calculateClearScore(int stripsCleared, int boardSize) {
        if (stripsCleared < 1) return 0;
        if (stripsCleared == 1) return boardSize;
        else return boardSize * stripsCleared + calculateClearScore(stripsCleared - 1, boardSize);
    }

    //endregion

    //region Public methods

    // Adds the score a given piece would give
    public void addPieceScore(final int areaPut) {
        currentScore += areaPut;
    }

    // Adds the score given by the board, this is, the count of cleared strips
    public int addBoardScore(int stripsCleared, int boardSize) {
        int score = calculateClearScore(stripsCleared, boardSize);
        currentScore += score;
        return score;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void pause() {
    }

    public void resume() {
    }

    abstract public boolean isGameOver();

    abstract protected boolean isNewRecord();

    public String gameOverReason() {
        return "";
    }

    abstract public void saveScore();

    public void draw(SpriteBatch batch) {
        // Translucent panel behind the whole HUD
        batch.setColor(0f, 0f, 0f, 0.22f);
        batch.draw(panelTexture, hudArea.x, hudArea.y, hudArea.width, hudArea.height);
        batch.setColor(1f, 1f, 1f, 1f);

        // Cup pulses when we're beating the record
        float pulse = 1f;
        if (isNewRecord())
            pulse = 1.06f + 0.05f * (float) Math.sin((double) System.nanoTime() * 1e-8);

        cupColor.lerp(isNewRecord() ? Klooni.theme.highScore : Klooni.theme.currentScore, 0.05f);
        batch.setColor(cupColor);
        batch.draw(cupTexture,
                cupArea.x - (cupArea.width * pulse - cupArea.width) * 0.5f,
                cupArea.y - (cupArea.height * pulse - cupArea.height) * 0.5f,
                cupArea.width * pulse, cupArea.height * pulse);

        int roundShown = MathUtils.round(shownScore);
        if (roundShown != currentScore) {
            shownScore = Interpolation.linear.apply(shownScore, currentScore, 0.1f);
            currentScoreLabel.setText(Integer.toString(MathUtils.round(shownScore)));
        }

        // Captions in a soft translucent tone
        scoreCaption.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                Klooni.theme.foreground.b, 0.55f);
        scoreCaption.draw(batch, 1f);
        bestCaption.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                Klooni.theme.foreground.b, 0.55f);
        bestCaption.draw(batch, 1f);

        currentScoreLabel.setColor(Klooni.theme.currentScore);
        currentScoreLabel.draw(batch, 1f);

        highScoreLabel.setColor(Klooni.theme.highScore);
        highScoreLabel.draw(batch, 1f);
    }

    // Star Puzzle: public record check for celebrations
    public boolean isRecord() {
        return isNewRecord();
    }

    //endregion
}
