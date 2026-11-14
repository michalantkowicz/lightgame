package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tapping the cow walks the rabbit next to it. The cow's life (its state machine): it stands, lies down, rests, sleeps, gets up, walks a step onto a free cell next
 * to it (looking where it goes), and from time to time wishes for a carrot (a bubble over it). Next to it, the round
 * button strokes it (a hand; a heart over the cow and a small one floating up over the player, also while it is hungry) or, when it wishes for a carrot the player
 * carries, gives it (the carrot's picture; its slot shakes, the cow eats and is happy). Without a carrot the hint says
 * "find" with the carrot's picture.
 */
class CowTest {
    /** The rabbit stands under the cow, which has no free cell to walk to. */
    private static final TestMap PEN = TestMap.of(
            "~~~~~",
            "~~M~~",
            "~~P~~",
            "~~.~~",
            "~~~~~");

    /** The rabbit stands above the cow, which has no free cell to walk to. */
    private static final TestMap ON_TOP = TestMap.of(
            "~~~~~",
            "~~.~~",
            "~~P~~",
            "~~M~~",
            "~~~~~");

    /** The cow's only free cell is {@code a}, next to a full chest; the rabbit is far away. */
    private static final TestMap PATH = TestMap.of(
            "~~~~~~~~~",
            "~P...CaM~",
            "~~~~~~~~~");

    /** The cow's only free cell is {@code a}; the rabbit can walk there too. */
    private static final TestMap CORRIDOR = TestMap.of(
            "~~~~~~~",
            "~P.baM~",
            "~~~~~~~");

    /** The rabbit is three cells from the cow, whose only free cell is {@code a}. */
    private static final TestMap ROW = TestMap.of(
            "~~~~~~",
            "~P.aM~",
            "~~~~~~");

    /** Which animation shows which state. */
    private static final Map<String, String> ANIMATIONS = Map.of(
            "STANDING", "cow_brown/idle",
            "WALKING", "cow_brown/walk",
            "LYING_DOWN", "cow_brown/settle",
            "RESTING", "cow_brown/rest",
            "SLEEPING", "cow_brown/sleep",
            "GETTING_UP", "cow_brown/settle",
            "WISHING", "cow_brown/idle",
            "EATING", "cow_brown/eat",
            "HAPPY", "cow_brown/love",
            "STROKED", "cow_brown/love");

    @Test
    void strokingTheCow() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals("STANDING", game.cowState());
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "next to a cow: the hand");

        game.clickButton("STROKE");
        game.frames(2);
        assertEquals("STROKED", game.cowState());
        assertEquals("cow_brown/love", game.cowAnimation(), "a heart over the cow");
        assertEquals(List.of("heart"), game.floatingPictures(), "and a small one floats up over the player");
        game.clickButton("STROKE"); // fading while it enjoys the stroke: nothing more happens
        game.seconds(1);
        assertEquals("STANDING", game.cowState());
        game.seconds(1);
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "it can be stroked again: the button is back");
        assertEquals(List.of(), game.floatingPictures(), "the heart is gone");
        assertFalse(game.crashed());
    }

    /** Above the cow there is no button: the button over the cow would cover the player. */
    @Test
    void noButtonAboveTheCow() {
        GameDriver game = GameDriver.start(ON_TOP.write("cow-on-top"));
        game.seconds(1);
        assertEquals(ON_TOP.cell('P'), game.playerCell());
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "standing above the cow: no hand");
        assertFalse(game.crashed());
    }

    /** The bubble with the wish is over the cow until the rabbit comes next to it; then the button takes its place. */
    @Test
    void theButtonTakesTheBubblesPlaceNextToTheCow() {
        GameDriver game = GameDriver.start(ROW.write("cow-row"));
        waitFor(game, "WISHING", 23);
        game.frames(2);
        assertEquals("carrot", game.cowWish(), "far away: the bubble with a carrot");
        assertEquals(List.of(), game.buttonAlphas("GIVE"));

        game.click(GameDriver.Kind.COW);
        game.seconds(1.5f);
        assertTrue(game.usedFromNeighbours(game.cellOf(GameDriver.Kind.COW)).contains(game.playerCell()));
        assertEquals("hidden", game.cowWish(), "next to it: no bubble");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "the button instead");

        game.click(ROW.cell('P'));
        game.seconds(3);
        assertEquals(List.of(), game.buttonAlphas("GIVE"), "walked away: the button is gone");
        assertEquals("carrot", game.cowWish(), "and the bubble is back");
        assertFalse(game.crashed());
    }

    /** Stroked while it wishes for a carrot, the cow enjoys it and goes on wishing: the carrot button comes back. */
    @Test
    void strokingTheCowWhileItWishesForACarrot() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        waitFor(game, "WISHING", 23);
        game.seconds(1);
        game.clickButton("STROKE");
        game.frames(2);
        assertEquals("STROKED", game.cowState());
        assertEquals("cow_brown/love", game.cowAnimation());
        assertEquals(List.of("heart"), game.floatingPictures());
        waitFor(game, "WISHING", 3);
        game.seconds(1);
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "still hungry: the carrot again");
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and the hand");
        assertFalse(game.crashed());
    }

    @Test
    void tappingTheCowWalksNextToIt() {
        GameDriver game = GameDriver.start(ROW.write("cow-row"));
        game.click(GameDriver.Kind.COW);
        game.seconds(1.5f);
        assertEquals(ROW.cell('a'), game.playerCell(), "like a chest: the rabbit stops on the cell next to it");
        assertEquals("IDLE", game.playerStatus());
        assertEquals(ROW.cell('M'), game.cellOf(GameDriver.Kind.COW));
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and can stroke it");
        assertFalse(game.crashed());
    }

    @Test
    void theCowWishesForACarrotGetsItAndIsHappy() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals("none", game.cowWish());
        waitFor(game, "WISHING", 23); // hungry after 20 s; lying or asleep it first gets up
        game.seconds(1);
        assertEquals("hidden", game.cowWish(), "the rabbit is next to it: no bubble...");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "...but the button with the carrot over the cow");
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and the hand still there");
        assertTrue(x(game, "STROKE") < x(game, "GIVE"), "the hand left of the carrot");

        game.clickButton("GIVE");
        game.frames(2);
        assertEquals("WISHING", game.cowState(), "no carrot in the backpack: nothing to give");
        assertTrue(game.actionButtonShaking("GIVE"), "the button shakes");
        assertTrue(game.describeHud().contains("bubble: \"find\""), "and the face says: find... " + game.describeHud());
        assertTrue(game.hintShowsPicture(), "...the carrot");

        game.putCarrotIntoBackpack();
        game.seconds(1);
        game.clickButton("GIVE");
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -]"), game.describeHud());
        assertTrue(game.backpackSlotShaking(0), "the slot it came from shakes");
        assertEquals("EATING", game.cowState());
        assertEquals("cow_brown/eat", game.cowAnimation());
        assertEquals("none", game.cowWish(), "the bubble is gone");

        waitFor(game, "HAPPY", 3);
        assertEquals("cow_brown/love", game.cowAnimation());
        assertEquals(List.of(), game.floatingPictures(), "the heart over its head is all it shows");
        waitFor(game, "STANDING", 2);
        game.seconds(2);
        assertEquals(List.of(), game.floatingPictures());
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "it wishes for nothing: the hand again");
        assertFalse(game.crashed());
    }

    /** Fed whenever it wishes for a carrot, in a few minutes the cow does everything it can; each state has its animation. */
    @Test
    void everyStateShowsItsAnimation() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        Set<String> seen = new HashSet<>();
        for (int frame = 0; frame < 5 * 60 * 60; frame++) {
            String state = game.cowState();
            seen.add(state);
            if (!"WALKING".equals(state)) { // in the pen it can't walk: it turns back to standing at once
                assertEquals(ANIMATIONS.get(state), game.cowAnimation(), state);
            }
            if ("WISHING".equals(state) && game.describeHud().contains("backpack: [-, -]")) {
                game.putCarrotIntoBackpack();
            } else if ("WISHING".equals(state) && !game.buttonAlphas("GIVE").isEmpty()) {
                game.clickButton("GIVE");
            }
            game.frames(1);
        }
        assertEquals(ANIMATIONS.keySet(), union(seen, "STROKED"), "everything but being stroked");
        assertEquals(PEN.cell('M'), game.cellOf(GameDriver.Kind.COW), "it never left the pen");
        assertFalse(game.crashed());
    }

    @Test
    void theCowWalksOntoTheFreeCellNextToIt() {
        GameDriver game = GameDriver.start(PATH.write("cow-path"));
        Cell start = PATH.cell('M');
        Cell free = PATH.cell('a');
        assertTrue(game.isAccessible(free));
        float startX = game.cowXY()[0];

        waitFor(game, "WALKING", 19);
        assertEquals("cow_brown/walk", game.cowAnimation());
        assertTrue(game.cowLooksLeft(), "it goes left, so it looks left");
        assertEquals(free, game.cellOf(GameDriver.Kind.COW), "it is on the cell it goes to at once");
        assertFalse(game.isAccessible(free), "which nobody else can take");
        assertFalse(game.isAccessible(start), "and it still blocks the one it leaves");
        game.seconds(0.5f);
        float x = game.cowXY()[0];
        assertTrue(x < startX && x > startX - 128, "on its way: " + x);

        waitFor(game, "STANDING", 2);
        assertEquals(startX - 128, game.cowXY()[0], 0.01f, "one cell to the left");
        assertFalse(game.cowLooksLeft(), "standing, it looks right again");
        assertTrue(game.isAccessible(start), "the cell it left is free");
        assertFalse(game.isAccessible(free));
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "a cow next to a chest doesn't get its button");
        assertFalse(game.crashed());
    }

    /** The rabbit walks to the cell the cow takes meanwhile: it turns back at the cell's edge and stays in front of it. */
    @Test
    void thePlayerStepsBackFromACellTheCowTook() {
        GameDriver probe = GameDriver.start(CORRIDOR.write("cow-corridor"));
        int framesUntilTheCowWalks = 0;
        while (!"WALKING".equals(probe.cowState())) {
            probe.frames(1);
            framesUntilTheCowWalks++;
            assertTrue(framesUntilTheCowWalks < 19 * 60, "the cow never walked");
        }

        GameDriver game = GameDriver.start(CORRIDOR.write("cow-corridor"));
        Cell taken = CORRIDOR.cell('a');
        game.frames(framesUntilTheCowWalks - 40); // the rabbit needs 72 frames for its 3 cells
        game.click(taken);
        game.frames(40);
        assertEquals("WALKING", game.cowState(), "the cow sets off while the rabbit is on its way");
        assertEquals(taken, game.cellOf(GameDriver.Kind.COW));
        for (int frame = 0; frame < 120; frame++) {
            game.frames(1);
            assertNotEquals(taken, game.playerCell(), "the rabbit never gets onto the cow's cell");
        }
        assertEquals("IDLE", game.playerStatus());
        assertEquals(CORRIDOR.cell('b'), game.playerCell(), "it stands on the cell in front of the cow");
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and can stroke it");
        assertFalse(game.crashed());
    }

    @Test
    void theDebugLabelSaysWhatTheCowDoes() {
        assertEquals("none", GameDriver.start(PEN.write("cow-pen")).cowStateLabel(), "off by default");
        GameDriver.showStateLabels(true);
        try {
            GameDriver game = GameDriver.start(PEN.write("cow-pen"));
            assertEquals("STANDING", game.cowStateLabel());
            game.clickButton("STROKE");
            game.frames(2);
            assertEquals("STROKED", game.cowStateLabel());
        } finally {
            GameDriver.showStateLabels(false);
        }
    }

    /** The left edge of a button on the screen. */
    private static float x(GameDriver game, String button) {
        return Float.parseFloat(game.buttonOnScreen(button).split(",")[0]);
    }

    private static void waitFor(GameDriver game, String state, float maxSeconds) {
        for (int frame = 0; frame < maxSeconds * 60 && !state.equals(game.cowState()); frame++) {
            game.frames(1);
        }
        assertEquals(state, game.cowState(), "after " + maxSeconds + " s");
    }

    private static Set<String> union(Set<String> set, String more) {
        Set<String> all = new HashSet<>(set);
        all.add(more);
        return all;
    }
}
