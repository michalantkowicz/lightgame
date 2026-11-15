package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chickens: a chicken and a chick (Tiled's CHICKEN and CHICKEN_BABY) live like the cow (they get hungry, wish for a
 * crop, eat it, enjoy a stroke, rest and sleep) but they are restless: they walk more than the cow (the chick most),
 * peck at the ground often, wish only for corn, and soon get bored of wishing: then they peck at the ground three
 * times and are no longer hungry. Happy or stroked, hearts rise over them.
 */
class ChickenTest {
    /** The rabbit stands under a chicken that can't walk anywhere. */
    private static final TestMap PEN = TestMap.of(
            "~~~~~",
            "~~H~~",
            "~~P~~",
            "~~.~~",
            "~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The same with a chick. */
    private static final TestMap CHICK_PEN = TestMap.of(
            "~~~~~",
            "~~B~~",
            "~~P~~",
            "~~.~~",
            "~~~~~")
            .with('B', "CHICKEN_BABY", Map.of());

    /** A cow, a chicken and a chick on open grass; the rabbit in a corner. */
    private static final TestMap FARM = TestMap.of(
            "~~~~~~~~~~~",
            "~P........~",
            "~.........~",
            "~..M......~",
            "~.........~",
            "~.....H...~",
            "~.........~",
            "~.......B.~",
            "~.........~",
            "~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .with('B', "CHICKEN_BABY", Map.of());

    /** Which animation shows which state (the chicken's; the chick's rows have the same names). */
    private static final Map<String, String> ANIMATIONS = Map.ofEntries(
            Map.entry("STANDING", "idle"),
            Map.entry("WALKING", "walk"),
            Map.entry("PECKING", "peck"),
            Map.entry("SITTING_DOWN", "settle"),
            Map.entry("RESTING", "rest"),
            Map.entry("SLEEPING", "sleep"),
            Map.entry("GETTING_UP", "rise"),
            Map.entry("WISHING", "idle"),
            Map.entry("EATING", "eat"),
            Map.entry("FORAGING", "peck"),
            Map.entry("HAPPY", "love"),
            Map.entry("STROKED", "love"));

    @Test
    void chickensWishOnlyForCorn() {
        GameDriver game = GameDriver.start(FARM.write("chicken-farm"));
        assertEquals(List.of("corn"), game.animalFavouriteDishes(Kind.CHICKEN));
        assertEquals(List.of("corn"), game.animalFavouriteDishes(Kind.CHICKEN_BABY));
        assertFalse(game.crashed());
    }

    /** Over ten minutes the chick takes the most steps, then the chicken, then the cow; the chickens peck a lot. */
    @Test
    void chickensAreRestless() {
        GameDriver game = GameDriver.start(FARM.write("chicken-farm"));
        Map<Kind, Integer> steps = new HashMap<>();
        Map<Kind, Integer> pecks = new HashMap<>();
        Map<Kind, Cell> cells = new HashMap<>();
        Map<Kind, String> states = new HashMap<>();
        List<Kind> animals = List.of(Kind.COW, Kind.CHICKEN, Kind.CHICKEN_BABY);
        for (int frame = 0; frame < 10 * 60 * 60; frame++) {
            for (Kind animal : animals) {
                Cell cell = game.cellOf(animal);
                if (!cell.equals(cells.put(animal, cell)) && frame > 0) {
                    steps.merge(animal, 1, Integer::sum);
                }
                String state = game.animalState(animal);
                if (!state.equals(states.put(animal, state)) && "PECKING".equals(state)) {
                    pecks.merge(animal, 1, Integer::sum);
                }
            }
            game.frames(1);
        }
        int cow = steps.getOrDefault(Kind.COW, 0);
        int chicken = steps.getOrDefault(Kind.CHICKEN, 0);
        int chick = steps.getOrDefault(Kind.CHICKEN_BABY, 0);
        assertTrue(chick > chicken && chicken > cow, "steps: chick " + chick + ", chicken " + chicken + ", cow " + cow);
        assertTrue(pecks.getOrDefault(Kind.CHICKEN, 0) >= 30, "the chicken pecks often: " + pecks);
        assertTrue(pecks.getOrDefault(Kind.CHICKEN_BABY, 0) >= 30, "the chick pecks often: " + pecks);
        assertFalse(game.crashed());
    }

    /** Nobody feeds it: hungry, it wishes for corn, and 15 s later, bored, it pecks at the ground three times. */
    @Test
    void aChickenSoonGetsBoredOfWishing() {
        GameDriver game = GameDriver.start(PEN.write("chicken-pen"));
        waitFor(game, Kind.CHICKEN, "WISHING", 25);
        game.seconds(1);
        assertEquals("corn", game.animalWants(Kind.CHICKEN));
        List<String> states = new ArrayList<>();
        int frames = 0;
        int bored = -1;
        for (; frames < 30 * 60 && !"STANDING".equals(game.animalState(Kind.CHICKEN)); frames++) {
            String state = game.animalState(Kind.CHICKEN);
            if (states.isEmpty() || !state.equals(states.get(states.size() - 1))) {
                states.add(state);
                if ("FORAGING".equals(state)) {
                    bored = frames;
                }
            }
            game.frames(1);
        }
        assertEquals(List.of("WISHING", "FORAGING"), states, "it gives up and pecks at the ground");
        assertTrue(bored > 13 * 60 && bored < 15 * 60,
                "bored 15 s after it began wishing: " + (bored / 60f + 1) + " s");
        assertEquals("none", game.animalWish(Kind.CHICKEN), "the bubble is gone");
        game.seconds(10);
        assertFalse("WISHING".equals(game.animalState(Kind.CHICKEN)), "it found something: not hungry for a while");
        assertFalse(game.crashed());
    }

    /** Given corn, the chicken eats it and hearts rise over it, one frame (16 art pixels, 128 px) above it. */
    @Test
    void aChickenFedCornIsHappy() {
        GameDriver game = GameDriver.start(PEN.write("chicken-pen"));
        waitFor(game, Kind.CHICKEN, "WISHING", 25);
        game.seconds(1);
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"), "next to it: the button with the corn");
        game.putIntoBackpack("corn");
        game.clickButton("GIVE");
        assertEquals("EATING", game.animalState(Kind.CHICKEN));
        assertEquals("chicken/eat", game.animalAnimation(Kind.CHICKEN));
        game.seconds(2.1f);
        assertEquals("HAPPY", game.animalState(Kind.CHICKEN));
        assertEquals("chicken/love", game.animalAnimation(Kind.CHICKEN));
        assertTrue(game.chickenShowsHearts(Kind.CHICKEN));
        assertArrayEquals(new float[]{0, 128}, game.chickenHeartsOffset(Kind.CHICKEN));
        game.seconds(1.5f);
        assertFalse(game.chickenShowsHearts(Kind.CHICKEN), "the hearts are gone");
        assertEquals("STANDING", game.animalState(Kind.CHICKEN));
        assertFalse(game.crashed());
    }

    @Test
    void strokingAChick() {
        GameDriver game = GameDriver.start(CHICK_PEN.write("chick-pen"));
        game.seconds(0.5f);
        waitForStrokable(game);
        game.clickButton("STROKE");
        assertEquals("STROKED", game.animalState(Kind.CHICKEN_BABY));
        assertEquals("chicken_baby/love", game.animalAnimation(Kind.CHICKEN_BABY));
        assertTrue(game.chickenShowsHearts(Kind.CHICKEN_BABY));
        game.seconds(1.5f);
        assertEquals("STANDING", game.animalState(Kind.CHICKEN_BABY));
        assertFalse(game.crashed());
    }

    /** Fed whenever they wish, within minutes both do everything they can; each state has its animation. */
    @Test
    void everyStateShowsItsAnimation() {
        for (Kind kind : List.of(Kind.CHICKEN, Kind.CHICKEN_BABY)) {
            GameDriver game = GameDriver.start(FARM.write("chicken-farm"));
            String sheet = kind == Kind.CHICKEN ? "chicken/" : "chicken_baby/";
            Set<String> seen = new HashSet<>();
            Set<String> wanted = new HashSet<>(ANIMATIONS.keySet());
            wanted.removeAll(Set.of("EATING", "HAPPY", "STROKED")); // nobody feeds or strokes it here
            for (int frame = 0; frame < 15 * 60 * 60 && !seen.containsAll(wanted); frame++) {
                String state = game.animalState(kind);
                seen.add(state);
                assertEquals(sheet + ANIMATIONS.get(state), game.animalAnimation(kind), kind + " " + state);
                game.frames(1);
            }
            assertTrue(seen.containsAll(wanted), kind + " saw only " + seen);
            assertFalse(game.crashed());
        }
    }

    /** Level 1 has a chicken and a chick at its bottom right (cells count rows from the bottom). */
    @Test
    void levelOneHasAChickenAndAChick() {
        GameDriver game = GameDriver.start(GameDriver.defaultMap());
        assertEquals(new Cell(17, 2), game.cellOf(Kind.CHICKEN));
        assertEquals(new Cell(19, 2), game.cellOf(Kind.CHICKEN_BABY));
        game.seconds(60);
        assertFalse(game.crashed());
    }

    private static void waitFor(GameDriver game, Kind kind, String state, int maxSeconds) {
        for (int frame = 0; frame < maxSeconds * 60 && !state.equals(game.animalState(kind)); frame++) {
            game.frames(1);
        }
        assertEquals(state, game.animalState(kind));
    }

    private static void waitForStrokable(GameDriver game) {
        for (int frame = 0; frame < 30 * 60 && !game.buttonAlphas("STROKE").equals(List.of(1f)); frame++) {
            game.frames(1);
        }
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"));
    }
}
