package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

/**
 * S15: the collect flow. Clicking the chest walks next to it (it blocks its cell) and shows a COLLECT button; it
 * opens a window with the chest's and the player's slots; items are moved by clicking a full slot and then a free one;
 * OK saves, CANCEL drops the changes;
 * walking away removes the button. Every COLLECT click opens a fresh window showing the saved inventories.
 * (Was bug B1: the game crashed when the button appeared; B7: a cancelled window was reopened with its unsaved changes.)
 */
class CollectTest {
    /** The rabbit starts two cells below the chest: clicking the chest walks one cell up, next to it. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..C..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~");

    @Test
    void collectingTheLighter() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        game.snapshot("on chest");

        game.clickButton("COLLECT");
        game.seconds(1);
        game.snapshot("window open");
        game.note("inventories: " + game.inventories());
        game.note("debug lines: " + game.uiDebugLinesShown());

        game.clickChestSlot(0);
        game.clickPlayerSlot(0);
        game.clickButton("OK");
        game.seconds(1);
        game.snapshot("after OK");
        game.note("inventories: " + game.inventories());

        game.clickButton("COLLECT");
        game.seconds(1);
        game.note("reopened window: " + game.collectWindowSlots());
        game.clickButton("CANCEL");
        game.seconds(1);

        game.click(MAP.cell('P'));
        game.seconds(3);
        game.snapshot("walked away");
        Golden.assertMatches("collect", game.timeline());
    }

    @Test
    void cancelDropsTheChanges() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.seconds(1);

        game.clickChestSlot(0);
        game.clickPlayerSlot(0);
        game.clickButton("CANCEL");
        game.seconds(1);
        game.snapshot("after CANCEL");
        game.note("inventories: " + game.inventories());

        game.clickButton("COLLECT");
        game.seconds(1);
        game.note("reopened window: " + game.collectWindowSlots());
        Golden.assertMatches("collect-cancel", game.timeline());
    }
}
