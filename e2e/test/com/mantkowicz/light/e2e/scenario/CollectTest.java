package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/**
 * S15: the collect flow. Walking onto the chest shows a COLLECT button; it opens a window with the chest's and the
 * player's slots; items are moved by clicking a full slot and then a free one; OK saves, CANCEL drops the changes;
 * walking away removes the button. Every COLLECT click opens a fresh window showing the saved inventories.
 * (Was bug B1: the game crashed when the button appeared; B7: a cancelled window was reopened with its unsaved changes.)
 */
class CollectTest {
    private static final Cell CHEST = new Cell(10, 24);
    private static final Cell START = new Cell(10, 22);

    @Test
    void collectingTheLighter() {
        GameDriver game = GameDriver.start();
        game.click(CHEST);
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

        game.click(START);
        game.seconds(3);
        game.snapshot("walked away");
        Golden.assertMatches("collect", game.timeline());
    }

    @Test
    void cancelDropsTheChanges() {
        GameDriver game = GameDriver.start();
        game.click(CHEST);
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
