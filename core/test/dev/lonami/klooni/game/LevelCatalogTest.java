/*
    Star Puzzle — World Puzzle Adventure level system tests.
    Validates that all 1000 levels generate correctly and are playable
    by construction. Runs headlessly (no libGDX required).
*/
package dev.lonami.klooni.game;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class LevelCatalogTest {

    private static final int EXPECTED_LEVELS = 1000;
    private static final int EXPECTED_WORLDS = 10;

    @Test
    public void testAllLevelsGenerate() {
        for (int id = 1; id <= EXPECTED_LEVELS; ++id) {
            final LevelDefinition level = LevelCatalog.getLevel(id);
            assertEquals(id, level.id);
            final int expectedWorld = (id - 1) / 100 + 1;
            assertEquals(expectedWorld, level.world);
            assertTrue("world must be 1..10", level.world >= 1 && level.world <= EXPECTED_WORLDS);
            assertTrue("reward must be positive", level.rewardCoins > 0);
            assertTrue("special chance in 5..20", level.specialChance >= 5 && level.specialChance <= 20);
            assertTrue("prefill density capped at 30",
                    level.prefillDensity >= 0 && level.prefillDensity <= 30);
        }
    }

    @Test
    public void testLevelsAreDeterministic() {
        for (int id : new int[]{1, 42, 100, 101, 500, 555, 999, 1000}) {
            final LevelDefinition a = LevelCatalog.getLevel(id);
            final LevelDefinition b = LevelCatalog.getLevel(id);
            assertEquals(a.seed, b.seed);
            assertEquals(a.targetScore, b.targetScore);
            assertEquals(a.targetLines, b.targetLines);
            assertEquals(a.targetCombo, b.targetCombo);
            assertEquals(a.maxMoves, b.maxMoves);
            assertEquals(a.timeLimit, b.timeLimit);
            assertEquals(a.objectiveType, b.objectiveType);
        }
    }

    @Test
    public void testObjectivesAreReachable() {
        // Move-based levels must have a move budget generous enough that a
        // skilled player can reach the target: >= 12 moves, and score targets
        // must be below a sane per-move gain for the tier (60 pts/move max).
        for (int id = 1; id <= EXPECTED_LEVELS; ++id) {
            final LevelDefinition level = LevelCatalog.getLevel(id);

            switch (level.objectiveType) {
                case LevelDefinition.TYPE_SCORE:
                    assertTrue("moves too few: " + id, level.maxMoves >= 12);
                    assertTrue("score target unreachable: " + id,
                            level.targetScore <= level.maxMoves * 60);
                    assertTrue("score target too trivial: " + id,
                            level.targetScore >= level.maxMoves * 12);
                    break;
                case LevelDefinition.TYPE_LINES:
                    assertTrue("lines target too high: " + id, level.targetLines <= 45);
                    assertTrue("lines move budget too tight: " + id,
                            level.maxMoves >= level.targetLines);
                    break;
                case LevelDefinition.TYPE_TIME:
                    assertTrue("time limit sane: " + id,
                            level.timeLimit >= 60 && level.timeLimit <= 180);
                    assertTrue("time score target unreachable: " + id,
                            level.targetScore <= level.timeLimit * 30);
                    assertTrue("time score target too trivial: " + id,
                            level.targetScore >= level.timeLimit * 10);
                    break;
                case LevelDefinition.TYPE_COMBO:
                    assertTrue("combo target achievable: " + id,
                            level.targetCombo >= 2 && level.targetCombo <= 7);
                    assertTrue("combo move budget too tight: " + id, level.maxMoves >= 12);
                    break;
                default:
                    fail("unknown objective type on level " + id);
            }
        }
    }

    @Test
    public void testWorldArc() {
        // Difficulty must grow: the average score target per score-type
        // level must clearly rise from world 1 to world 10.
        int w1sum = 0, w1count = 0, w10sum = 0, w10count = 0;
        for (int i = 1; i <= 100; ++i) {
            final LevelDefinition l1 = LevelCatalog.getLevel(i);
            final LevelDefinition l10 = LevelCatalog.getLevel(900 + i);
            if (l1.objectiveType == LevelDefinition.TYPE_SCORE) {
                w1sum += l1.targetScore;
                w1count++;
            }
            if (l10.objectiveType == LevelDefinition.TYPE_SCORE) {
                w10sum += l10.targetScore;
                w10count++;
            }
        }
        final int w1avg = w1count == 0 ? 0 : w1sum / w1count;
        final int w10avg = w10count == 0 ? 0 : w10sum / w10count;
        assertTrue("world 10 must be harder than world 1 ("
                + w1avg + " vs " + w10avg + ")", w10avg > w1avg);
    }

    @Test
    public void testStarTiers() {
        final LevelDefinition level = LevelCatalog.getLevel(50);
        assertEquals(1, level.starsFor(0.0f));
        assertEquals(2, level.starsFor(0.25f));
        assertEquals(3, level.starsFor(0.5f));
    }

    @Test
    public void testDailyIsStablePerDay() {
        final LevelDefinition a = LevelCatalog.dailyLevel();
        final LevelDefinition b = LevelCatalog.dailyLevel();
        assertEquals(a.id, b.id);
        assertTrue("daily challenge in the mid band: " + a.id,
                a.id >= 101 && a.id <= 500);
    }

    @Test
    public void testWorldUnlockGates() {
        assertEquals(0, LevelCatalog.getWorldUnlockStars(1));
        assertEquals(30, LevelCatalog.getWorldUnlockStars(2));
        assertEquals(70, LevelCatalog.getWorldUnlockStars(3));
        // Every gate is reachable: world w-1 can grant up to 300 stars,
        // so the gate for world w must fit inside (w-1)*300.
        for (int w = 2; w <= 10; ++w) {
            assertTrue("gate for world " + w + " unreachable",
                    LevelCatalog.getWorldUnlockStars(w) <= (w - 1) * 300);
        }
    }

    @Test
    public void testBossLevels() {
        for (int world = 1; world <= 10; ++world) {
            final LevelDefinition boss = LevelCatalog.getLevel(world * 100);
            assertTrue("world boss must be a boss", boss.isBoss());
        }
    }
}
