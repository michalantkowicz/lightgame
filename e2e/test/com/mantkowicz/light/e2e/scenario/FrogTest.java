package com.mantkowicz.light.e2e.scenario;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frog (character.Frog, FrogState): about as big as a chick, it can't be stroked or fed and keeps more than two
 * tiles away from the player and the animals, hopping away (half a tile a hop), into the water rather than along the
 * land. It blocks nobody's way. Now and then, and always for the night, it goes into the water and swims there; by
 * day it comes out again after a while. A crop lying on the ground nearby it eats with its tongue, when nobody is
 * near. Water is what the map draws with its water tileset only (board.Tile#isWater).
 */
class FrogTest {
    /** Water all around, more along the bottom; the frog in the meadow, the rabbit three tiles from it. */
    private static final TestMap POND = TestMap.of(
            "~~~~~~~~~~",
            "~........~",
            "~........~",
            "~..F..P..~",
            "~........~",
            "~~~~~~~~~~",
            "~~~~~~~~~~")
            .with('F', "FROG", Map.of("colour", "green"));

    /** Like {@link #POND}, with a chick right next to the frog. */
    private static final TestMap WITH_A_CHICK = TestMap.of(
            "~~~~~~~~~~",
            "~.......P~",
            "~........~",
            "~........~",
            "~..FK....~",
            "~........~",
            "~~~~~~~~~~")
            .with('F', "FROG", Map.of())
            .with('K', "CHICKEN_BABY", Map.of());

    @AfterEach
    void dayAndNightAsInTests() {
        GameDriver.dayAndNightAsInTests();
    }

    @Test
    void waterIsWhatTheMapDrawsAsWater() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        assertTrue(game.isWater(new Cell(0, 0)));
        assertTrue(game.isWater(new Cell(4, 0)));
        assertFalse(game.isWater(POND.cell('F')));
        assertFalse(game.isWater(POND.cell('P')));
        assertFalse(game.crashed());
    }

    @Test
    void levelOneHasAGreenAndAPaleFrogByTheWater() {
        GameDriver game = GameDriver.start(GameDriver.defaultMap());
        assertEquals(List.of("frog SITTING (5,2)", "frog_pale SITTING (15,2)"), game.frogs());
        assertTrue(game.isWater(new Cell(5, 1)), "the river just below them");
        assertFalse(game.crashed());
    }

    /** The rabbit walks onto the very tile the frog sat on: the frog blocks nobody, and has hopped away. */
    @Test
    void itBlocksNobodysWay() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        Cell frog = POND.cell('F');
        game.click(frog);
        game.seconds(6);
        assertEquals(frog, game.playerCell(), "the rabbit got there");
        assertTrue(distance(game.cellOf(Kind.FROG), game.playerCell()) > 2 || game.isWater(game.cellOf(Kind.FROG)),
                "the frog keeps away (or is in the water): " + game.cellOf(Kind.FROG));
        assertFalse(game.crashed());
    }

    /**
     * Whoever comes near, it hops away from, into the water rather than along the land, jumping in with a splash. In the
     * water it is safe: it floats still a while, though the rabbit stands on the bank. It never answers a tap: no button
     * over it.
     */
    @Test
    void itKeepsAwayFromTheRabbitIntoTheWater() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        Cell frog = POND.cell('F');
        game.click(new Cell(frog.col() + 1, frog.row()));
        Set<String> states = new HashSet<>();
        int splashes = 0;
        for (int frame = 0; frame < 6 * 60 && !"SWIMMING".equals(game.frogState()); frame++) {
            states.add(game.frogState());
            game.frames(1);
            splashes = Math.max(splashes, game.frogSplashes());
        }
        assertTrue(states.contains("FLEEING"), "states: " + states);
        assertEquals("SWIMMING", game.frogState());
        assertTrue(game.isWater(game.cellOf(Kind.FROG)), "in the water: " + game.cellOf(Kind.FROG));
        assertEquals(1, splashes, "it jumped in with a splash");
        Vector2 landed = game.frogPosition();
        for (int frame = 0; frame < 4 * 60; frame++) {
            game.frames(1);
            assertTrue(game.frogPosition().epsilonEquals(landed, 0.001f), "it floats still, frame " + frame);
            assertEquals("frog/swim", game.frogAnimation());
        }
        assertTrue(game.buttonAlphas("STROKE").isEmpty() && game.buttonAlphas("GIVE").isEmpty(), "no buttons");
        assertFalse(game.crashed());
    }

    /** It keeps away from the animals too: a chick right next to it at the start, it hops away at once. */
    @Test
    void itKeepsAwayFromTheAnimals() {
        GameDriver game = GameDriver.start(WITH_A_CHICK.write("frog-chick"));
        game.seconds(4);
        Cell frog = game.cellOf(Kind.FROG);
        assertTrue(distance(frog, game.cellOf(Kind.CHICKEN_BABY)) > 2 || game.isWater(frog),
                "frog " + frog + ", chick " + game.cellOf(Kind.CHICKEN_BABY));
        assertFalse(game.crashed());
    }

    /**
     * A step (onto a tile next to it, diagonally too) is two hops: half way through the step's time it is half way,
     * hopping (and the hop's animation starts again for the second half).
     */
    @Test
    void itHopsHalfATileAtATime() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        Cell frog = POND.cell('F');
        game.click(new Cell(frog.col() + 1, frog.row()));
        Vector2 start = game.frogPosition();
        int frames = 0;
        while (game.frogPosition().epsilonEquals(start, 0.001f) && frames++ < 10 * 60) {
            game.frames(1);
        }
        assertEquals("frog/hop", game.frogAnimation());
        List<Float> moved = new ArrayList<>();
        for (int frame = 0; frame < 59; frame++) {
            moved.add(game.frogPosition().dst(start));
            game.frames(1);
        }
        float step = moved.get(58), halfWay = moved.get(28) / step; // one hop: 0.49 s
        assertTrue(step > 0.9f, "a whole step (a tile, more diagonally) after two hops: " + step);
        assertTrue(halfWay > 0.45f && halfWay < 0.55f, "half the step after one hop: " + halfWay + " " + moved);
        assertEquals("frog/hop", game.frogAnimation(), "the second hop");
        assertFalse(game.crashed());
    }

    /**
     * At night it goes into the water and floats there until the morning, though nobody comes near: gliding from tile
     * to tile now and then, never hopping on the water.
     */
    @Test
    void itSpendsTheNightInTheWater() {
        GameDriver.dayAndNight(2, 1, 20, 1, 0.25f);
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        game.seconds(12); // the night began at 3 s
        assertTrue(game.isWater(game.cellOf(Kind.FROG)), "in the water: " + game.cellOf(Kind.FROG));
        for (int frame = 0; frame < 8 * 60; frame++) {
            assertTrue(game.isWater(game.cellOf(Kind.FROG)), "all night, frame " + frame);
            assertEquals("frog/swim", game.frogAnimation(), "floating, frame " + frame);
            game.frames(1);
        }
        assertFalse(game.crashed());
    }

    /** Chased into the water by day, it comes out again (with a splash) a while after the rabbit has gone. */
    @Test
    void byDayItComesOutOfTheWaterAgain() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        Cell frog = POND.cell('F');
        game.click(new Cell(frog.col() + 1, frog.row()));
        game.seconds(8);
        assertTrue(game.isWater(game.cellOf(Kind.FROG)));
        game.click(POND.cell('P'));
        game.seconds(2); // the splash going in is gone
        boolean out = false;
        int splashes = 0;
        for (int frame = 0; frame < 60 * 60 && !out; frame++) {
            game.frames(1);
            splashes = Math.max(splashes, game.frogSplashes());
            out = "SITTING".equals(game.frogState()) && !game.isWater(game.cellOf(Kind.FROG));
        }
        assertTrue(out, "on land again: " + game.frogState() + " " + game.cellOf(Kind.FROG));
        assertEquals(1, splashes, "it jumped out with a splash");
        assertFalse(game.crashed());
    }

    /** A crop lying near it, with nobody near, it goes for and eats with its tongue. */
    @Test
    void itEatsACropLyingNearby() {
        GameDriver game = GameDriver.start(POND.write("frog-pond"));
        Cell frog = POND.cell('F');
        game.dropCrop("carrot", new Cell(frog.col() - 2, frog.row() + 1)); // away from the rabbit
        Set<String> animations = new HashSet<>();
        Set<String> states = new HashSet<>();
        for (int frame = 0; frame < 30 * 60 && !game.itemsOnGround().isEmpty(); frame++) {
            animations.add(game.frogAnimation());
            states.add(game.frogState());
            game.frames(1);
        }
        assertTrue(game.itemsOnGround().isEmpty(), "eaten: " + game.itemsOnGround());
        assertTrue(states.contains("GOING_FOR_CROP") && states.contains("EATING"), "states: " + states);
        assertTrue(animations.contains("frog/tongue"), "animations: " + animations);
        assertFalse(game.crashed());
    }

    /** How far apart two cells are, diagonally too. */
    private static int distance(Cell a, Cell b) {
        return Math.max(Math.abs(a.col() - b.col()), Math.abs(a.row() - b.row()));
    }
}
