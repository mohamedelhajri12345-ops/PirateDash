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
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;

import java.text.SimpleDateFormat;
import java.util.Date;

// Lucky wheel dialog: one free spin per day with coin rewards.
// The wheel has 8 segments; one is chosen at random and the wheel is
// animated so that it lands exactly on it.
public class WheelDialog extends Dialog {

    //region Members

    private static final int[] REWARDS = {50, 20, 80, 10, 100, 15, 60, 25};
    private static final float SEGMENT_DEGREES = 360f / REWARDS.length;

    private static final Preferences prefs = Gdx.app.getPreferences("dev.lonami.klooni.wheel");

    private final Image wheelImage;
    private final TextButton spinButton;
    private final Label resultLabel;

    private boolean spinning;

    //endregion

    //region Constructor

    public WheelDialog(final Skin skin) {
        super("LUCKY WHEEL", skin, "dialog");

        final Texture wheelTexture = SkinLoader.loadPng("wheel");
        wheelImage = new Image(wheelTexture);
        wheelImage.setOrigin(Align.center);
        getContentTable().add(wheelImage).size(Gdx.graphics.getHeight() * 0.45f);

        resultLabel = new Label("", skin);
        resultLabel.setAlignment(Align.center);
        getContentTable().row();
        getContentTable().add(resultLabel).padTop(16);

        spinButton = new TextButton("SPIN!", skin);
        spinButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                spin();
            }
        });
        getButtonTable().add(spinButton);

        final TextButton closeButton = new TextButton("CLOSE", skin);
        closeButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getButtonTable().add(closeButton).padLeft(16);

        if (spunToday()) {
            resultLabel.setText("Come back tomorrow!");
            spinButton.setDisabled(true);
        }
    }

    //endregion

    //region Private methods

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    private static boolean spunToday() {
        return prefs.getString("wheelDay", "").equals(today());
    }

    private void spin() {
        if (spinning || spunToday())
            return;

        spinning = true;
        prefs.putString("wheelDay", today()).flush();
        spinButton.setDisabled(true);

        // Choose the winning segment at random and rotate the wheel so
        // that the pointer (at the top) lands inside that segment.
        final int segment = MathUtils.random(REWARDS.length - 1);
        final float jitter = SEGMENT_DEGREES * 0.4f * MathUtils.random() - SEGMENT_DEGREES * 0.2f;
        final float target = 360f * 5 - segment * SEGMENT_DEGREES + jitter;

        wheelImage.clearActions();
        wheelImage.addAction(Actions.sequence(
                Actions.rotateBy(target, 3.5f, Interpolation.pow3Out),
                new Action() {
                    @Override
                    public boolean act(float delta) {
                        spinning = false;
                        final int coins = REWARDS[segment];
                        Klooni.addMoney(coins);
                        Klooni.playCoinSound();
                        resultLabel.setText("+" + coins + " COINS!");
                        return true;
                    }
                }
        ));
    }

    //endregion
}
