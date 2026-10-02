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

// Persistent per-level progression: stars earned on each of the 1000 levels
// plus the daily-challenge completion. Uses libGDX Preferences like the rest
// of the game's save data (safe defaults, corruption-proof by design since
// every field has a default and invalid entries simply read as 0).
public final class LevelProgress {

    //region Members

    private static final String KEY_STARS = "levelStars";
    private static final String KEY_DAILY = "dailyDone";
    private static Preferences prefs;

    //endregion

    //region Private methods

    private static Preferences getPrefs() {
        if (prefs == null)
            prefs = Gdx.app.getPreferences("dev.lonami.klooni.levels");
        return prefs;
    }

    // Stars are packed in a compact string: one character per level,
    // '0'..'3'. A 1000-character value stays well within limits and an
    // index simply reads a char, making reads and writes O(1).
    private static char[] starsArray() {
        final String stored = getPrefs().getString(KEY_STARS, "");
        final char[] stars = new char[LevelCatalog.TOTAL_LEVELS];
        for (int i = 0; i < stars.length; ++i)
            stars[i] = '0';
        for (int i = 0; i < stored.length() && i < stars.length; ++i) {
            final char c = stored.charAt(i);
            if (c >= '0' && c <= '3')
                stars[i] = c;
        }
        return stars;
    }

    //endregion

    //region Public methods

    public static int getStars(final int levelId) {
        final char[] stars = starsArray();
        if (levelId < 1 || levelId > stars.length)
            return 0;
        return stars[levelId - 1] - '0';
    }

    // Saves the stars only if they improved. Returns true when progress was made.
    public static boolean setStars(final int levelId, final int stars) {
        if (levelId < 1 || levelId > LevelCatalog.TOTAL_LEVELS || stars < 0 || stars > 3)
            return false;

        final char[] arr = starsArray();
        if (stars <= arr[levelId - 1] - '0')
            return false;

        arr[levelId - 1] = (char) ('0' + stars);
        getPrefs().putString(KEY_STARS, new String(arr)).flush();
        return true;
    }

    // Total stars earned across all levels.
    public static int totalStars() {
        final char[] stars = starsArray();
        int total = 0;
        for (char c : stars)
            total += c - '0';
        return total;
    }

    // Stars earned within one world (0..300).
    public static int worldStars(final int world) {
        final char[] stars = starsArray();
        int total = 0;
        final int first = (world - 1) * LevelCatalog.LEVELS_PER_WORLD;
        for (int i = 0; i < LevelCatalog.LEVELS_PER_WORLD; ++i)
            total += stars[first + i] - '0';
        return total;
    }

    public static boolean isWorldUnlocked(final int world) {
        return totalStars() >= LevelCatalog.getWorldUnlockStars(world);
    }

    // A level is playable when its world is unlocked and it is either the
    // first level, or the previous level has been completed with >= 1 star.
    public static boolean isLevelUnlocked(final int levelId) {
        if (levelId < 1 || levelId > LevelCatalog.TOTAL_LEVELS)
            return false;
        if (levelId == 1)
            return true;
        return isWorldUnlocked((levelId - 1) / LevelCatalog.LEVELS_PER_WORLD + 1)
                && getStars(levelId - 1) >= 1;
    }

    // Completed level count for stats and achievements.
    public static int levelsCompleted() {
        final char[] stars = starsArray();
        int count = 0;
        for (char c : stars)
            if (c > '0')
                ++count;
        return count;
    }

    // Three-star level count for stats and achievements.
    public static int perfectLevels() {
        final char[] stars = starsArray();
        int count = 0;
        for (char c : stars)
            if (c == '3')
                ++count;
        return count;
    }

    //endregion

    //region Daily challenge

    public static boolean isDailyDone() {
        return getPrefs().getString(KEY_DAILY, "").equals(LevelCatalog.dailyKey());
    }

    public static void markDailyDone() {
        getPrefs().putString(KEY_DAILY, LevelCatalog.dailyKey()).flush();
    }

    //endregion
}
