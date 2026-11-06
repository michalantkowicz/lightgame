package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/**
 * S15: walking onto the chest. KNOWN BUG B1 (docs/refactoring-plan.md): the game crashes because the COLLECT
 * button is added to the menu stage. The golden records the crash on purpose; it is re-recorded when B1 is fixed.
 */
class CollectTest {
    private static final Cell CHEST = new Cell(10, 24);

    @Test
    void walkingOntoChest() {
        GameDriver game = GameDriver.start();
        game.click(CHEST);
        game.seconds(2);
        game.snapshot("on chest");
        Golden.assertMatches("collect", game.timeline());
    }
}
