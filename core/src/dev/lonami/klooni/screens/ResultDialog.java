/*
    Star Puzzle — World Puzzle Adventure
    A data-driven level system built on top of 1010! Klooni (GPL-3.0, Lonami Exo).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
*/
package dev.lonami.klooni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;
import dev.lonami.klooni.game.LevelDefinition;

// End-of-level result dialog: shows the outcome, earned stars, coins,
// and offers next / replay / world map navigation. Fully functional:
// every button transitions to a real screen.
class ResultDialog extends Dialog {

    //region Members

    private final Klooni game;
    private final LevelDefinition level;
    private final boolean won;
    private final boolean daily;

    //endregion

    //region Constructor

    // stars = 0 when the level was failed
    ResultDialog(final Klooni game, final Skin skin, final LevelDefinition level,
                 final boolean won, final int stars, final int coins,
                 final String detailLine, final boolean daily) {
        super(won
                ? (daily ? "DAILY COMPLETE!" : (level.isBoss() ? "BOSS DEFEATED!" : "LEVEL COMPLETE!"))
                : "TRY AGAIN", skin, "dialog");

        this.game = game;
        this.level = level;
        this.won = won;
        this.daily = daily;

        final Label coinsLabel = new Label("Coins: " + Klooni.getMoney(), skin);
        getContentTable().add(coinsLabel).padBottom(8);

        // Stars row: earned stars shine gold, the rest stay dim
        getContentTable().row();
        final float starSize = Math.min(
                Gdx.graphics.getWidth() * 0.16f, Gdx.graphics.getHeight() * 0.1f);
        for (int i = 0; i < 3; ++i) {
            final Image star = new Image(dev.lonami.klooni.Theme.skin.getDrawable("star_texture"));
            if (won && i < stars)
                star.setColor(1f, 0.84f, 0.1f, 1f);
            else
                star.setColor(0.35f, 0.35f, 0.4f, 0.6f);
            getContentTable().add(star).size(starSize).pad(4);
        }

        getContentTable().row();
        final Label detail = new Label(detailLine, skin);
        detail.setAlignment(Align.center);
        getContentTable().add(detail).padTop(10);

        if (won && coins > 0) {
            getContentTable().row();
            final Label earned = new Label("+" + coins + " coins!", skin);
            earned.setColor(1f, 0.84f, 0.1f, 1f);
            getContentTable().add(earned).padTop(6);
        }

        // Next level (only when it is unlocked or this was the daily challenge)
        if (won && !daily && level.id < 1000
                && dev.lonami.klooni.game.LevelProgress.isLevelUnlocked(level.id + 1)) {
            final TextButton nextButton = new TextButton("NEXT", skin);
            nextButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    hide();
                    game.transitionTo(new GameScreen(game,
                            dev.lonami.klooni.game.LevelCatalog.getLevel(level.id + 1)));
                }
            });
            getButtonTable().add(nextButton);
        }

        // Replay
        final TextButton replayButton = new TextButton("REPLAY", skin);
        replayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                if (daily)
                    game.transitionTo(new GameScreen(game, dev.lonami.klooni.game.LevelCatalog.dailyLevel()));
                else
                    game.transitionTo(new GameScreen(game, level));
            }
        });
        getButtonTable().add(replayButton).padLeft(12);

        // Back to the world map
        final TextButton mapButton = new TextButton("MAP", skin);
        mapButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                game.transitionTo(new WorldMapScreen(game));
            }
        });
        getButtonTable().add(mapButton).padLeft(12);

        // The result dialog cannot be dismissed by tapping outside or
        // with the back key; the player must pick an option.
        setModal(true);
    }

    @Override
    protected void result(Object object) {
        // no-op: navigation happens in the button listeners
    }

    @Override
    public void cancel() {
        // ignore: the player must pick one of the dialog options
    }

    //endregion
}
