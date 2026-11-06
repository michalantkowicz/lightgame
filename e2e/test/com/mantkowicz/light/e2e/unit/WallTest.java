package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A wall tile blocks light. The map has no walls, so the test puts one on (10,21), between (10,20) and (10,22).
 * (Was bug B3: creating a wall threw UnsupportedOperationException.)
 */
class WallTest {
    private static final Cell ABOVE = new Cell(10, 22);
    private static final Cell WALL = new Cell(10, 21);
    private static final Cell BELOW = new Cell(10, 20);
    private static final Cell BESIDE = new Cell(12, 22);

    @Test
    void wallBlocksLight() {
        GameDriver game = GameDriver.start();
        assertTrue(game.lightReaches(ABOVE, BELOW), "without a wall the light reaches the cell");

        game.addWall(WALL);

        assertFalse(game.crashed(), "adding a wall must not crash");
        assertFalse(game.lightReaches(ABOVE, BELOW), "the wall casts a shadow");
        assertTrue(game.lightReaches(ABOVE, BESIDE), "the wall doesn't block other directions");
    }
}
