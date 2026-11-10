package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import com.mantkowicz.light.e2e.Cell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S15: the collect flow. Clicking the chest walks next to it (it blocks its cell) and shows a COLLECT button; it
 * opens a window with the chest's and the player's slots; items are moved by clicking a full slot and then a free one;
 * OK saves, CANCEL drops the changes;
 * the chest is used from its four sides only (no button diagonally next to it); walking around it keeps one button,
 * walking away removes it; COLLECT clicked
 * while walking away stops the player next to the chest and opens the window there. Every COLLECT click opens a fresh window showing the saved inventories.
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

    /**
     * Bug: stepping to another cell next to the chest faded the button out while a new one popped up. Walking from below
     * the chest to its left side passes its corner cell (out of reach): the button may dim there, but it is always the same
     * single button and it is fully back on the side cell.
     */
    @Test
    void walkingAroundTheChestKeepsOneButton() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));

        Cell leftOfTheChest = new Cell(2, 4);
        game.click(leftOfTheChest);
        for (int frame = 0; frame < 120; frame++) {
            game.frames(1);
            assertEquals(1, game.buttonAlphas("COLLECT").size(), "frame " + frame);
        }
        assertEquals(leftOfTheChest, game.playerCell());
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));

        game.click(new Cell(2, 1));
        game.seconds(3);
        assertTrue(game.buttonAlphas("COLLECT").isEmpty(), "the button is gone once the player left the chest");
    }

    /** A chest is used only from its four sides: diagonally next to it there is no button. */
    @Test
    void noButtonDiagonallyNextToTheChest() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        Cell cornerOfTheChest = new Cell(2, 3);
        game.click(cornerOfTheChest);
        game.seconds(2);
        assertEquals(cornerOfTheChest, game.playerCell());
        assertTrue(game.buttonAlphas("COLLECT").isEmpty());

        game.click(MAP.cell('C'));
        game.seconds(2);
        assertTrue(game.sideNeighbours(MAP.cell('C')).contains(game.playerCell()), "clicking the chest walks to its side");
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));
    }

    /** COLLECT clicked while walking away (still next to the chest) must not open the window with the player leaving. */
    @Test
    void collectWhileWalkingAwayStopsNextToTheChest() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        Cell besideTheChest = game.playerCell();

        game.click(new Cell(3, 1));
        game.frames(3);
        game.clickButton("COLLECT");
        assertFalse(game.collectWindowOpen(), "no window while the player is still walking");

        game.seconds(1);
        assertEquals(besideTheChest, game.playerCell());
        assertEquals("IDLE", game.playerStatus());
        assertTrue(game.collectWindowOpen(), "the window opens once the player stands next to the chest");
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "one COLLECT button, never faded");
    }
}
