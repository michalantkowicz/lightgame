package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprite animations. The rabbit is animated by direction: it walks right with "walk_right" and when it stops it turns
 * back to the default "idle_down"; a diagonal walk keeps one direction. The cow chews in its place ("idle"); the chest's
 * lid opens when it is opened and stays open.
 */
class AnimationTest {
    /** Targets around the rabbit: e(ast), w(est), n(orth), s(outh), r = north-east, q = north-west. */
    private static final TestMap FIELD = TestMap.of(
            "~~~~~~~~~",
            "~...n...~",
            "~.q...r.~",
            "~.......~",
            "~.w.P.e.~",
            "~.......~",
            "~...s...~",
            "~~~~~~~~~");
    private static final TestMap FARM = TestMap.of(
            "~~~~~~~",
            "~M.C..~",
            "~.....~",
            "~..P..~",
            "~~~~~~~");
    private static final int MAX_WALK_FRAMES = 300;

    @Test
    void theRabbitIsAnimatedInTheDirectionItWalks() {
        GameDriver game = GameDriver.start(FIELD.write("field"));
        assertEquals("rabbit/idle_down", game.playerAnimation());

        game.click(FIELD.cell('e'));
        game.frames(10);
        assertEquals("rabbit/walk_right", game.playerAnimation());

        game.seconds(3);
        assertEquals(FIELD.cell('e'), game.playerCell());
        assertEquals("rabbit/idle_down", game.playerAnimation(), "back to the default pose after the walk");
        assertFalse(game.crashed());
    }

    @Test
    void everyDirectionHasItsWalk() {
        assertEquals(Set.of("rabbit/walk_left"), animationsWhileWalkingTo('w'));
        assertEquals(Set.of("rabbit/walk_up"), animationsWhileWalkingTo('n'));
        assertEquals(Set.of("rabbit/walk_down"), animationsWhileWalkingTo('s'));
    }

    @Test
    void diagonalWalksDontFlickerBetweenTwoDirections() {
        assertEquals(Set.of("rabbit/walk_right"), animationsWhileWalkingTo('r'));
        assertEquals(Set.of("rabbit/walk_left"), animationsWhileWalkingTo('q'));
    }

    @Test
    void theRabbitsFrameIsCentredOnItsTile() {
        GameDriver game = GameDriver.start(FIELD.write("field"));
        assertArrayEquals(new float[]{64, 64}, game.playerFrameCentre(), 0.001f, "the middle of the 128 px tile");
    }

    @Test
    void theCowStandsInItsPlaceChewingAway() {
        GameDriver game = GameDriver.start(FARM.write("farm"));
        Cell cow = FARM.cell('M');
        assertEquals(cow, game.cellOf(GameDriver.Kind.COW));
        assertEquals("cow_brown/idle", game.cowAnimation(), "its default animation");
        assertFalse(game.isAccessible(cow), "nobody walks through a cow");
    }

    @Test
    void theCowStandsInTheMiddleOfItsTile() {
        GameDriver game = GameDriver.start(FARM.write("farm"));
        // the cow is drawn low in its 32x32 frame (its pixels span x 3..30, y 12..30): its body's middle is (16, 21)
        assertArrayEquals(new float[]{64, 64}, game.cowFramePixel(16, 21), 0.001f, "on the middle of the 128 px tile");
    }

    @Test
    void theChestOpensAndStaysOpen() {
        GameDriver game = GameDriver.start(FARM.write("farm"));
        Cell chest = FARM.cell('C');
        assertEquals(chest, game.cellOf(GameDriver.Kind.CHEST));
        assertFalse(game.isAccessible(chest), "the chest blocks its cell");
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "closed at the start");

        game.click(chest);
        game.seconds(3);
        assertTrue(game.usedFromNeighbours(chest).contains(game.playerCell()),
                "clicking the chest walks next to its side");

        game.clickButton("COLLECT");
        game.seconds(1);
        assertEquals("chest/open_front", game.chestAnimation(), "the lid opens");

        game.seconds(3);
        assertEquals("chest/open_front", game.chestAnimation(), "and stays open");
        assertFalse(game.crashed());
    }

    /** Every animation the rabbit shows while walking to the target, in the order they appear. */
    private static Set<String> animationsWhileWalkingTo(char target) {
        GameDriver game = GameDriver.start(FIELD.write("field"));
        game.click(FIELD.cell(target));
        Set<String> shown = new LinkedHashSet<>();
        for (int frame = 0; frame < MAX_WALK_FRAMES; frame++) {
            game.frames(1);
            if ("MOVEMENT".equals(game.playerStatus())) {
                shown.add(game.playerAnimation());
            } else if (!shown.isEmpty()) {
                break;
            }
        }
        assertEquals(FIELD.cell(target), game.playerCell(), "the rabbit gets there");
        assertFalse(game.crashed());
        return shown;
    }
}
