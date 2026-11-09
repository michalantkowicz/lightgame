package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * What the board draws: every tile's layers first, then the furniture centred on its tile (with its offset).
 * (Was bug B2: furniture blocked its tiles but was never drawn.)
 */
class BoardDrawingTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~",
            "~PF.~",
            "~.G.~",
            "~~~~~")
            .with('F', "SPRITE", Map.of("textureName", "sprite_fireplace", "textureOffset", "0,10"))
            .with('G', "SPRITE", Map.of("textureName", "sprite_table"));
    @Test
    void backgroundsThenFurniture() {
        GameDriver game = GameDriver.start(MAP.write("board-drawing"));
        Golden.assertMatches("board-drawing", game.describeBoardDrawing());
    }
}
