package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** The board built from map.tmx: tile cells, positions, backgrounds, accessibility, neighbours, objects. */
class BoardLayoutTest {
    @Test
    void boardMatchesMap() {
        GameDriver game = GameDriver.start();
        Golden.assertMatches("board-layout", game.describeBoard());
    }
}
