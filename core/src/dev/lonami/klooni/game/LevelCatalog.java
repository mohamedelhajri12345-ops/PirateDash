/*
    Star Puzzle — World Puzzle Adventure
    A data-driven level system built on top of 1010! Klooni (GPL-3.0, Lonami Exo).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
*/
package dev.lonami.klooni.game;

import java.util.Calendar;
import java.util.Random;

// Deterministic catalog of 1000 levels across 10 worlds.
//
// Levels are not hand-authored one by one; instead every level is derived
// from its id with a fixed-seed random generator, following templates
// (score / lines / time / combo) and a world-based difficulty curve.
// The same id always produces the exact same level on every device.
//
// This class has NO libGDX dependency so it can be unit tested headlessly.
public final class LevelCatalog {

    //region Members

    public static final int LEVELS_PER_WORLD = 100;
    public static final int TOTAL_WORLDS = 10;
    public static final int TOTAL_LEVELS = LEVELS_PER_WORLD * TOTAL_WORLDS; // 1000

    // Worlds follow an adventure arc; each introduces a mechanic:
    // 1 basics, 2 tighter moves, 3 pre-filled obstacles, 4 time attack,
    // 5 combo objectives, 6 dense obstacles, 7 special pieces, 8 hard time,
    // 9 big combos, 10 everything combined (boss levels at 100th).
    private static final String[] WORLD_NAMES = {
            "Green Valley",      // 1
            "Golden Desert",     // 2
            "Coral Ocean",       // 3
            "Frozen Peaks",      // 4
            "Ember Volcano",     // 5
            "Deep Forest",      // 6
            "Neon City",         // 7
            "Candy Wonderland",  // 8
            "Cosmic Space",      // 9
            "Mystery Realm"      // 10
    };

    // Star requirements to unlock each world (cumulative stars).
    // 3 stars on every level of a world = 300 stars, so these gates are
    // always reachable long before the previous world is exhausted.
    private static final int[] WORLD_UNLOCK_STARS = {
            0,    // world 1
            30,   // world 2
            70,   // world 3
            120,  // world 4
            170,  // world 5
            220,  // world 6
            270,  // world 7
            320,  // world 8
            370,  // world 9
            420   // world 10
    };

    private static final long SEED_SALT = 53701221L; // "STARPUZZLE" flavor

    //endregion

    //region Public methods

    public static String getWorldName(final int world) {
        return WORLD_NAMES[world - 1];
    }

    public static int getWorldUnlockStars(final int world) {
        return WORLD_UNLOCK_STARS[world - 1];
    }

    // The level the player should play next: the first level without stars.
    public static int firstUnfinishedLevel() {
        for (int id = 1; id <= TOTAL_LEVELS; ++id)
            if (LevelProgress.getStars(id) == 0)
                return id;
        return TOTAL_LEVELS;
    }

    // Deterministic daily challenge: one level per day, always in a
    // mid difficulty band and playable regardless of normal progression.
    public static LevelDefinition dailyLevel() {
        final Calendar now = Calendar.getInstance();
        final int day = now.get(Calendar.YEAR) * 10000
                + (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH);
        final Random random = new Random(day * SEED_SALT);
        final int id = 101 + random.nextInt(400); // levels 101..500 band
        return getLevel(id);
    }

    public static String dailyKey() {
        final Calendar now = Calendar.getInstance();
        return String.format("%04d%02d%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
    }

    // Build a level from its id. Pure and deterministic.
    public static LevelDefinition getLevel(final int id) {
        if (id < 1 || id > TOTAL_LEVELS)
            throw new IllegalArgumentException("level id out of range: " + id);

        final int world = (id - 1) / LEVELS_PER_WORLD + 1;
        final int indexInWorld = (id - 1) % LEVELS_PER_WORLD + 1;

        final Random random = new Random(id * 2654435761L + SEED_SALT);

        // Difficulty tiers per the content plan:
        // 1-20 tutorial, 21-50 core, 51-100 mastery, 101+ advanced...
        // t goes 0..9 across the whole journey (per 100 levels).
        final float t = (float) (indexInWorld - 1) / (float) (LEVELS_PER_WORLD - 1); // 0..1 in world
        final float w = world - 1; // 0..9

        // Objective template rotates deterministically. World decides which
        // mechanic it introduces; every 5th level in a world is a "challenge"
        // variant with a different objective than its neighbors.
        final int objectiveType = pickObjective(random, world, indexInWorld);

        // Board difficulty: obstacle pre-fills appear from world 3 onward.
        final int prefillDensity;
        if (world < 3)
            prefillDensity = 0;
        else
            prefillDensity = Math.min(4 + world + (int) (t * 6), 30); // max 30%

        // Special piece chance: worlds 7+ feature more specials.
        final int specialChance = world >= 7 ? 10 + (world - 7) * 2 : 5;

        // Reward grows slowly with progression.
        final int rewardCoins = 10 + id / 20 + (indexInWorld == 100 ? 40 : 0);

        switch (objectiveType) {
            case LevelDefinition.TYPE_LINES: {
                // Clear N lines. Targets 4..40 with the world curve.
                final int targetLines = 3 + (int) (t * 6) + world + random.nextInt(3);
                final int maxMoves = (int) (targetLines * 1.7f) + 8;
                return new LevelDefinition(id, world, indexInWorld, objectiveType,
                        0, targetLines, 0, maxMoves, 0,
                        prefillDensity, random.nextLong(), specialChance, rewardCoins);
            }
            case LevelDefinition.TYPE_TIME: {
                // Reach a score before the timer runs out. Introduced in
                // world 4; the band tightens in world 8.
                final int timeLimit = world >= 8
                        ? 60 + random.nextInt(30)
                        : 90 + random.nextInt(60);
                // A decent player scores roughly 12-20 pts/second.
                final int targetScore = Math.round(timeLimit
                        * (10f + w * 1.2f + t * 4f));
                return new LevelDefinition(id, world, indexInWorld, objectiveType,
                        targetScore, 0, 0, 0, timeLimit,
                        prefillDensity, random.nextLong(), specialChance, rewardCoins);
            }
            case LevelDefinition.TYPE_COMBO: {
                // Land a single combo of x3..x7. Introduced in world 5.
                final int targetCombo = Math.min(3 + (int) (w * 0.4f) + (t > 0.6f ? 1 : 0)
                        + (random.nextBoolean() ? 1 : 0), 7);
                final int maxMoves = 12 + random.nextInt(6) + world;
                return new LevelDefinition(id, world, indexInWorld, objectiveType,
                        0, 0, targetCombo, maxMoves, 0,
                        prefillDensity, random.nextLong(), specialChance, rewardCoins);
            }
            case LevelDefinition.TYPE_SCORE:
            default: {
                // Reach a score within a move budget. The core objective.
                final int maxMoves = 14 + world + (int) (t * 8) + random.nextInt(5);
                // Average gain per move grows with player skill depth:
                // roughly 20-45 points per move as worlds progress.
                final int targetScore = Math.round(maxMoves
                        * (20f + w * 2.2f + t * 6f) * 0.92f);
                return new LevelDefinition(id, world, indexInWorld, objectiveType,
                        targetScore, 0, 0, maxMoves, 0,
                        prefillDensity, random.nextLong(), specialChance, rewardCoins);
            }
        }
    }

    //endregion

    //region Private methods

    private static int pickObjective(final Random random, final int world, final int indexInWorld) {
        // Every 25th level and the world boss (100th) mix it up.
        if (indexInWorld == 100)
            return world >= 8 ? LevelDefinition.TYPE_TIME : LevelDefinition.TYPE_LINES;

        switch (world) {
            case 1: // basics: score + gentle lines
                return (indexInWorld % 5 == 0) ? LevelDefinition.TYPE_LINES
                        : LevelDefinition.TYPE_SCORE;
            case 2: // lines introduced more, tighter moves
                return (indexInWorld % 4 <= 1) ? LevelDefinition.TYPE_LINES
                        : LevelDefinition.TYPE_SCORE;
            case 3: // obstacles + lines/score mix
                return (indexInWorld % 3 == 0) ? LevelDefinition.TYPE_LINES
                        : LevelDefinition.TYPE_SCORE;
            case 4: // time attack introduced
                if (indexInWorld % 3 == 0) return LevelDefinition.TYPE_TIME;
                return (indexInWorld % 3 == 1) ? LevelDefinition.TYPE_SCORE
                        : LevelDefinition.TYPE_LINES;
            case 5: // combos introduced
                if (indexInWorld % 4 == 0) return LevelDefinition.TYPE_COMBO;
                if (indexInWorld % 2 == 0) return LevelDefinition.TYPE_LINES;
                return LevelDefinition.TYPE_SCORE;
            case 6: // dense obstacles: mostly lines
                return (indexInWorld % 3 == 0) ? LevelDefinition.TYPE_SCORE
                        : LevelDefinition.TYPE_LINES;
            case 7: // special pieces shine; combo/score
                if (indexInWorld % 4 == 1) return LevelDefinition.TYPE_COMBO;
                return (indexInWorld % 2 == 0) ? LevelDefinition.TYPE_LINES
                        : LevelDefinition.TYPE_SCORE;
            case 8: // hard time
                if (indexInWorld % 3 != 2) return LevelDefinition.TYPE_TIME;
                return LevelDefinition.TYPE_COMBO;
            case 9: // big combos
                if (indexInWorld % 2 == 0) return LevelDefinition.TYPE_COMBO;
                return (indexInWorld % 3 == 1) ? LevelDefinition.TYPE_TIME
                        : LevelDefinition.TYPE_LINES;
            case 10: // everything combined
            default: {
                final int roll = random.nextInt(4);
                return roll;
            }
        }
    }

    //endregion
}
