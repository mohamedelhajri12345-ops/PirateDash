/*
    Star Puzzle — World Puzzle Adventure
    A data-driven level system built on top of 1010! Klooni (GPL-3.0, Lonami Exo).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
*/
package dev.lonami.klooni.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import dev.lonami.klooni.Klooni;

// Real statistics and achievements backed by persistent counters.
// Every counter is updated from a real gameplay event and every
// achievement grants a coin reward exactly once.
public final class Achievements {

    //region Members

    private static Preferences prefs;

    // Achievement ids are stable; the bitmask records which were unlocked.
    public static final int FIRST_WIN = 0;
    public static final int LINES_100 = 1;
    public static final int LINES_1000 = 2;
    public static final int COMBO_MASTER = 3;
    public static final int POWER_USER = 4;
    public static final int WORLD_EXPLORER = 5;
    public static final int PERFECT_PLAYER = 6;
    public static final int LEVELS_100 = 7;
    public static final int LEVELS_500 = 8;
    public static final int LEVELS_1000 = 9;
    public static final int STAR_COLLECTOR = 10;

    private static final String[] NAMES = {
            "First Win", "100 Lines", "1000 Lines", "Combo Master",
            "Power User", "World Explorer", "Perfect Player",
            "100 Levels", "500 Levels", "1000 Levels", "300 Stars"
    };
    private static final int[] REWARDS = {
            20, 30, 100, 60, 40, 80, 150, 80, 200, 500, 120
    };

    //endregion

    //region Private methods

    private static Preferences getPrefs() {
        if (prefs == null)
            prefs = Gdx.app.getPreferences("dev.lonami.klooni.stats");
        return prefs;
    }

    private static void bump(final String key, final int amount) {
        getPrefs().putInteger(key, getStats(key) + amount).flush();
    }

    private static int getStats(final String key) {
        return getPrefs().getInteger(key, 0);
    }

    // Unlocks the achievement if not already unlocked, paying its reward.
    private static void unlock(final int id) {
        final int unlocked = getStats("unlocked");
        if ((unlocked & (1 << id)) != 0)
            return;

        getPrefs().putInteger("unlocked", unlocked | (1 << id)).flush();
        Klooni.addMoney(REWARDS[id]);
        Klooni.playStarSound();
    }

    //endregion

    //region Event triggers (called from real gameplay events)

    public static void onGameStarted() {
        bump("gamesPlayed", 1);
    }

    public static void onLinesCleared(final int cleared) {
        final int total = getStats("linesCleared") + cleared;
        getPrefs().putInteger("linesCleared", total).flush();
        if (total >= 100) unlock(LINES_100);
        if (total >= 1000) unlock(LINES_1000);
    }

    public static void onPiecesPlaced(final int area) {
        bump("piecesPlaced", area);
    }

    public static void onCombo(final int combo) {
        if (combo > getStats("maxCombo"))
            getPrefs().putInteger("maxCombo", combo).flush();
        if (combo >= 6)
            unlock(COMBO_MASTER);
    }

    public static void onSpecialUsed() {
        bump("specialsUsed", 1);
        if (getStats("specialsUsed") >= 25)
            unlock(POWER_USER);
    }

    public static void onLevelCompleted() {
        bump("levelsWon", 1);
        unlock(FIRST_WIN);

        final int completed = LevelProgress.levelsCompleted();
        if (completed >= 100) unlock(LEVELS_100);
        if (completed >= 500) unlock(LEVELS_500);
        if (completed >= 1000) unlock(LEVELS_1000);
        if (LevelProgress.perfectLevels() >= 50)
            unlock(PERFECT_PLAYER);
        if (LevelProgress.totalStars() >= 300)
            unlock(STAR_COLLECTOR);

        for (int world = 2; world <= LevelCatalog.TOTAL_WORLDS; ++world) {
            if (LevelProgress.isWorldUnlocked(world)) {
                unlock(WORLD_EXPLORER);
                break;
            }
        }
    }

    //endregion

    //region Public getters (for the profile screen)

    public static String getName(final int id) {
        return NAMES[id];
    }

    public static int getReward(final int id) {
        return REWARDS[id];
    }

    public static int count() {
        return NAMES.length;
    }

    public static boolean isUnlocked(final int id) {
        return (getStats("unlocked") & (1 << id)) != 0;
    }

    public static int gamesPlayed() {
        return getStats("gamesPlayed");
    }

    public static int linesCleared() {
        return getStats("linesCleared");
    }

    public static int maxCombo() {
        return getStats("maxCombo");
    }

    public static int specialsUsed() {
        return getStats("specialsUsed");
    }

    //endregion
}
