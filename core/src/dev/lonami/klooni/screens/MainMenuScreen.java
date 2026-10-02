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
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;
import dev.lonami.klooni.actors.SoftButton;

// Main menu screen, presenting some options (play, customize…)
// Star Puzzle redesign: mobile-first vertical layout with the
// logo on top, a big play button and a tidy grid of icon buttons.
public class MainMenuScreen extends InputListener implements Screen {

    //region Members

    private final Klooni game;
    private final Stage stage;

    private boolean giftChecked;

    //endregion

    //region Static members

    // As the examples show on the LibGdx wiki
    private static final float minDelta = 1 / 30f;

    //endregion

    //region Constructor

    public MainMenuScreen(Klooni game) {
        this.game = game;

        stage = new Stage();

        final float width = Gdx.graphics.getWidth();
        final float height = Gdx.graphics.getHeight();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Game logo, a wide banner
        final Texture logoTexture = SkinLoader.loadPng("logo");
        final Image logo = new Image(logoTexture);
        table.add(logo).colspan(3)
                .size(width * 0.72f, height * 0.15f)
                .padTop(height * 0.02f).space(height * 0.015f);

        table.row();

        // Play button, the star of the menu
        final SoftButton playButton = new SoftButton(
                0, GameScreen.hasSavedData() ? "play_saved_texture" : "play_texture");
        playButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(
                        new GameScreen(MainMenuScreen.this.game, GameScreen.GAME_MODE_SCORE));
            }
        });
        table.add(playButton).colspan(3).size(width * 0.62f, height * 0.13f).space(height * 0.02f);

        table.row();

        // First row of icon buttons
        final float iconSize = Math.min(width * 0.24f, height * 0.11f);

        // Time mode
        final SoftButton stopwatchButton = new SoftButton(2, "stopwatch_texture");
        stopwatchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(
                        new GameScreen(MainMenuScreen.this.game, GameScreen.GAME_MODE_TIME));
            }
        });
        table.add(stopwatchButton).size(iconSize).space(height * 0.012f);

        // Palette button (buy colors)
        final SoftButton paletteButton = new SoftButton(3, "palette_texture");
        paletteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Don't dispose because then it needs to take us to the previous screen
                MainMenuScreen.this.game.transitionTo(new CustomizeScreen(
                        MainMenuScreen.this.game, MainMenuScreen.this.game.getScreen()), false);
            }
        });
        table.add(paletteButton).size(iconSize).space(height * 0.012f);

        // Star button (on GitHub)
        final SoftButton starButton = new SoftButton(1, "star_texture");
        starButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.net.openURI("https://github.com/mohamedelhajri12345-ops/PirateDash/stargazers");
            }
        });
        table.add(starButton).size(iconSize).space(height * 0.012f);

        table.row();

        // Second row: the Star Puzzle features
        // Lucky wheel (one free spin per day)
        final SoftButton wheelButton = new SoftButton(0, "wheel_texture");
        wheelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new WheelDialog(MainMenuScreen.this.game.skin));
            }
        });
        table.add(wheelButton).size(iconSize).space(height * 0.012f);

        // Daily missions
        final SoftButton missionsButton = new SoftButton(1, "missions_texture");
        missionsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new DailyDialog(MainMenuScreen.this.game.skin));
            }
        });
        table.add(missionsButton).size(iconSize).space(height * 0.012f);

        // Daily gift
        final SoftButton giftButton = new SoftButton(2, "gift_texture");
        giftButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new DailyGiftDialog(MainMenuScreen.this.game.skin));
            }
        });
        table.add(giftButton).size(iconSize).space(height * 0.012f);

        table.row();

        // Small version footer
        final Label.LabelStyle footerStyle = new Label.LabelStyle();
        footerStyle.font = game.skin.getFont("font_small");
        final Label footer = new Label("Star Puzzle v1.1  •  Mohamed Elhajri", footerStyle);
        footer.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                Klooni.theme.foreground.b, 0.5f);
        table.add(footer).colspan(3).padTop(height * 0.012f);
    }

    //endregion

    //region Private methods

    private void showCentered(final com.badlogic.gdx.scenes.scene2d.ui.Dialog dialog) {
        dialog.pack();
        dialog.show(stage);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) * 0.5f,
                (stage.getHeight() - dialog.getHeight()) * 0.5f);
    }

    //endregion

    //region Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // Auto-show today's gift once, the first time we get to the menu
        if (!giftChecked) {
            giftChecked = true;
            if (DailyGiftDialog.isGiftAvailable())
                showCentered(new DailyGiftDialog(game.skin));
        }
    }

    @Override
    public void render(float delta) {
        Klooni.theme.glClearBackground();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), minDelta));
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    //endregion

    //region Unused methods

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    //endregion
}
