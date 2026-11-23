package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chicken eats a crop lying on the ground (one the player didn't pick up): up to 6 tiles away it notices it and goes
 * for it slowly, a step every 5 s, never onto the crop's tile; beside it (left or right of it, never above or below) it
 * turns to it and pecks at it, the crop pulsing; after 10 s the crop is
 * eaten. Then, or when the player picked it up first, it goes back where it started. It never stands in the player's
 * way: when its next step would leave the player no way to the crop, it gives that crop up and goes back.
 */
class ChickenEatsCropsTest {
    private static final Set<String> HUNTING = Set.of("GOING_FOR_CROP", "STEPPING_TO_CROP", "PECKING_CROP");

    /** The chicken, a crop 4 tiles right of it, the player out of the way. */
    private static final TestMap RIGHT = TestMap.of(
            "~~~~~~~~~~~~",
            "~..........~",
            "~.H...a....~",
            "~..........~",
            "~.........P~",
            "~~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The chicken, a crop 4 tiles right of it, the player close enough to come and pick it up. */
    private static final TestMap PICK_UP = TestMap.of(
            "~~~~~~~~~~",
            "~........~",
            "~H...a...~",
            "~........~",
            "~....P...~",
            "~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The player right next to the chicken, a crop further on. */
    private static final TestMap NEXT_TO_IT = TestMap.of(
            "~~~~~~~~~",
            "~.......~",
            "~PH...a.~",
            "~.......~",
            "~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The crop left of the chicken. */
    private static final TestMap LEFT = TestMap.of(
            "~~~~~~~~~~~~",
            "~..........~",
            "~.a...H....~",
            "~..........~",
            "~.........P~",
            "~~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The crop below the chicken: it goes round to its side. */
    private static final TestMap BELOW = TestMap.of(
            "~~~~~~~~~",
            "~...H...~",
            "~.......~",
            "~.......~",
            "~...a...~",
            "~......P~",
            "~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /**
     * The crop at the end of a corridor, the player at its other end: the only tile next to the crop is in the
     * corridor, so a chicken standing there would shut the player out.
     */
    private static final TestMap CORRIDOR = TestMap.of(
            "~~~~~~~~",
            "~~~~H~~~",
            "~~~~.~~~",
            "~P...a~~",
            "~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** The chicken already beside the crop, in the corridor between it and the player (found by a play-tester). */
    private static final TestMap IN_THE_WAY = TestMap.of(
            "~~~~~~~",
            "~P.Ha~~",
            "~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /**
     * The chicken's way to the crop 'a' goes through 'g', the only way to the crop 'b' (which no chicken can peck at: no
     * tile left or right of it). Found by a play-tester on level 1, where it waited 5 s in the gap between two fields.
     */
    private static final TestMap GAP = TestMap.of(
            "~~~~~~~",
            "~H~~~~~",
            "~g.a.P~",
            "~b~~~~~",
            "~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /**
     * The crop 3 tiles from the chicken, but its only free side is reached round a long wall of water (22 steps): too
     * far, the chicken doesn't go (found by a play-tester on level 1: a 110 s detour). The player is out of the way.
     */
    private static final TestMap DETOUR = TestMap.of(
            "~~~~~~~~~~~~",
            "~~~~~~~~~~.~",
            "~H.~a.~~~~.~",
            "~.~~~.~~~~.~",
            "~.~P~......~",
            "~.~~~~~~~~.~",
            "~..........~",
            "~~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /**
     * The chicken's home 'h' is the only way to the left room; it pecks at the tomato 'c' in the gate 'g'. A carrot
     * dropped in the left room meanwhile: going back home would shut the player out of it (found by a play-tester).
     */
    private static final TestMap GATE = TestMap.of(
            "~~~~~~~~~~",
            "~a.~~~...~",
            "~..hg.c.P~",
            "~..~~~...~",
            "~~~~~~~~~~")
            .with('h', "CHICKEN", Map.of());

    /** The chicken already beside the crop, in the open. */
    private static final TestMap BESIDE = TestMap.of(
            "~~~~~~~~",
            "~......~",
            "~..Ha..~",
            "~......~",
            "~.....P~",
            "~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** Crops 6 and 7 tiles away. */
    private static final TestMap FAR = TestMap.of(
            "~~~~~~~~~~~",
            "~H......b.~",
            "~.........~",
            "~........P~",
            "~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());
    private static final TestMap NEAR = TestMap.of(
            "~~~~~~~~~~~",
            "~H.....a..~",
            "~.........~",
            "~........P~",
            "~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of());

    /** A chicken and a chick, a crop between them. */
    private static final TestMap TWO = TestMap.of(
            "~~~~~~~~~~~",
            "~.........~",
            "~.H..a..B.~",
            "~.........~",
            "~........P~",
            "~~~~~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .with('B', "CHICKEN_BABY", Map.of());

    @Test
    void itGoesSlowlyPecksAndEatsTheCropThenGoesBack() {
        GameDriver game = GameDriver.start(RIGHT.write("chicken-crop-right"));
        Cell crop = RIGHT.cell('a');
        game.dropCrop("cucumber", crop);
        waitFor(game, "GOING_FOR_CROP", 30);
        Cell start = game.cellOf(Kind.CHICKEN);

        game.seconds(4);
        assertEquals(start, game.cellOf(Kind.CHICKEN), "not at once: it looks a while first");
        assertEquals("GOING_FOR_CROP", game.animalState(Kind.CHICKEN));

        Cell at = start;
        int framesSinceStep = 0;
        for (int frame = 0; frame < 60 * 60 && !"PECKING_CROP".equals(game.animalState(Kind.CHICKEN)); frame++) {
            game.frames(1);
            framesSinceStep++;
            Cell now = game.cellOf(Kind.CHICKEN);
            assertNotEquals(crop, now, "never on the crop's tile");
            if (!now.equals(at)) {
                assertTrue(framesSinceStep > 4 * 60 || at.equals(start), "a step every 5 s: " + framesSinceStep);
                assertTrue(game.neighbours(at).contains(now));
                at = now;
                framesSinceStep = 0;
            }
        }
        assertEquals("PECKING_CROP", game.animalState(Kind.CHICKEN));
        assertEquals(new Cell(crop.col() - 1, crop.row()), game.cellOf(Kind.CHICKEN), "next to it, on its side");
        assertFalse(game.animalLooksLeft(Kind.CHICKEN), "turned to the crop on its right");
        assertEquals("chicken/peck", game.animalAnimation(Kind.CHICKEN));
        assertEquals("pecked", game.cropOnGround());
        float biggest = 0;
        for (int frame = 0; frame < 60; frame++) {
            game.frames(1);
            biggest = Math.max(biggest, game.cropOnGroundScale());
        }
        assertTrue(biggest > 1.05f, "the crop pulses: " + biggest);

        game.seconds(9.5f);
        assertEquals(List.of(), game.itemsOnGround(), "eaten after 10 s");
        for (int frame = 0; frame < 30 * 60 && "GOING_BACK".equals(game.animalState(Kind.CHICKEN)); frame++) {
            game.frames(1);
        }
        assertEquals(start, game.cellOf(Kind.CHICKEN), "back where it started");
        assertFalse(game.crashed());
    }

    @Test
    void rightOfTheCropItLooksLeft() {
        GameDriver game = GameDriver.start(LEFT.write("chicken-crop-left"));
        game.dropCrop("carrot", LEFT.cell('a'));
        waitFor(game, "PECKING_CROP", 120);
        Cell crop = LEFT.cell('a');
        assertEquals(new Cell(crop.col() + 1, crop.row()), game.cellOf(Kind.CHICKEN));
        assertTrue(game.animalLooksLeft(Kind.CHICKEN));
        assertFalse(game.crashed());
    }

    @Test
    void aboveTheCropItGoesRoundToItsSide() {
        GameDriver game = GameDriver.start(BELOW.write("chicken-crop-below"));
        Cell crop = BELOW.cell('a');
        game.dropCrop("tomato", crop);
        waitFor(game, "PECKING_CROP", 120);
        Cell chicken = game.cellOf(Kind.CHICKEN);
        assertEquals(crop.row(), chicken.row(), "beside it, not above it");
        assertEquals(1, Math.abs(crop.col() - chicken.col()));
        assertEquals(chicken.col() > crop.col(), game.animalLooksLeft(Kind.CHICKEN), "looking at it");
        assertFalse(game.crashed());
    }

    @Test
    void pickedUpFirstTheChickenGoesBack() {
        GameDriver game = GameDriver.start(PICK_UP.write("chicken-crop-pick-up"));
        Cell crop = PICK_UP.cell('a');
        game.dropCrop("cucumber", crop);
        waitFor(game, "PECKING_CROP", 120);
        game.click(crop);
        for (int frame = 0; frame < 30 * 60 && HUNTING.contains(game.animalState(Kind.CHICKEN)); frame++) {
            game.frames(1);
        }
        assertEquals(List.of(), game.itemsOnGround(), "the player has it");
        assertTrue(game.backpackItems().stream().anyMatch(item -> item != null), "in the backpack");
        assertEquals("GOING_BACK", game.animalState(Kind.CHICKEN));
        assertFalse(game.crashed());
    }

    @Test
    void itNeverShutsThePlayerOut() {
        GameDriver game = GameDriver.start(CORRIDOR.write("chicken-crop-corridor"));
        Cell crop = CORRIDOR.cell('a');
        game.dropCrop("carrot", crop);
        waitFor(game, "GOING_FOR_CROP", 30);
        boolean wentBack = false;
        for (int frame = 0; frame < 40 * 60; frame++) {
            game.frames(1);
            if (HUNTING.contains(game.animalState(Kind.CHICKEN))) { // going for it, never in the player's way
                assertNotEquals(new Cell(crop.col() - 1, crop.row()), game.cellOf(Kind.CHICKEN),
                        "never in the corridor next to the crop while it goes for it, frame " + frame);
            }
            wentBack |= "GOING_BACK".equals(game.animalState(Kind.CHICKEN));
        }
        assertTrue(wentBack, "it gave the crop up");
        assertEquals("lying", game.cropOnGround(), "the crop is still there, lying as before");
        for (int frame = 0; frame < 60 * 60; frame++) {
            game.frames(1);
            assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN)), "it doesn't go for it again");
        }
        assertFalse(game.crashed());
    }

    /** Was a bug: already beside the crop in the corridor it pecked at once, shutting the player out for 10 s. */
    @Test
    void alreadyInThePlayersWayItGivesTheCropUp() {
        GameDriver game = GameDriver.start(IN_THE_WAY.write("chicken-crop-in-the-way"));
        game.dropCrop("carrot", IN_THE_WAY.cell('a'));
        for (int frame = 0; frame < 30 * 60; frame++) {
            game.frames(1);
            assertNotEquals("PECKING_CROP", game.animalState(Kind.CHICKEN));
        }
        assertEquals("lying", game.cropOnGround(), "not eaten, not pecked");
        assertFalse(game.crashed());
    }

    /** Was a bug: it shut the player out of another crop, standing in the only way there while going for its own. */
    @Test
    void itNeverShutsThePlayerOutOfAnotherCrop() {
        GameDriver game = GameDriver.start(GAP.write("chicken-crop-gap"));
        game.dropCrop("carrot", GAP.cell('b'));
        game.dropCrop("corn", GAP.cell('a'));
        waitFor(game, "GOING_FOR_CROP", 30);
        for (int frame = 0; frame < 15 * 60; frame++) {
            game.frames(1);
            assertNotEquals(GAP.cell('g'), game.cellOf(Kind.CHICKEN), "not in the way to the carrot");
        }
        assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN)), "it gave its corn up: "
                + game.animalState(Kind.CHICKEN));
        assertEquals(2, game.itemsOnGround().size());
        assertFalse(game.crashed());
    }

    /** Was a bug: giving up (or done), it went back home into the only way to another crop, and stayed there. */
    @Test
    void goingBackItKeepsOutOfThePlayersWay() {
        GameDriver game = GameDriver.start(GATE.write("chicken-crop-gate"));
        Cell home = game.cellOf(Kind.CHICKEN);
        game.dropCrop("tomato", GATE.cell('c'));
        waitFor(game, "PECKING_CROP", 60);
        game.dropCrop("carrot", GATE.cell('a'));
        for (int frame = 0; frame < 20 * 60 && (HUNTING.contains(game.animalState(Kind.CHICKEN))
                || "GOING_BACK".equals(game.animalState(Kind.CHICKEN))); frame++) {
            game.frames(1);
        }
        assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN)), "done with the tomato");
        assertNotEquals(home, game.cellOf(Kind.CHICKEN), "not back in the only way to the carrot");
        assertFalse(game.crashed());
    }

    /**
     * Going for a crop, the chicken has its stroke button (a child tapping it gets an answer); stroked, it shows its
     * hearts, gives the crop up for good and goes back where it started.
     */
    @Test
    void strokedWhileItGoesForTheCropItGivesItUp() {
        GameDriver game = GameDriver.start(PICK_UP.write("chicken-crop-pick-up"));
        Cell start = game.cellOf(Kind.CHICKEN);
        game.dropCrop("cucumber", PICK_UP.cell('a'));
        waitFor(game, "GOING_FOR_CROP", 30);
        game.strokeAnimal(Kind.CHICKEN);
        game.frames(1);
        assertEquals("BEING_STROKED", game.animalState(Kind.CHICKEN));
        waitFor(game, "STROKED", 5);
        assertTrue(game.chickenShowsHearts(Kind.CHICKEN));
        for (int frame = 0; frame < 30 * 60 && !"STANDING".equals(game.animalState(Kind.CHICKEN)); frame++) {
            game.frames(1);
        }
        assertEquals(start, game.cellOf(Kind.CHICKEN), "back where it started");
        for (int frame = 0; frame < 30 * 60; frame++) {
            game.frames(1);
            assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN)), "it gave that crop up");
        }
        assertEquals("lying", game.cropOnGround());
        assertFalse(game.crashed());
    }

    @Test
    void goingForACropItHasItsStrokeButton() {
        GameDriver game = GameDriver.start(NEXT_TO_IT.write("chicken-crop-next-to-it"));
        game.dropCrop("carrot", NEXT_TO_IT.cell('a'));
        waitFor(game, "GOING_FOR_CROP", 30);
        game.frames(30);
        assertEquals(Kind.CHICKEN, game.buttonOver("STROKE"), "a tap on it gets an answer");
        game.clickButton("STROKE");
        game.frames(1);
        assertEquals("BEING_STROKED", game.animalState(Kind.CHICKEN));
        assertFalse(game.crashed());
    }

    /** Stroked while it pecks, it stops (the crop lies still again), shows its hearts and goes back. */
    @Test
    void strokedWhileItPecksItStops() {
        GameDriver game = GameDriver.start(RIGHT.write("chicken-crop-right"));
        game.dropCrop("cucumber", RIGHT.cell('a'));
        waitFor(game, "PECKING_CROP", 120);
        game.strokeAnimal(Kind.CHICKEN);
        game.frames(1);
        assertEquals("BEING_STROKED", game.animalState(Kind.CHICKEN));
        assertEquals("lying", game.cropOnGround(), "not pecked any more");
        assertEquals(1f, game.cropOnGroundScale());
        waitFor(game, "STROKED", 5);
        waitFor(game, "GOING_BACK", 5);
        game.seconds(12);
        assertEquals("lying", game.cropOnGround(), "not eaten");
        assertFalse(game.crashed());
    }

    /** Was a bug: a crop near by but a long way round, it walked off on a 15-step detour for almost two minutes. */
    @Test
    void aLongWayRoundItDoesntGo() {
        GameDriver game = GameDriver.start(DETOUR.write("chicken-crop-detour"));
        game.dropCrop("cucumber", DETOUR.cell('a'));
        for (int frame = 0; frame < 30 * 60; frame++) {
            game.frames(1);
            assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN)), "too far round");
        }
        assertFalse(game.crashed());
    }

    /** Was a bug: already beside the crop it pecked at once; it looks a while first, as from further away. */
    @Test
    void besideTheCropItLooksFirstToo() {
        GameDriver game = GameDriver.start(BESIDE.write("chicken-crop-beside"));
        game.dropCrop("carrot", BESIDE.cell('a'));
        waitFor(game, "GOING_FOR_CROP", 30);
        game.seconds(4.5f);
        assertEquals("GOING_FOR_CROP", game.animalState(Kind.CHICKEN), "still looking");
        game.seconds(1);
        assertEquals("PECKING_CROP", game.animalState(Kind.CHICKEN));
        assertFalse(game.crashed());
    }

    @Test
    void sixTilesAwayItNoticesTheCropSevenNot() {
        GameDriver near = GameDriver.start(NEAR.write("chicken-crop-near"));
        near.dropCrop("carrot", NEAR.cell('a'));
        near.seconds(1);
        assertEquals("GOING_FOR_CROP", near.animalState(Kind.CHICKEN), "6 tiles: noticed");

        GameDriver far = GameDriver.start(FAR.write("chicken-crop-far"));
        Cell start = far.cellOf(Kind.CHICKEN);
        far.dropCrop("carrot", FAR.cell('b'));
        for (int frame = 0; frame < 60 && far.cellOf(Kind.CHICKEN).equals(start); frame++) {
            far.frames(1);
            assertFalse(HUNTING.contains(far.animalState(Kind.CHICKEN)), "7 tiles: not noticed");
        }
        assertFalse(near.crashed() || far.crashed());
    }

    @Test
    void onlyOneChickenGoesForACrop() {
        GameDriver game = GameDriver.start(TWO.write("chicken-crop-two"));
        game.dropCrop("corn", TWO.cell('a'));
        for (int frame = 0; frame < 60 * 60; frame++) {
            game.frames(1);
            assertFalse(HUNTING.contains(game.animalState(Kind.CHICKEN))
                    && HUNTING.contains(game.animalState(Kind.CHICKEN_BABY)), "not both");
        }
        assertFalse(game.crashed());
    }

    private static void waitFor(GameDriver game, String state, int maxSeconds) {
        for (int frame = 0; frame < maxSeconds * 60 && !state.equals(game.animalState(Kind.CHICKEN)); frame++) {
            game.frames(1);
        }
        assertEquals(state, game.animalState(Kind.CHICKEN));
    }
}
