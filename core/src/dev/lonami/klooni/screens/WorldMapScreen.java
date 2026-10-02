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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;


import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;
import dev.lonami.klooni.game.LevelCatalog;
import dev.lonami.klooni.game.LevelProgress;

// The world map: 10 worlds, 100 level nodes each. Every node is a real,
// playable level. Locked worlds and levels show their requirement.
public class WorldMapScreen extends InputListener implements Screen {

    //region Members

    private final Klooni game;
    private final com.badlogic.gdx.scenes.scene2d.Stage stage;
    private int currentWorld;

    private Table worldTable;   // rebuilt when switching worlds
    private final Table root;

    private static final float minDelta = 1 / 30f;

    //endregion

    //region Constructor

    public WorldMapScreen(final Klooni game) {
        this.game = game;
        this.stage = new com.badlogic.gdx.scenes.scene2d.Stage();

        // Open the map on the world containing the next unfinished level
        final int next = LevelCatalog.firstUnfinishedLevel();
        currentWorld = (next - 1) / LevelCatalog.LEVELS_PER_WORLD + 1;

        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        buildRoot();
    }

    //endregion

    //region Building

    private void buildRoot() {
        root.clear();

        final float width = Gdx.graphics.getWidth();
        final float height = Gdx.graphics.getHeight();

        // Header: back button, world title and stars
        final Table header = new Table();
        root.add(header).top().fillX().padTop(height * 0.015f);
        header.setFillParent(false);

        final TextButton backButton = new TextButton("<", game.skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.transitionTo(new MainMenuScreen(game));
            }
        });
        header.add(backButton).size(width * 0.09f).padLeft(width * 0.02f);

        final Label title = new Label("WORLD " + currentWorld + "\n" + LevelCatalog.getWorldName(currentWorld), game.skin);
        title.setAlignment(Align.center);
        title.setColor(Klooni.theme.foreground);
        header.add(title).expandX();

        final Label starsLabel = new Label(
                LevelProgress.worldStars(currentWorld) + "/300 " + "*", game.skin);
        starsLabel.setColor(1f, 0.84f, 0.1f, 1f);
        header.add(starsLabel).padRight(width * 0.02f).width(width * 0.18f);

        root.row();

        // Level grid in a scroll pane: 5 columns x 20 rows
        worldTable = new Table();
        final ScrollPane pane = new ScrollPane(worldTable, game.skin);
        root.add(pane).expand().fill().pad(height * 0.01f, width * 0.03f, 0, width * 0.03f);

        buildWorld();

        root.row();

        // Footer: world navigation arrows
        final Table footer = new Table();
        root.add(footer).bottom().padBottom(height * 0.015f);

        final TextButton prevButton = new TextButton("PREV", game.skin);
        prevButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchWorld(currentWorld - 1);
            }
        });
        footer.add(prevButton).padRight(width * 0.05f);

        final TextButton nextButton = new TextButton("NEXT", game.skin);
        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchWorld(currentWorld + 1);
            }
        });
        footer.add(nextButton).padLeft(width * 0.05f);
    }

    // Builds the 100 level nodes of the current world.
    private void buildWorld() {
        worldTable.clear();
        final boolean worldUnlocked = LevelProgress.isWorldUnlocked(currentWorld);

        final Label.LabelStyle lockStyle = new Label.LabelStyle();
        lockStyle.font = game.skin.getFont("font");
        if (!worldUnlocked) {
            final int required = LevelCatalog.getWorldUnlockStars(currentWorld);
            final Label lockLabel = new Label("LOCKED\nNeed " + required + " stars"
                    + " (you have " + LevelProgress.totalStars() + ")", lockStyle);
            lockLabel.setAlignment(Align.center);
            lockStyle.fontColor = Klooni.theme.foreground;
            worldTable.add(lockLabel).colspan(5).pad(30);
            return;
        }

        final float width = Gdx.graphics.getWidth();
        final float height = Gdx.graphics.getHeight();
        final float nodeSize = Math.min(width * 0.15f, height * 0.09f);

        final Texture starTexture = SkinLoader.loadPng("star.png");
        final int first = (currentWorld - 1) * LevelCatalog.LEVELS_PER_WORLD;

        for (int i = 0; i < LevelCatalog.LEVELS_PER_WORLD; ++i) {
            final int levelId = first + i + 1;
            final int stars = LevelProgress.getStars(levelId);
            final boolean unlocked = LevelProgress.isLevelUnlocked(levelId);

            final Table node = new Table(game.skin);
            final com.badlogic.gdx.graphics.Color nodeBg = Klooni.theme.bandColor.cpy();
            nodeBg.a = 0.85f;
            node.setBackground(game.skin.newDrawable("white", nodeBg));

            final Label number = new Label(Integer.toString(i + 1), game.skin);
            number.setAlignment(Align.center);
            if (!unlocked)
                number.setColor(0.5f, 0.5f, 0.55f, 0.6f);
            else if (levelId % 100 == 0)
                number.setColor(1f, 0.55f, 0.15f, 1f); // boss levels pop
            else
                number.setColor(Klooni.theme.foreground);
            node.add(number).padTop(nodeSize * 0.08f);

            // Tiny star strip: 1 to 3 stars earned
            node.row();
            final Table starStrip = new Table();
            for (int s = 0; s < 3; ++s) {
                final Image star = new Image(starTexture);
                if (s < stars)
                    star.setColor(1f, 0.84f, 0.1f, 1f);
                else
                    star.setColor(0.4f, 0.4f, 0.45f, 0.5f);
                starStrip.add(star).size(nodeSize * 0.18f).pad(nodeSize * 0.015f);
            }
            node.add(starStrip).padBottom(nodeSize * 0.06f);

            node.addListener(new LevelClickListener(levelId, unlocked));

            worldTable.add(node).size(nodeSize).pad(nodeSize * 0.08f);

            if ((i + 1) % 5 == 0)
                worldTable.row();
        }
    }

    private void switchWorld(final int world) {
        if (world < 1 || world > LevelCatalog.TOTAL_WORLDS)
            return;
        currentWorld = world;
        buildRoot();
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

    //region Level click listener

    private class LevelClickListener extends ChangeListener {
        private final int levelId;
        private final boolean unlocked;

        LevelClickListener(final int levelId, final boolean unlocked) {
            this.levelId = levelId;
            this.unlocked = unlocked;
        }

        @Override
        public void changed(ChangeEvent event, Actor actor) {
            if (!unlocked)
                return;
            game.transitionTo(new GameScreen(game, LevelCatalog.getLevel(levelId)));
        }
    }

    //endregion
}
