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

    /** The rabbit between a chicken and a chick, neither of which can walk away. */
    private static final TestMap BETWEEN = TestMap.of(
            "~~~~~~~",
            "~~HPB~~",
            "~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .with('B', "CHICKEN_BABY", Map.of());

    /** The same, but the chicken can wander off to the right (the chick can't). */
    private static final TestMap WANDERING = TestMap.of(
            "~~~~~~~~",
            "~BPH...~",
            "~~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .with('B', "CHICKEN_BABY", Map.of());

    /** The rabbit right next to a chicken and a chick on open grass. */
    private static final TestMap CLOSE = TestMap.of(
            "~~~~~~~~~",
            "~.......~",
            "~.......~",
            "~..HP...~",
            "~....B..~",
            "~.......~",
            "~~~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .with('B', "CHICKEN_BABY", Map.of());

    /** A small field, all on screen: the rabbit and a chick. */
    private static final TestMap YARD = TestMap.of(
            "~~~~~~~",
            "~P....~",
            "~.....~",
            "~...B.~",
            "~.....~",
            "~~~~~~~")
            .with('B', "CHICKEN_BABY", Map.of());

    /**
     * The chick's only cell it is used from ({@code a}, left of it) is shut in between the chicken and the chick; the
     * cell above the chick can be reached (as in level 1's bottom right corner).
     */
    private static final TestMap CORNER = TestMap.of(
            "~~~~~~~",
            "~P....~",
            "~~~~~.~",
            "~~~HaB~",
            "~~~~~~~")
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
            Map.entry("BEING_STROKED", "idle"),
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
        assertTrue(pecks.getOrDefault(Kind.CHICKEN, 0) >= 15, "the chicken pecks often: " + pecks);
        assertTrue(pecks.getOrDefault(Kind.CHICKEN_BABY, 0) >= 15, "the chick pecks often: " + pecks);
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

    /**
     * Was a bug: with two animals next to the rabbit only the first one found got buttons, so after feeding one the
     * other, still wishing, could not be fed. Now the button with the corn comes over the other one.
     */
    @Test
    void bothWishingNeighboursCanBeFed() {
        GameDriver game = GameDriver.start(BETWEEN.write("chicken-between"));
        game.putIntoBackpack("corn");
        game.putIntoBackpack("corn");
        for (int frame = 0; frame < 30 * 60 && !("WISHING".equals(game.animalState(Kind.CHICKEN))
                && "WISHING".equals(game.animalState(Kind.CHICKEN_BABY))); frame++) {
            game.frames(1);
        }
        game.seconds(0.5f);
        Kind first = game.buttonOver("GIVE");
        Kind other = first == Kind.CHICKEN ? Kind.CHICKEN_BABY : Kind.CHICKEN;
        game.clickButton("GIVE");
        assertEquals("EATING", game.animalState(first), "the one under the button eats");
        assertEquals("WISHING", game.animalState(other));
        for (int frame = 0; frame < 3 * 60 && !(other == game.buttonOver("GIVE")
                && game.buttonAlphas("GIVE").equals(List.of(1f))); frame++) {
            game.frames(1); // the first button fades out, the next one fades in
        }
        assertEquals(other, game.buttonOver("GIVE"), "then the button with the corn is over the other one");
        assertEquals(List.of(1f), game.buttonAlphas("GIVE"));
        game.clickButton("GIVE");
        assertEquals("EATING", game.animalState(other), "and it eats too");
        assertFalse(game.crashed());
    }

    /** The rabbit between a cow that wants only carrots and a chicken (corn). */
    private static final TestMap COW_AND_CHICKEN = TestMap.of(
            "~~~~~~~",
            "~~KPH~~",
            "~~~~~~~")
            .with('K', "COW", Map.of("favouriteDish", "CARROT"))
            .with('H', "CHICKEN", Map.of());

    /**
     * Found by a test agent: with the button over one wishing animal, getting what the other one wants didn't move the
     * button there. Now it moves to the one the rabbit can feed.
     */
    @Test
    void theGiveButtonGoesToTheAnimalTheRabbitCanFeed() {
        GameDriver game = GameDriver.start(COW_AND_CHICKEN.write("cow-and-chicken"));
        for (int frame = 0; frame < 90 * 60 && !("WISHING".equals(game.animalState(Kind.COW))
                && "WISHING".equals(game.animalState(Kind.CHICKEN))); frame++) {
            game.frames(1);
        }
        game.seconds(1);
        Kind first = game.buttonOver("GIVE");
        Kind other = first == Kind.COW ? Kind.CHICKEN : Kind.COW;
        game.putIntoBackpack(other == Kind.COW ? "carrot" : "corn");
        for (int frame = 0; frame < 3 * 60 && !(other == game.buttonOver("GIVE")
                && game.buttonAlphas("GIVE").equals(List.of(1f))); frame++) {
            game.frames(1);
        }
        assertEquals(other, game.buttonOver("GIVE"), "the button went to the one it can feed");
        assertEquals("hidden", game.animalWish(other));
        assertEquals(first == Kind.COW ? "carrot" : "corn", game.animalWish(first), "the other one's bubble is back");
        game.clickButton("GIVE");
        assertEquals("EATING", game.animalState(other), "and eats");
        assertFalse(game.crashed());
    }

    /**
     * Was a bug: a button stayed over the animal it came for even after that one walked away, while tapping it
     * stroked another one next to the rabbit. A fully shown button is always over an animal next to the rabbit.
     */
    @Test
    void aButtonStaysWithItsAnimal() {
        GameDriver game = GameDriver.start(WANDERING.write("chicken-wandering"));
        int checked = 0;
        Map<String, Integer> awayFor = new HashMap<>();
        for (int frame = 0; frame < 3 * 60 * 60; frame++) {
            game.frames(1);
            for (String name : List.of("STROKE", "GIVE")) {
                Kind over = game.buttonOver(name);
                if (over == null || !game.buttonAlphas(name).equals(List.of(1f))) {
                    awayFor.put(name, 0);
                    continue;
                }
                checked++;
                Cell cell = game.cellOf(over), player = game.playerCell();
                boolean next = Math.abs(cell.col() - player.col()) + Math.abs(cell.row() - player.row()) == 1;
                int away = next ? 0 : awayFor.getOrDefault(name, 0) + 1;
                awayFor.put(name, away);
                // the frame its animal steps away the button is still whole; it fades from the next one
                assertTrue(away <= 2, name + " still over the " + over + " at " + cell
                        + ", not next to the rabbit at " + player + ", for " + away + " frames");
            }
        }
        assertTrue(checked > 60 * 60, "buttons were shown for a while: " + checked + " frames");
        assertFalse(game.crashed());
    }

    /** With the rabbit right next to them (diagonally too) they peck and sit but never walk away. */
    @Test
    void theyDontWalkAwayFromTheRabbitNextToThem() {
        GameDriver game = GameDriver.start(CLOSE.write("chicken-close"));
        Cell chicken = game.cellOf(Kind.CHICKEN), chick = game.cellOf(Kind.CHICKEN_BABY);
        Set<String> states = new HashSet<>();
        for (int frame = 0; frame < 3 * 60 * 60; frame++) {
            game.frames(1);
            states.add(game.animalState(Kind.CHICKEN));
            assertEquals(chicken, game.cellOf(Kind.CHICKEN), "the chicken stays");
            assertEquals(chick, game.cellOf(Kind.CHICKEN_BABY), "the chick stays (diagonally next to the rabbit)");
        }
        assertTrue(states.contains("PECKING"), "it pecks instead: " + states);
        assertFalse(game.crashed());
    }

    /**
     * Tapped, the chick (the most restless) waits for the rabbit: it doesn't walk away while the rabbit comes, and the
     * rabbit ends next to it. Over and over, from wherever the rabbit was.
     */
    @Test
    void aChickWaitsForTheRabbitComingToIt() {
        GameDriver game = GameDriver.start(YARD.write("chick-yard"));
        int rounds = 0;
        for (int round = 0; round < 20; round++) {
            Cell chick = game.cellOf(Kind.CHICKEN_BABY);
            Cell away = farthestVisibleFreeCell(game, chick);
            if (away != null) {
                game.click(away); // the rabbit as far from the chick as it can first
            }
            game.seconds(4);
            game.click(Kind.CHICKEN_BABY);
            game.frames(2);
            Cell waiting = game.cellOf(Kind.CHICKEN_BABY);
            if (game.playerAnimation().startsWith("rabbit/walk")) {
                rounds++;
            }
            for (int frame = 0; frame < 10 * 60 && game.playerAnimation().startsWith("rabbit/walk"); frame++) {
                game.frames(1);
                assertEquals(waiting, game.cellOf(Kind.CHICKEN_BABY), "round " + round + ": it doesn't walk away");
            }
            assertTrue(game.usedFromNeighbours(waiting).contains(game.playerCell()),
                    "round " + round + ": the rabbit reached it");
        }
        assertTrue(rounds >= 15, "the rabbit walked to it in most rounds: " + rounds);
        assertFalse(game.crashed());
    }

    private static Cell farthestVisibleFreeCell(GameDriver game, Cell from) {
        Cell farthest = null;
        int distance = 0;
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                Cell cell = new Cell(col, row);
                int d = Math.abs(col - from.col()) + Math.abs(row - from.row());
                if (d > distance && game.isVisible(cell) && game.isAccessible(cell)) {
                    farthest = cell;
                    distance = d;
                }
            }
        }
        return farthest;
    }

    /** After a step it keeps looking the way it went (its sheet looks right). */
    @Test
    void aChickenKeepsLookingTheWayItWent() {
        GameDriver game = GameDriver.start(FARM.write("chicken-farm"));
        Set<String> seen = new HashSet<>();
        Cell before = game.cellOf(Kind.CHICKEN);
        for (int frame = 0; frame < 5 * 60 * 60 && seen.size() < 2; frame++) {
            game.frames(1);
            Cell now = game.cellOf(Kind.CHICKEN);
            if (now.col() != before.col()) {
                boolean left = now.col() < before.col();
                assertEquals(left, game.animalLooksLeft(Kind.CHICKEN), "walking " + (left ? "left" : "right"));
                for (int wait = 0; wait < 3 * 60 && !"STANDING".equals(game.animalState(Kind.CHICKEN)); wait++) {
                    game.frames(1);
                }
                if (game.cellOf(Kind.CHICKEN).equals(now)) {
                    assertEquals(left, game.animalLooksLeft(Kind.CHICKEN), "and standing after it");
                    seen.add(left ? "left" : "right");
                }
            }
            before = game.cellOf(Kind.CHICKEN);
        }
        assertEquals(Set.of("left", "right"), seen);
        assertFalse(game.crashed());
    }

    /**
     * Was a bug: a stroke tapped just before the animal started something else (walking, sitting down) was never
     * played. Now it stands still from the tap until its hearts: every tap is a stroke.
     */
    @Test
    void everyStrokeIsPlayed() {
        GameDriver game = GameDriver.start(CHICK_PEN.write("chick-pen"));
        int taps = 0;
        for (int frame = 0; frame < 4 * 60 * 60; frame++) {
            game.frames(1);
            if (!game.buttonAlphas("STROKE").equals(List.of(1f))) {
                continue;
            }
            game.clickButton("STROKE");
            taps++;
            assertEquals("BEING_STROKED", game.animalState(Kind.CHICKEN_BABY), "tap " + taps + ": it stands still");
            for (int wait = 0; wait < 60 && !"STROKED".equals(game.animalState(Kind.CHICKEN_BABY)); wait++) {
                game.frames(1);
            }
            assertEquals("STROKED", game.animalState(Kind.CHICKEN_BABY), "tap " + taps + ": the stroke is played");
            game.seconds(0.2f + (taps % 7) * 0.4f); // tap again at different moments of its day
        }
        assertTrue(taps >= 10, "taps (one at most every 20 s): " + taps);
        assertFalse(game.crashed());
    }

    /**
     * Was a bug (found by the owner): with no way to the cells it is used from, tapping the chick from far away did
     * nothing. Now the rabbit walks as close as it can get: next to it, above it.
     */
    @Test
    void tappingAChickNobodyCanGetToWalksAsCloseAsPossible() {
        GameDriver game = GameDriver.start(CORNER.write("chick-corner"));
        game.resize(1600, 1200); // a big window: the chick is on screen with the rabbit far away
        game.frames(1);
        game.click(Kind.CHICKEN_BABY);
        game.frames(6);
        assertTrue(game.playerAnimation().startsWith("rabbit/walk"), "the rabbit sets off");
        game.seconds(5);
        assertEquals(new Cell(5, 2), game.playerCell(), "to the cell above the chick");
        assertEquals(new Cell(5, 1), game.cellOf(Kind.CHICKEN_BABY), "which waited for it");
        assertFalse(game.crashed());
    }

    @Test
    void strokingAChick() {
        GameDriver game = GameDriver.start(CHICK_PEN.write("chick-pen"));
        game.seconds(0.5f);
        waitForStrokable(game);
        game.clickButton("STROKE");
        assertEquals("BEING_STROKED", game.animalState(Kind.CHICKEN_BABY), "it stands still while the hand fades");
        for (int frame = 0; frame < 60 && !"STROKED".equals(game.animalState(Kind.CHICKEN_BABY)); frame++) {
            game.frames(1);
        }
        game.frames(2);
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "the hand is gone: nothing covers the hearts");
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
            wanted.removeAll(Set.of("EATING", "HAPPY", "BEING_STROKED", "STROKED")); // nobody feeds or strokes it here
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
