/*
    Star Puzzle — World Puzzle Adventure
    A data-driven level system built on top of 1010! Klooni (GPL-3.0, Lonami Exo).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
*/
package dev.lonami.klooni.game;

// A single level definition. Levels are generated deterministically by
// LevelCatalog, so the whole 1000-level database is reproducible from
// code without shipping data files.
public class LevelDefinition {

    //region Objective types

    public static final int TYPE_SCORE = 0;  // reach targetScore before moves/time run out
    public static final int TYPE_LINES = 1;  // clear targetLines lines in maxMoves
    public static final int TYPE_TIME = 2;   // reach targetScore before timeLimit seconds
    public static final int TYPE_COMBO = 3;  // achieve a single combo of targetCombo in maxMoves

    //endregion

    //region Members

    public final int id;             // 1..1000
    public final int world;          // 1..10
    public final int indexInWorld;   // 1..100
    public final int objectiveType;

    public final int targetScore;
    public final int targetLines;
    public final int targetCombo;
    public final int maxMoves;       // 0 = unlimited (time levels)
    public final int timeLimit;      // seconds, 0 = none

    public final int prefillDensity; // percent of board cells pre-filled
    public final long seed;          // deterministic board prefill seed
    public final int specialChance;  // percent chance of special pieces
    public final int rewardCoins;    // coins granted on first clear / star improvement

    //endregion

    //region Constructor

    LevelDefinition(final int id, final int world, final int indexInWorld, final int objectiveType,
                    final int targetScore, final int targetLines, final int targetCombo,
                    final int maxMoves, final int timeLimit, final int prefillDensity,
                    final long seed, final int specialChance, final int rewardCoins) {
        this.id = id;
        this.world = world;
        this.indexInWorld = indexInWorld;
        this.objectiveType = objectiveType;
        this.targetScore = targetScore;
        this.targetLines = targetLines;
        this.targetCombo = targetCombo;
        this.maxMoves = maxMoves;
        this.timeLimit = timeLimit;
        this.prefillDensity = prefillDensity;
        this.seed = seed;
        this.specialChance = specialChance;
        this.rewardCoins = rewardCoins;
    }

    //endregion

    //region Public methods

    // Short human-readable objective, shown in the level HUD
    public String objectiveText() {
        switch (objectiveType) {
            case TYPE_SCORE:
                return "SCORE " + targetScore;
            case TYPE_LINES:
                return "CLEAR " + targetLines + " LINES";
            case TYPE_TIME:
                return "SCORE " + targetScore + " IN " + timeLimit + "s";
            case TYPE_COMBO:
                return "COMBO x" + targetCombo;
            default:
                return "SURVIVE";
        }
    }

    // The progress value (0..target) and its target for the objective HUD.
    // progressA/progressB are filled with the current objective progress
    // and its target so the HUD can render "a / b".
    public int target() {
        switch (objectiveType) {
            case TYPE_LINES: return targetLines;
            case TYPE_COMBO: return targetCombo;
            default: return targetScore; // score based
        }
    }

    // Stars are awarded by performance margin:
    // 1 star = complete the objective
    // 2 stars = finish with >= 20% of the allowance (moves/time) to spare
    // 3 stars = finish with >= 40% of the allowance to spare
    public int starsFor(final float allowanceLeftFraction) {
        if (allowanceLeftFraction >= 0.4f) return 3;
        if (allowanceLeftFraction >= 0.2f) return 2;
        return 1;
    }

    public boolean isBoss() {
        return indexInWorld == 100;
    }

    //endregion
}
