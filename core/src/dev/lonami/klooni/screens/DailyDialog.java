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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.game.Missions;

// Daily missions dialog: shows the progress of today's three missions
// and lets the player claim the coin reward of each completed one.
public class DailyDialog extends Dialog {

    //region Constructor

    public DailyDialog(final Skin skin) {
        super("DAILY MISSIONS", skin, "dialog");

        final String[] DESCRIPTIONS = {
                "Clear lines", "Place piece cells", "Score points"
        };

        for (int i = 0; i < 3; ++i) {
            final int index = i;

            final Table row = new Table();
            final Label label = new Label(DESCRIPTIONS[i], skin);
            row.add(label).left().expandX();

            final Label progress = new Label(
                    Missions.getProgress(index) + " / " + Missions.getTarget(index)
                            + "  (+" + Missions.getReward(index) + " coins)", skin);
            row.add(progress).right().padRight(16);

            getContentTable().add(row).pad(6).row();

            final TextButton button = new TextButton(buttonText(index), skin);
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (Missions.claim(index)) {
                        Klooni.playCoinSound();
                        button.setText("CLAIMED");
                        button.setDisabled(true);
                    }
                }
            });
            button.setDisabled(!Missions.isComplete(index) || Missions.isClaimed(index));
            getContentTable().add(button).row();
        }

        final TextButton closeButton = new TextButton("CLOSE", skin);
        closeButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getButtonTable().add(closeButton);
    }

    //endregion

    //region Private methods

    private static String buttonText(final int index) {
        if (Missions.isClaimed(index))
            return "CLAIMED";
        return Missions.isComplete(index) ? "CLAIM!" : "IN PROGRESS";
    }

    //endregion
}
