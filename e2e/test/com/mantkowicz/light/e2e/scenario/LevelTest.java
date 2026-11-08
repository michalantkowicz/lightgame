package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packed starter level (maps/level1.tmx, made by tools/assets): a grass island in water. Water is on the collision
 * layer, so nobody walks there. The player is the rabbit, animated by direction: it walks right with "walk_right" and
 * when it stops it turns back to the default "idle_down".
 */
class LevelTest {
    private static final String LEVEL = "maps/level1.tmx";
    private static final Cell START = new Cell(6, 5);
    private static final Cell EAST = new Cell(8, 5);
    private static final Cell WATER = new Cell(6, 1);

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
}
