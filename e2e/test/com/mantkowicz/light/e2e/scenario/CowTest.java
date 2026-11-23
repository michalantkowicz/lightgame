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
 * Tapping the cow walks the rabbit next to it. The cow's life (its state machine): it stands, lies down, rests, sleeps,
 * gets up, walks a step onto a free cell next to it (looking where it goes), grazes now and then. It wishes for nothing
 * (no bubble by day): its favourite crop (Tiled's favouriteDish, a carrot without it) can be given whenever it stands or
 * lies. Next to it, the round buttons stroke it (a hand; a heart over the cow and a small one floating up over the
 * player) and give it its favourite (the crop's picture; its slot shakes, the cow eats and is happy). Without that crop
 * the hint says "find" with its picture.
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

    /** The rabbit stands under a cow whose favouriteDish lists carrots and cucumbers (the first counts). */
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
        game.seconds(2); // 20 s after it, the hand fades back in (once the cow does something it can be stroked in)
        for (int frame = 0; frame < 20 * 60 && !game.buttonAlphas("STROKE").equals(List.of(1f)); frame++) {
            game.frames(1);
        }
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

    /**
     * Next to the cow, whenever it can be fed (standing), a button with its favourite crop (a carrot without Tiled's
     * favouriteDish) is over it, right of the hand. Without one in the backpack the button shakes and the face says
     * "find" with it; with one, the cow eats it (its slot shakes) and is happy, and can be fed again at once after.
     */
    @Test
    void theCowCanBeFedItsFavouriteWhenItStands() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        assertEquals("carrot", game.cowFavourite());
        assertEquals("STANDING", game.cowState());
        game.frames(2);
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "the button with its favourite");
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "and the hand");
        assertTrue(x(game, "STROKE") < x(game, "GIVE"), "the hand left of the crop");
        assertEquals("none", game.cowBubble(), "no bubble: it wishes for nothing");

        game.clickButton("GIVE");
        game.frames(2);
        assertEquals("STANDING", game.cowState(), "nothing in the backpack: nothing to give");
        assertTrue(game.actionButtonShaking("GIVE"), "the button shakes");
        assertTrue(game.describeHud().contains("bubble: \"find\""), "and the face says: find... " + game.describeHud());
        assertTrue(game.hintShowsPicture(), "...the carrot");

        game.putCarrotIntoBackpack();
        game.seconds(1);
        game.clickButton("GIVE");
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
        assertTrue(game.backpackSlotShaking(0), "the slot it came from shakes");
        assertEquals("EATING", game.cowState());
        assertEquals("cow_brown/eat", game.cowAnimation());
        assertFalse(game.canBeFed(GameDriver.Kind.COW), "eating: nothing more now");

        waitFor(game, "HAPPY", 3);
        assertEquals("cow_brown/love", game.cowAnimation());
        waitFor(game, "STANDING", 2);
        game.frames(30);
        assertTrue(game.canBeFed(GameDriver.Kind.COW), "it can be fed again at once");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "the button is back");
        assertFalse(game.crashed());
    }

    /** Lying (resting or napping), it can be fed too: it gets up first, then eats. */
    @Test
    void lyingItGetsUpToEat() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        waitFor(game, "RESTING", 120);
        game.frames(2);
        assertTrue(game.canBeFed(GameDriver.Kind.COW));
        game.putCarrotIntoBackpack();
        game.frames(30);
        game.clickButton("GIVE");
        game.frames(2);
        assertEquals("GETTING_UP", game.cowState(), "it gets up first");
        waitFor(game, "EATING", 2);
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
        assertFalse(game.crashed());
    }

    /**
     * Next to the cow, tapping the slot with its favourite feeds it, as its button does; a slot with something else
     * throws its item out as usual. With several crops in Tiled's favouriteDish, the first is its favourite.
     */
    @Test
    void tappingTheSlotWithItsFavouriteFeedsTheCow() {
        GameDriver game = GameDriver.start(PICKY.write("cow-picky"));
        assertEquals("carrot", game.cowFavourite(), "the first of CARROT, CUCUMBER");
        game.putIntoBackpack("cucumber");
        game.putIntoBackpack("carrot");
        game.seconds(1);

        game.tapBackpackSlot(0);
        game.frames(2);
        assertFalse("EATING".equals(game.cowState()), "not its favourite: it doesn't eat it");
        assertNull(game.backpackItems().get(0), "the item jumped out, as usual");
        assertNotNull(game.backpackItems().get(1));
        assertEquals(1, game.itemsOnGround().size(), game.itemsOnGround().toString());

        waitForFeedable(game);
        game.tapBackpackSlot(1);
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
        assertTrue(game.backpackSlotShaking(1), "the slot it came from shakes");
        assertTrue(List.of("EATING", "GETTING_UP").contains(game.cowState()), "the cow got it: " + game.cowState());
        assertEquals(1, game.itemsOnGround().size(), "nothing more on the ground");
        assertFalse(game.crashed());
    }

    /** Its favourite comes from Tiled's favouriteDish (one crop); without it, or empty, a carrot. */
    @Test
    void itsFavouriteComesFromTiled() {
        assertEquals("carrot", GameDriver.start(PEN.write("cow-pen")).cowFavourite(), "no favouriteDish");
        for (Map.Entry<String, String> dish : Map.of("", "carrot", "CUCUMBER", "cucumber", "TOMATO", "tomato")
                .entrySet()) {
            TestMap map = TestMap.of(
                    "~~~~~",
                    "~~K~~",
                    "~~P~~",
                    "~~~~~")
                    .with('K', "COW", Map.of("favouriteDish", dish.getKey()));
            GameDriver game = GameDriver.start(map.write("cow-dish"));
            assertEquals(dish.getValue(), game.cowFavourite(), "favouriteDish \"" + dish.getKey() + "\"");
            assertFalse(game.crashed());
        }
    }

    /** It wishes for nothing: no bubble over it all day (without quests), and now and then it grazes by itself. */
    @Test
    void noBubbleByDayAndItGrazesNowAndThen() {
        GameDriver game = GameDriver.start(LONG_ROW.write("cow-long-row"));
        stepAway(game);
        List<String> grazing = new ArrayList<>();
        for (int frame = 0; frame < 6 * 60 * 60; frame++) {
            assertEquals("none", game.cowBubble(), "frame " + frame);
            String state = game.cowState();
            if (("GRAZING".equals(state) || "CHEWING".equals(state))
                    && (grazing.isEmpty() || !state.equals(grazing.get(grazing.size() - 1)))) {
                grazing.add(state);
                assertEquals(ANIMATIONS.get(state), game.cowAnimation(), state);
            }
            game.frames(1);
        }
        assertTrue(grazing.size() >= 6, "it grazed by itself, bites and chewing: " + grazing);
        assertFalse(game.crashed());
    }

    /**
     * Fed now and then, within minutes the cow does everything it can (how soon depends on its random choices); each
     * state has its animation.
     */
    @Test
    void everyStateShowsItsAnimation() {
        GameDriver game = GameDriver.start(PEN.write("cow-pen"));
        Set<String> seen = new HashSet<>();
        for (int frame = 0; frame < 15 * 60 * 60
                && !union(seen, "BEING_STROKED", "STROKED").equals(ANIMATIONS.keySet()); frame++) {
            String state = game.cowState();
            seen.add(state);
            if (!"WALKING".equals(state)) { // in the pen it can't walk: it turns back to standing at once
                assertEquals(ANIMATIONS.get(state), game.cowAnimation(), state);
            }
            if (frame % (20 * 60) == 0 && game.canBeFed(GameDriver.Kind.COW)
                    && game.describeHud().contains("backpack: [-, -, -]")) {
                game.putCarrotIntoBackpack();
                game.frames(30);
                if (!game.buttonAlphas("GIVE").isEmpty()) {
                    game.clickButton("GIVE");
                }
            }
            game.frames(1);
        }
        assertEquals(ANIMATIONS.keySet(), union(seen, "BEING_STROKED", "STROKED"), "everything but being stroked");
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
            assertTrue(framesUntilTheCowWalks < 60 * 60, "the cow never walked");
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

    /** The rabbit steps left, a cell at a time, until it is at least three cells from the cow (or can't step further). */
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

    /** Waits (at most a minute) until the cow can be fed (it may be walking or lying down meanwhile). */
    private static void waitForFeedable(GameDriver game) {
        for (int frame = 0; frame < 60 * 60 && !game.canBeFed(GameDriver.Kind.COW); frame++) {
            game.frames(1);
        }
        assertTrue(game.canBeFed(GameDriver.Kind.COW));
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
