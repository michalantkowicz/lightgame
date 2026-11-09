package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * The board built from a map: tile cells, positions, what they draw, accessibility, neighbours, objects. One cell of
 * each kind: grass, water, blocked (x), wall (#), no tile (space), and every object.
 */
class BoardLayoutTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~~",
            "~P.*D~",
            "~x# C~",
            "~.FM.~",
            "~~~~~~")
            .with('F', "SPRITE", Map.of("textureName", "sprite_table"));

    @Test
    void boardMatchesMap() {
        GameDriver game = GameDriver.start(MAP.write("board-layout"));
        Golden.assertMatches("board-layout", game.describeBoard());
    }
}
