package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packed starter level (maps/level1.tmx, made by tools/assets): a grass island in water. Water is on the collision
 * layer, so nobody walks there. It's the level the game starts with. The player is the rabbit, animated by direction:
 * it walks right with "walk_right" and when it stops it turns back to the default "idle_down".
 */
class LevelTest {
    private static final String LEVEL = "maps/level1.tmx";
    private static final Cell START = new Cell(6, 5);
    private static final Cell EAST = new Cell(8, 5);
    private static final Cell WATER = new Cell(6, 1);
    private static final Cell WEST = new Cell(4, 5);
    private static final Cell NORTH = new Cell(6, 8);
    private static final Cell SOUTH = new Cell(6, 3);
    private static final Cell NORTH_EAST = new Cell(8, 7);
    private static final Cell NORTH_WEST = new Cell(4, 7);
    private static final int MAX_WALK_FRAMES = 300;
    private static final Cell COW = new Cell(4, 9);
    private static final Cell CHEST = new Cell(8, 9);

    @Test
    void theGameStartsOnThisLevel() {
        assertEquals(LEVEL, GameDriver.defaultMap());
    }

    @Test
    void loadsTheLevelWithWaterAroundTheIsland() {
        GameDriver game = GameDriver.start(LEVEL);
        assertFalse(game.crashed());
        assertEquals(24 * 16, game.cells().size(), "every cell has a tile (water included)");
        assertEquals(START, game.playerCell());
        assertTrue(game.isAccessible(START));
        assertFalse(game.isAccessible(WATER), "water is on the collision layer");

        game.click(WATER);
        game.seconds(1);
        assertEquals(START, game.playerCell(), "clicking water doesn't move the rabbit");
    }

    @Test
    void theRabbitIsAnimatedInTheDirectionItWalks() {
        GameDriver game = GameDriver.start(LEVEL);
        assertEquals("rabbit/idle_down", game.playerAnimation());

        game.click(EAST);
        game.frames(10);
        assertEquals("rabbit/walk_right", game.playerAnimation());

        game.seconds(3);
        assertEquals(EAST, game.playerCell());
        assertEquals("rabbit/idle_down", game.playerAnimation(), "back to the default pose after the walk");
        assertFalse(game.crashed());
    }

    @Test
    void theCowStandsInItsPlaceChewingAway() {
        GameDriver game = GameDriver.start(LEVEL);
        assertEquals(COW, game.cellOf(GameDriver.Kind.COW));
        assertEquals("cow_brown/idle", game.cowAnimation(), "its default animation");
        assertFalse(game.isAccessible(COW), "nobody walks through a cow");
    }

    @Test
    void theChestOpensWithTheCollectWindowAndShutsAfterIt() {
        GameDriver game = GameDriver.start(LEVEL);
        assertEquals(CHEST, game.cellOf(GameDriver.Kind.CHEST));
        assertFalse(game.isAccessible(CHEST), "the chest blocks its cell");
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "closed at the start");

        game.click(CHEST);
        game.seconds(3);
        assertTrue(game.neighbours(CHEST).contains(game.playerCell()), "clicking the chest walks next to it");

        game.clickButton("COLLECT");
        game.seconds(1);
        assertEquals("chest/open_front", game.chestAnimation(), "the lid opens with the window");

        game.clickButton("CANCEL");
        game.seconds(1);
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "and shuts when it closes");
        assertFalse(game.crashed());
    }

    @Test
    void everyDirectionHasItsWalk() {
        assertEquals(Set.of("rabbit/walk_left"), animationsWhileWalkingTo(WEST));
        assertEquals(Set.of("rabbit/walk_up"), animationsWhileWalkingTo(NORTH));
        assertEquals(Set.of("rabbit/walk_down"), animationsWhileWalkingTo(SOUTH));
    }

    @Test
    void diagonalWalksDontFlickerBetweenTwoDirections() {
        assertEquals(Set.of("rabbit/walk_right"), animationsWhileWalkingTo(NORTH_EAST));
        assertEquals(Set.of("rabbit/walk_left"), animationsWhileWalkingTo(NORTH_WEST));
    }

    @Test
    void theRabbitsFrameIsCentredOnItsTile() {
        GameDriver game = GameDriver.start(LEVEL);
        assertArrayEquals(new float[]{64, 64}, game.playerFrameCentre(), 0.001f, "the middle of the 128 px tile");
    }

    /** Every animation the rabbit shows while walking from START to the target, in the order they appear. */
    private static Set<String> animationsWhileWalkingTo(Cell target) {
        GameDriver game = GameDriver.start(LEVEL);
        game.click(target);
        Set<String> shown = new LinkedHashSet<>();
        for (int frame = 0; frame < MAX_WALK_FRAMES; frame++) {
            game.frames(1);
            if ("MOVEMENT".equals(game.playerStatus())) {
                shown.add(game.playerAnimation());
            } else if (!shown.isEmpty()) {
                break;
            }
        }
        assertEquals(target, game.playerCell(), "the rabbit gets there");
        assertFalse(game.crashed());
        return shown;
    }
}
