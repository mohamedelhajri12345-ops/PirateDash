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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.game.Achievements;
import dev.lonami.klooni.game.LevelProgress;

// Player profile: real statistics backed by persistent counters, and the
// full achievement list with its unlock status. Everything shown here is
// computed from actual gameplay data.
public class ProfileScreen extends InputListener implements Screen {

    //region Members

    private final Klooni game;
    private final com.badlogic.gdx.scenes.scene2d.Stage stage;

    private static final float minDelta = 1 / 30f;

    //endregion

    //region Constructor

    public ProfileScreen(final Klooni game) {
        this.game = game;
        stage = new com.badlogic.gdx.scenes.scene2d.Stage();

        final float width = Gdx.graphics.getWidth();
        final float height = Gdx.graphics.getHeight();

        final Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Header: back button + title
        final Table header = new Table();
        root.add(header).top().fillX().padTop(height * 0.02f);

        final TextButton backButton = new TextButton("<", game.skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.transitionTo(new MainMenuScreen(game));
            }
        });
        header.add(backButton).size(width * 0.09f).padLeft(width * 0.02f);

        final Label title = new Label("PROFILE", game.skin);
        title.setColor(Klooni.theme.foreground);
        header.add(title).expandX().padLeft(width * 0.03f);

        root.row();

        // Scrollable content
        final Table content = new Table();
        final ScrollPane pane = new ScrollPane(content, game.skin);
        root.add(pane).expand().fill().pad(
                0, width * 0.03f, height * 0.01f, width * 0.03f);

        // Statistics section: all live values
        content.add(sectionLabel("STATISTICS")).colspan(2).padTop(10).padBottom(6);

        addStat(content, "Games played", Integer.toString(Achievements.gamesPlayed()));
        addStat(content, "Levels completed", Integer.toString(LevelProgress.levelsCompleted()));
        addStat(content, "Perfect levels (3 stars)", Integer.toString(LevelProgress.perfectLevels()));
        addStat(content, "Total stars", LevelProgress.totalStars() + " / 3000");
        addStat(content, "Lines cleared", Integer.toString(Achievements.linesCleared()));
        addStat(content, "Best combo", "x" + Math.max(1, Achievements.maxCombo()));
        addStat(content, "Special pieces used", Integer.toString(Achievements.specialsUsed()));
        addStat(content, "Coins", Integer.toString(Klooni.getMoney()));

        // Achievements section: full list with status
        content.row();
        content.add(sectionLabel("ACHIEVEMENTS")).colspan(2).padTop(18).padBottom(6);

        for (int id = 0; id < Achievements.count(); ++id) {
            final boolean unlocked = Achievements.isUnlocked(id);
            final Label name = new Label((unlocked ? "[DONE] " : "[   ]   ") + Achievements.getName(id), game.skin);
            name.setColor(unlocked ? Klooni.theme.foreground
                    : dimmedForeground(0.5f));
            content.row();
            content.add(name).left().expandX().pad(2, 10, 2, 4);

            final Label reward = new Label(unlocked ? "done" : "+" + Achievements.getReward(id), game.skin);
            reward.setColor(1f, 0.84f, 0.1f, unlocked ? 0.5f : 1f);
            content.add(reward).right().pad(2, 4, 2, 10);
        }
    }

    private com.badlogic.gdx.graphics.Color dimmedForeground(final float alpha) {
        final com.badlogic.gdx.graphics.Color color = Klooni.theme.foreground.cpy();
        color.a = alpha;
        return color;
    }

    private Label sectionLabel(final String text) {
        final Label label = new Label(text, game.skin);
        label.setAlignment(Align.center);
        label.setColor(1f, 0.84f, 0.1f, 1f);
        return label;
    }

    private void addStat(final Table content, final String name, final String value) {
        final Label nameLabel = new Label(name, game.skin);
        nameLabel.setColor(dimmedForeground(0.75f));
        content.row();
        content.add(nameLabel).left().expandX().pad(2, 10, 2, 4);

        final Label valueLabel = new Label(value, game.skin);
        valueLabel.setColor(Klooni.theme.foreground);
        valueLabel.setAlignment(Align.right);
        content.add(valueLabel).right().pad(2, 4, 2, 10);
    }

    //endregion

    //region Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Klooni.theme.glClearBackground();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), minDelta));
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.transitionTo(new MainMenuScreen(game));
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
