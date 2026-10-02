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

import com.badlogic.gdx.Gdx;

import dev.lonami.klooni.Klooni;
import com.badlogic.gdx.Preferences;

import java.text.SimpleDateFormat;
import java.util.Date;

// Star Puzzle daily missions: 3 missions that reset every day.
// Progress is tracked on the game preferences, rewards are paid in coins.
public final class Missions {

    //region Members

    private static final String[] KEYS = {"missionLines", "missionPieces", "missionScore"};
    private static final int[] TARGETS = {15, 25, 1500};
    private static final int[] REWARDS = {40, 30, 50};

    private static Preferences prefs;

    //endregion

    //region Private methods

    private static Preferences getPrefs() {
        if (prefs == null)
            prefs = Gdx.app.getPreferences("dev.lonami.klooni.missions");
        return prefs;
    }

    private static void ensureFreshDay() {
        final String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!getPrefs().getString("day", "").equals(today)) {
            for (String key : KEYS)
                getPrefs().putInteger(key, 0);
            getPrefs().putInteger("claimed", 0);
            getPrefs().putString("day", today);
            getPrefs().flush();
        }
    }

    private static void addProgress(final int index, final int amount) {
        ensureFreshDay();
        final int now = Math.min(getProgress(index) + amount, TARGETS[index]);
        getPrefs().putInteger(KEYS[index], now).flush();
    }

    //endregion

    //region Public methods

    public static void onLinesCleared(final int cleared) {
        addProgress(0, cleared);
    }

    public static void onPiecesPlaced(final int area) {
        addProgress(1, area);
    }

    // Called with the total score of the current game; the mission tracks
    // the cumulative score gained today, so we only add the newly gained part.
    public static void onScore(final int totalScore, final int previousScore) {
        addProgress(2, Math.max(0, totalScore - previousScore));
    }

    public static int getProgress(final int index) {
        ensureFreshDay();
        return getPrefs().getInteger(KEYS[index], 0);
    }

    public static int getTarget(final int index) {
        return TARGETS[index];
    }

    public static int getReward(final int index) {
        return REWARDS[index];
    }

    public static boolean isComplete(final int index) {
        return getProgress(index) >= TARGETS[index];
    }

    public static boolean isClaimed(final int index) {
        ensureFreshDay();
        return (getPrefs().getInteger("claimed", 0) & (1 << index)) != 0;
    }

    // Claims the reward for the given mission and pays it in coins.
    // Returns true if the reward was actually paid.
    public static boolean claim(final int index) {
        if (isClaimed(index) || !isComplete(index))
            return false;

        final int claimed = getPrefs().getInteger("claimed", 0) | (1 << index);
        getPrefs().putInteger("claimed", claimed).flush();
        Klooni.addMoney(REWARDS[index]);
        return true;
    }

    public static boolean allClaimed() {
        for (int i = 0; i < 3; ++i)
            if (!isClaimed(i))
                return false;
        return true;
    }

    //endregion
}
