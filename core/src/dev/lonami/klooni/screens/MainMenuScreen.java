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
import dev.lonami.klooni.game.LevelCatalog;
import dev.lonami.klooni.game.LevelProgress;

// Main menu screen for the World Puzzle Adventure:
// the journey (world map) is the primary action, with the classic
// endless modes, daily challenge, rewards, customization and
// the player profile one tap away.
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
                .size(width * 0.68f, height * 0.14f)
                .padTop(height * 0.02f).space(height * 0.015f);

        table.row();

        // Play button, the star of the menu: enter the world map journey
        final SoftButton playButton = new SoftButton(
                0, GameScreen.hasSavedData() ? "play_saved_texture" : "play_texture");
        playButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(new WorldMapScreen(MainMenuScreen.this.game));
            }
        });
        table.add(playButton).colspan(3).size(width * 0.62f, height * 0.13f).space(height * 0.02f);

        table.row();

        final float iconSize = Math.min(width * 0.22f, height * 0.1f);

        // First row: classic game modes + daily challenge
        table.add(iconButton(iconSize, "play_saved_texture", "ENDLESS", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(
                        new GameScreen(MainMenuScreen.this.game, GameScreen.GAME_MODE_SCORE));
            }
        })).space(width * 0.02f);

        table.add(iconButton(iconSize, "stopwatch_texture", "TIME", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(
                        new GameScreen(MainMenuScreen.this.game, GameScreen.GAME_MODE_TIME));
            }
        })).space(width * 0.02f);

        table.add(iconButton(iconSize, "star_texture",
                LevelProgress.isDailyDone() ? "DAILY OK" : "DAILY", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(new GameScreen(
                        MainMenuScreen.this.game, LevelCatalog.dailyLevel(), true));
            }
        })).space(width * 0.02f);

        table.row().spaceTop(height * 0.008f);

        // Second row: rewards
        table.add(iconButton(iconSize, "wheel_texture", "WHEEL", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new WheelDialog(MainMenuScreen.this.game.skin));
            }
        })).space(width * 0.02f);

        table.add(iconButton(iconSize, "missions_texture", "MISSIONS", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new DailyDialog(MainMenuScreen.this.game.skin));
            }
        })).space(width * 0.02f);

        table.add(iconButton(iconSize, "gift_texture", "GIFT", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCentered(new DailyGiftDialog(MainMenuScreen.this.game.skin));
            }
        })).space(width * 0.02f);

        table.row().spaceTop(height * 0.008f);

        // Third row: customization, profile, sound
        table.add(iconButton(iconSize, "palette_texture", "THEMES", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Don't dispose because then it needs to take us to the previous screen
                MainMenuScreen.this.game.transitionTo(new CustomizeScreen(
                        MainMenuScreen.this.game, MainMenuScreen.this.game.getScreen()), false);
            }
        })).space(width * 0.02f);

        table.add(iconButton(iconSize, "stats_texture", "PROFILE", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainMenuScreen.this.game.transitionTo(new ProfileScreen(MainMenuScreen.this.game));
            }
        })).space(width * 0.02f);

        final SoftButton soundButton = new SoftButton(3,
                Klooni.soundsEnabled() ? "sound_on_texture" : "sound_off_texture");
        soundButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final boolean enabled = Klooni.toggleSound();
                soundButton.updateImage(enabled ? "sound_on_texture" : "sound_off_texture");
                if (enabled)
                    Klooni.startMusic();
                else
                    Klooni.stopMusic();
            }
        });
        table.add(iconCell(iconSize, soundButton, "SOUND")).space(width * 0.02f);

        table.row();

        // Small version footer
        final Label.LabelStyle footerStyle = new Label.LabelStyle();
        footerStyle.font = game.skin.getFont("font_small");
        final Label footer = new Label("Star Puzzle v1.3  •  Mohamed Elhajri", footerStyle);
        footer.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                Klooni.theme.foreground.b, 0.5f);
        table.add(footer).colspan(3).padTop(height * 0.012f);
    }

    //endregion

    //region Private helpers

    // An icon button with a small caption below it
    private Table iconButton(final float iconSize, final String textureName,
                             final String caption, final ChangeListener listener) {
        final SoftButton button = new SoftButton(1, textureName);
        button.addListener(listener);
        return iconCell(iconSize, button, caption);
    }

    private Table iconCell(final float iconSize, final SoftButton button, final String caption) {
        final Table cell = new Table();
        cell.add(button).size(iconSize).spaceTop(iconSize * 0.08f);
        cell.row();
        final Label.LabelStyle captionStyle = new Label.LabelStyle();
        captionStyle.font = game.skin.getFont("font_small");
        final Label label = new Label(caption, captionStyle);
        label.setColor(Klooni.theme.foreground.r, Klooni.theme.foreground.g,
                Klooni.theme.foreground.b, 0.7f);
        cell.add(label).padTop(iconSize * 0.05f);
        return cell;
    }

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
