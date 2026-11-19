package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.math.MathUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fishing game's rules ({@link RhythmRound}): 12 fish in 8 s, on half-second beats, three lanes; a tap at the line
 * catches, a fish past the line is missed, three misses lose, a tap too early scares the fish off (a miss), with no fish in sight it does nothing.
 */
class RhythmRoundTest {
    private static final float FRAME = 1 / 60f;

    @Test
    void twelveFishReachTheLineOnBeatsWithinEightSeconds() {
        for (long seed = 1; seed <= 50; seed++) {
            MathUtils.random.setSeed(seed);
            RhythmRules rules = RhythmRules.forLevel(RhythmRules.AGE_3);
            List<RhythmRound.Fish> fish = new RhythmRound(rules).getFish();
            assertEquals(12, fish.size());
            float first = fish.get(0).getArrival(), last = fish.get(fish.size() - 1).getArrival();
            assertTrue(last - first <= 8, "a run of at most 8 s: " + (last - first));
            for (int i = 1; i < fish.size(); i++) {
                float gap = fish.get(i).getArrival() - fish.get(i - 1).getArrival();
                assertTrue(gap >= 0.5f - 1e-4f, "at least a beat apart: " + gap);
                assertEquals(Math.round(gap / 0.5f), gap / 0.5f, 1e-3f, "a whole number of beats: " + gap);
                if (i >= 2) {
                    assertFalse(fish.get(i).getLane() == fish.get(i - 1).getLane()
                            && fish.get(i).getLane() == fish.get(i - 2).getLane(), "not three down one lane");
                }
            }
        }
    }

    @Test
    void aTapAtTheLineCatchesAndPlayingWellWins() {
        MathUtils.random.setSeed(7);
        RhythmRound round = new RhythmRound(RhythmRules.forLevel(RhythmRules.AGE_5));
        while (!round.isOver()) {
            for (RhythmRound.Fish fish : round.getFish()) {
                if (fish.getState() == RhythmRound.State.COMING && round.getTime() >= fish.getArrival()) {
                    assertNotNull(round.tap(fish.getLane()));
                }
            }
            round.update(FRAME);
        }
        assertTrue(round.isWon());
        assertEquals(12, round.getCaught());
    }

    @Test
    void threeFishPastTheLineLose() {
        MathUtils.random.setSeed(7);
        RhythmRound round = new RhythmRound(RhythmRules.forLevel(RhythmRules.AGE_3));
        float third = round.getFish().get(2).getArrival() + round.getRules().window();
        while (!round.isOver()) {
            round.update(FRAME);
        }
        assertFalse(round.isWon());
        assertEquals(3, round.getMissed(), "over at the third miss");
        assertTrue(round.getTime() < third + 2 * FRAME, "at once, not after the last fish: " + round.getTime());
    }

    @Test
    void aTapTooEarlyScaresTheFishOffAndWithNoFishInSightDoesNothing() {
        MathUtils.random.setSeed(7);
        RhythmRound round = new RhythmRound(new RhythmRules(8, 0.5f, 12, 3, 3, 2, 0.2f));
        RhythmRound.Fish first = round.getFish().get(0);
        for (int lane = 0; lane < 3; lane++) {
            assertNull(round.tap(lane), "nothing in sight yet");
        }
        assertEquals(0, round.getMissed());
        runUntil(round, first.getArrival() - 0.5f);
        assertTrue(round.isTooEarly(first.getLane()));
        RhythmRound.Fish scared = round.tap(first.getLane());
        assertEquals(first, scared, "the fish coming down that lane");
        assertEquals(RhythmRound.State.MISSED, scared.getState());
        assertEquals(1, round.getMissed(), "a miss");
        runUntil(round, first.getArrival());
        assertFalse(round.isCatchable(first.getLane()), "it is gone");
    }

    private static void runUntil(RhythmRound round, float time) {
        while (round.getTime() < time) {
            round.update(FRAME);
        }
    }
}
