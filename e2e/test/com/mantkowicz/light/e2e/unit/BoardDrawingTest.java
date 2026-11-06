package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/**
 * What the board draws: every tile background first, then the furniture centred on its tile (with its offset).
 * (Was bug B2: furniture blocked its tiles but was never drawn.)
 */
class BoardDrawingTest {
    @Test
    void backgroundsThenFurniture() {
        GameDriver game = GameDriver.start();
        Golden.assertMatches("board-drawing", game.describeBoardDrawing());
    }
}
