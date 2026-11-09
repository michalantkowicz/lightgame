package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A wall tile blocks light. The map has no walls, so the test puts one on w, between a (above) and b (below).
 * (Was bug B3: creating a wall threw UnsupportedOperationException.)
 */
class WallTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~..a.s~",
            "~..w..~",
            "~P.b..~",
            "~~~~~~~");
    private static final Cell ABOVE = MAP.cell('a');
    private static final Cell WALL = MAP.cell('w');
    private static final Cell BELOW = MAP.cell('b');
    private static final Cell BESIDE = MAP.cell('s');

    @Test
    void wallBlocksLight() {
        GameDriver game = GameDriver.start(MAP.write("wall"));
        assertTrue(game.lightReaches(ABOVE, BELOW), "without a wall the light reaches the cell");

        game.addWall(WALL);

        assertFalse(game.crashed(), "adding a wall must not crash");
        assertFalse(game.lightReaches(ABOVE, BELOW), "the wall casts a shadow");
        assertTrue(game.lightReaches(ABOVE, BESIDE), "the wall doesn't block other directions");
    }
}
