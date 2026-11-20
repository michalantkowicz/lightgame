package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tapping the cow walks the rabbit next to it. The cow's life (its state machine): it stands, lies down, rests, sleeps, gets up, walks a step onto a free cell next
 * to it (looking where it goes), and from time to time wishes for a crop (a bubble over it): one of its favourite dishes
 * (Tiled's favouriteDish, any crop without it), picked at random each time it gets hungry. Next to it, the round
 * button strokes it (a hand; a heart over the cow and a small one floating up over the player, also while it is hungry) or, when it wishes for a crop the player
 * carries, gives it (the crop's picture; its slot shakes, the cow eats and is happy). Without that crop the hint says
 * "find" with its picture.
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
    /**
     * A long row: the rabbit left of the cow, which can't get past it (and walks a lot): the rabbit can always step
     * away from it to the left.
     */
    private static final TestMap LONG_ROW = TestMap.of(
            "~~~~~~~~~~~",
            "~.....P.aM~",
            "~~~~~~~~~~~");

    private static final TestMap ROW = TestMap.of(
            "~~~~~~",
            "~P.aM~",
            "~~~~~~");

    /** The rabbit stands under a cow that only likes carrots and cucumbers. */
    private static final TestMap PICKY = TestMap.of(
            "~~~~~",
            "~~K~~",
            "~~P~~",
            "~~.~~",
            "~~~~~")
            .with('K', "COW", Map.of("favouriteDish", "CARROT, CUCUMBER"));

    /** Which animation shows which state. */
    private static final Map<String, String> ANIMATIONS = Map.ofEntries(
            Map.entry("STANDING", "cow_brown/idle"),
            Map.entry("WALKING", "cow_brown/walk"),
            Map.entry("LYING_DOWN", "cow_brown/settle"),
            Map.entry("RESTING", "cow_brown/rest"),
            Map.entry("SLEEPING", "cow_brown/sleep"),
            Map.entry("GETTING_UP", "cow_brown/settle"),
            Map.entry("WISHING", "cow_brown/idle"),
            Map.entry("EATING", "cow_brown/eat"),
            Map.entry("HAPPY", "cow_brown/love"),
            Map.entry("BEING_STROKED", "cow_brown/idle"),
            Map.entry("STROKED", "cow_brown/love"),
            Map.entry("GRAZING", "cow_brown/graze"),
            Map.entry("CHEWING", "cow_brown/eat"));

    @Test
    void strokingTheCow() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals("STANDING", game.cowState());
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "next to a cow: the hand");

        game.clickButton("STROKE");
        game.frames(2);
        assertEquals("BEING_STROKED", game.cowState(), "it stands still...");
        assertTrue(game.buttonAlphas("STROKE").get(0) < 1, "...while the hand fades out");
        assertEquals(List.of(), game.floatingPictures());
        game.clickButton("STROKE"); // tapped again while it fades: nothing more happens
        waitFor(game, "STROKED", 1);
        game.frames(2);
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "once the hand is gone, nothing covers the heart");
        assertEquals("cow_brown/love", game.cowAnimation(), "a heart over the cow");
        assertEquals(List.of("heart"), game.floatingPictures(), "and a small one floats up over the player");
        game.seconds(1);
        assertEquals("STANDING", game.cowState());
        game.seconds(1);
        assertEquals(List.of(), game.floatingPictures(), "the heart is gone");
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "no hand: it was just stroked");
        game.seconds(16); // about 18 s after the stroke
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "still no hand");
        game.seconds(3); // 20 s after it, the hand fades back in
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "20 s later it can be stroked again: the hand is back");
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
        GameDriver game = GameDriver.start(LONG_ROW.write("cow-long-row"));
        waitFor(game, "WISHING", 63);
        stepAway(game);
        assertEquals(game.cowWants(), game.cowWish(), "far away: the bubble with what it wishes for");
        assertEquals(List.of(), game.buttonAlphas("GIVE"));

        game.click(GameDriver.Kind.COW);
        game.seconds(1.5f);
        assertTrue(game.usedFromNeighbours(game.cellOf(GameDriver.Kind.COW)).contains(game.playerCell()));
        assertEquals("hidden", game.cowWish(), "next to it: no bubble");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "the button instead");

        stepAway(game);
        assertEquals(List.of(), game.buttonAlphas("GIVE"), "walked away: the button is gone");
        assertEquals(game.cowWants(), game.cowWish(), "and the bubble is back");
        assertFalse(game.crashed());
    }

    /**
     * Stroked while it wishes for a crop, the cow enjoys it and goes on wishing for the same one: the button with it
     * comes back.
     */
    @Test
    void strokingTheCowWhileItWishesForACrop() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        waitFor(game, "WISHING", 63);
        game.seconds(1);
        String wish = game.cowWants();
        game.clickButton("STROKE");
        waitFor(game, "STROKED", 1); // once the hand has faded out
        game.frames(2);
        assertEquals("cow_brown/love", game.cowAnimation());
        assertEquals(List.of("heart"), game.floatingPictures());
        waitFor(game, "WISHING", 3);
        game.seconds(1);
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "still hungry: the crop again");
        assertEquals(wish, game.cowWants(), "the same crop as before");
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "but no hand: it was just stroked");
        assertFalse(game.crashed());
    }

    /** Far from the cow, tapping the bubble with its wish walks the rabbit next to it, as tapping the cow does. */
    @Test
    void tappingTheWishBubbleWalksNextToTheCow() {
        GameDriver game = GameDriver.start(LONG_ROW.write("cow-long-row"));
        waitFor(game, "WISHING", 63);
        stepAway(game);
        Cell far = game.playerCell();
        game.tapWishBubble(GameDriver.Kind.COW);
        game.seconds(2);
        assertNotEquals(far, game.playerCell());
        assertTrue(game.usedFromNeighbours(game.cellOf(GameDriver.Kind.COW)).contains(game.playerCell()),
                "the rabbit stands next to the cow");
        assertEquals("hidden", game.cowWish(), "next to it: the button instead of the bubble");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"));
        assertFalse(game.crashed());
    }

    /**
     * Next to the wishing cow, tapping the slot with what it wishes for gives it, as its button does; a slot with
     * something else (or an empty one) throws its item out as usual.
     */
    @Test
    void tappingTheSlotWithTheWishGivesItToTheCow() {
        GameDriver game = GameDriver.start(PICKY.write("cow-picky"));
        waitFor(game, "WISHING", 63);
        game.seconds(1);
        String wish = game.cowWants();
        String other = "carrot".equals(wish) ? "cucumber" : "carrot";
        game.putIntoBackpack(other);
        game.putIntoBackpack(wish);
        game.seconds(1);

        game.tapBackpackSlot(0);
        game.frames(2);
        assertEquals("WISHING", game.cowState(), "not what it wishes for: it stays hungry");
        assertNull(game.backpackItems().get(0), "the item jumped out, as usual");
        assertNotNull(game.backpackItems().get(1));
        assertEquals(1, game.itemsOnGround().size(), game.itemsOnGround().toString());

        game.tapBackpackSlot(1);
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
        assertTrue(game.backpackSlotShaking(1), "the slot it came from shakes");
        assertEquals("EATING", game.cowState(), "the cow got it");
        assertEquals(1, game.itemsOnGround().size(), "nothing more on the ground");
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
    void theCowWishesForACropGetsItAndIsHappy() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals("none", game.cowWish());
        waitFor(game, "WISHING", 63); // hungry after 20 s; lying or asleep it first gets up
        game.seconds(1);
        assertEquals("hidden", game.cowWish(), "the rabbit is next to it: no bubble...");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "...but the button with the crop over the cow");
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and the hand still there");
        assertTrue(x(game, "STROKE") < x(game, "GIVE"), "the hand left of the crop");

        game.clickButton("GIVE");
        game.frames(2);
        assertEquals("WISHING", game.cowState(), "nothing in the backpack: nothing to give");
        assertTrue(game.actionButtonShaking("GIVE"), "the button shakes");
        assertTrue(game.describeHud().contains("bubble: \"find\""), "and the face says: find... " + game.describeHud());
        assertTrue(game.hintShowsPicture(), "...the crop");

        game.putIntoBackpack(game.cowWants());
        game.seconds(1);
        game.clickButton("GIVE");
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
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

    /**
     * Wishing for a crop for a minute and a half with nobody feeding it, the cow gives up: the bubble goes and it
     * grazes, three bites of grass (each a bite and chewing), then stands again, no longer hungry.
     */
    @Test
    void theCowGrazesWhenNobodyFeedsIt() {
        GameDriver game = GameDriver.start(LONG_ROW.write("cow-long-row"));
        waitFor(game, "WISHING", 63);
        stepAway(game);
        assertEquals(game.cowWants(), game.cowWish());
        List<String> states = new ArrayList<>();
        for (int frame = 0; frame < 120 * 60 && !"STANDING".equals(game.cowState()); frame++) {
            String state = game.cowState();
            if (states.isEmpty() || !state.equals(states.get(states.size() - 1))) {
                states.add(state);
                if (!"WISHING".equals(state)) {
                    assertEquals(ANIMATIONS.get(state), game.cowAnimation(), state);
                }
            }
            game.frames(1);
        }
        assertEquals(List.of("WISHING", "GRAZING", "CHEWING", "GRAZING", "CHEWING", "GRAZING", "CHEWING"), states,
                "it gives up and takes three bites");
        assertEquals("none", game.cowWish(), "the bubble is gone");
        game.seconds(15);
        assertFalse("WISHING".equals(game.cowState()), "it has eaten: not hungry for a while");
        assertFalse(game.crashed());
    }

    /**
     * Fed whenever it wishes for a crop, within minutes the cow does everything it can (how soon depends on its random
     * choices); each state has its animation.
     */
    @Test
    void everyStateShowsItsAnimation() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        Set<String> seen = new HashSet<>();
        for (int frame = 0; frame < 15 * 60 * 60
                && !union(seen, "BEING_STROKED", "STROKED", "GRAZING", "CHEWING")
                        .equals(ANIMATIONS.keySet()); frame++) {
            String state = game.cowState();
            seen.add(state);
            if (!"WALKING".equals(state)) { // in the pen it can't walk: it turns back to standing at once
                assertEquals(ANIMATIONS.get(state), game.cowAnimation(), state);
            }
            if ("WISHING".equals(state) && game.describeHud().contains("backpack: [-, -, -]")) {
                game.putIntoBackpack(game.cowWants());
            } else if ("WISHING".equals(state) && !game.buttonAlphas("GIVE").isEmpty()) {
                game.clickButton("GIVE");
            }
            game.frames(1);
        }
        // fed each time, it never grazes (theCowGrazesWhenNobodyFeedsIt)
        assertEquals(ANIMATIONS.keySet(), union(seen, "BEING_STROKED", "STROKED", "GRAZING", "CHEWING"),
                "everything but being stroked");
        assertEquals(PEN.cell('M'), game.cellOf(GameDriver.Kind.COW), "it never left the pen");
        assertFalse(game.crashed());
    }

    /** Without favouriteDish in Tiled, or with it empty, the cow may wish for any crop. */
    @Test
    void aCowMayWishForAnyCrop() {
        List<String> all = List.of("corn", "carrot", "tomato", "pumpkin", "cucumber");
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals(all, game.cowFavouriteDishes(), "no favouriteDish");
        assertFalse(game.crashed());

        TestMap empty = TestMap.of(
                "~~~~~",
                "~~K~~",
                "~~P~~",
                "~~~~~")
                .with('K', "COW", Map.of("favouriteDish", ""));
        game = GameDriver.start(empty.write("cow-empty-dish"));
        assertEquals(all, game.cowFavouriteDishes(), "an empty favouriteDish");
        assertFalse(game.crashed());
    }

    /**
     * Its favourite dishes come from Tiled. Fed every time, it picks one of them at random each time it gets hungry
     * (both turn up), never anything else; a crop it doesn't wish for can't be given (the button shakes, it stays
     * hungry).
     */
    @Test
    void theCowWishesOnlyForItsFavouriteDishes() {
        GameDriver game = GameDriver.start(PICKY.write("cow-picky"));
        assertEquals(List.of("carrot", "cucumber"), game.cowFavouriteDishes());

        waitFor(game, "WISHING", 63);
        game.seconds(1);
        String other = "carrot".equals(game.cowWants()) ? "cucumber" : "carrot";
        game.putIntoBackpack(other);
        game.clickButton("GIVE");
        game.frames(2);
        assertEquals("WISHING", game.cowState(), "the other crop: nothing to give");
        assertTrue(game.actionButtonShaking("GIVE"), "the button shakes");
        assertTrue(game.describeHud().contains("backpack: [" + capital(other) + ", -, -]"), game.describeHud());

        Set<String> wishes = new HashSet<>();
        for (int frame = 0; frame < 5 * 60 * 60; frame++) {
            if ("WISHING".equals(game.cowState()) && game.buttonAlphas("GIVE").equals(List.of(1f))) {
                String wish = game.cowWants();
                wishes.add(wish);
                if (!wish.equals(other)) {
                    game.putIntoBackpack(wish); // into the free slot
                }
                game.clickButton("GIVE");
                game.frames(2);
                assertEquals("EATING", game.cowState(), "its wish: it eats");
                if (wish.equals(other)) {
                    game.putIntoBackpack(other); // keep one of the other crop for later
                }
            }
            game.frames(1);
        }
        assertEquals(Set.of("carrot", "cucumber"), wishes, "both, and only those");
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
            game.seconds(0.8f); // the hand fades out first
            assertEquals("STROKED", game.cowStateLabel());
        } finally {
            GameDriver.showStateLabels(false);
        }
    }

    private static String capital(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** The left edge of a button on the screen. */
    private static float x(GameDriver game, String button) {
        return Float.parseFloat(game.buttonOnScreen(button).split(",")[0]);
    }

    /** The rabbit steps left, a cell at a time, until it is at least three cells from the cow (the cow wishes: it stays). */
    private static void stepAway(GameDriver game) {
        for (int step = 0; step < 8; step++) {
            Cell rabbit = game.playerCell(), cow = game.cellOf(GameDriver.Kind.COW);
            Cell left = new Cell(rabbit.col() - 1, rabbit.row());
            if (Math.abs(cow.col() - rabbit.col()) >= 3 || !game.isAccessible(left) || !game.isVisible(left)) {
                break;
            }
            game.click(left);
            game.seconds(1.5f);
        }
        game.seconds(1);
    }

    private static void waitFor(GameDriver game, String state, float maxSeconds) {
        for (int frame = 0; frame < maxSeconds * 60 && !state.equals(game.cowState()); frame++) {
            game.frames(1);
        }
        assertEquals(state, game.cowState(), "after " + maxSeconds + " s");
    }

    private static Set<String> union(Set<String> set, String... more) {
        Set<String> all = new HashSet<>(set);
        all.addAll(List.of(more));
        return all;
    }
}
