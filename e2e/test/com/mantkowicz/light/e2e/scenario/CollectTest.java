package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S15: collecting. Clicking the chest walks next to it (it blocks its cell) and shows its button; the button opens the
 * chest: the lid lifts and stays open, stars pop out, and the carrot inside jumps out onto a free cell next to
 * the chest, one of those closest to the player; then the button shows the closed chest: it closes the lid, and opens
 * it again (empty: nothing comes out, no stars). Stepping onto the carrot puts it into the backpack (the HUD's
 * slots); tapping its slot makes it jump out again, from the player onto a free cell next to it (no stars), and it can
 * be picked up again. With the backpack full, the item in the first slot jumps out to make room (the slot shakes) and
 * the one stepped on comes into that slot a second later; the second slot is emptied only by tapping it. An item never lands on water, on a blocked cell or on someone; with no free cell it stays where
 * it is.
 * <p>
 * The chest is used only from its left, its right and below it (no button diagonally next to it or above it, where the
 * button would cover the player); walking around it keeps one button,
 * walking away removes it; a click on the fading button does nothing; clicked while walking away, the player stops next
 * to the chest and it opens there. (Was bug B1: the game crashed when the button appeared.)
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

    /** Like {@link #MAP}, with grass far below the chest, so there are cells under the button at the bottom. */
    private static final TestMap TALL_MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..C..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~.....~",
            "~.....~",
            "~.....~",
            "~.....~",
            "~.....~",
            "~.....~",
            "~~~~~~~");

    /** Next to the chest only {@code a} is free once the player stands below it: the carrot can land nowhere else. */
    private static final TestMap ONE_FREE_CELL = TestMap.of(
            "~~~~~~",
            "~~Ca~~",
            "~~.~~~",
            "~~P~~~",
            "~~~~~~");

    /** Next to the chest only the cell below it is grass: with the player there, nothing is free. */
    private static final TestMap NO_FREE_CELL = TestMap.of(
            "~~~~~",
            "~~C~~",
            "~~.~~",
            "~~P~~",
            "~~~~~");

    @Test
    void theChestThrowsOutTheCarrotThePlayerPicksItUpAndThrowsItOutAgain() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        Cell chest = MAP.cell('C');
        game.click(chest);
        game.seconds(2);
        game.snapshot("next to the chest");

        game.clickButton("COLLECT");
        game.frames(3);
        game.note("stars: " + game.starsOnScreen() + ", chest: " + game.chestAnimation() + ", items: "
                + game.itemsOnGround());
        game.seconds(1.5f);
        game.snapshot("the carrot has landed");
        game.note("inventories: " + game.inventories());
        Cell carrot = game.itemCell();
        assertTrue(game.neighbours(chest).contains(carrot), "it lands next to the chest");
        assertTrue(game.neighbours(game.playerCell()).contains(carrot), "close to the player");
        assertEquals("close", game.chestButton(), "the chest is open: its button closes it now");

        game.click(carrot);
        game.seconds(2);
        game.snapshot("walked onto the carrot");
        game.note("inventories: " + game.inventories());
        assertTrue(game.itemsOnGround().isEmpty(), "picked up");

        Cell player = game.playerCell();
        game.tapBackpackSlot(0);
        game.frames(3);
        game.note("stars: " + game.starsOnScreen() + ", items: " + game.itemsOnGround());
        game.seconds(1);
        game.snapshot("thrown out of the backpack");
        game.note("inventories: " + game.inventories());
        assertTrue(game.neighbours(player).contains(game.itemCell()), "it lands next to the player");
        assertEquals(player, game.playerCell(), "the tap on the slot didn't walk the player");

        game.click(game.itemCell());
        game.seconds(2);
        game.snapshot("picked up again");
        game.note("inventories: " + game.inventories());
        Golden.assertMatches("collect", game.timeline());
        assertFalse(game.crashed());
    }

    @Test
    void theCarrotLandsOnlyOnAFreeCell() {
        GameDriver game = GameDriver.start(ONE_FREE_CELL.write("collect-one-free"));
        game.click(ONE_FREE_CELL.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(ONE_FREE_CELL.cell('a'), game.itemCell(), "not on water, not on the player");
    }

    @Test
    void withNoFreeCellTheCarrotStaysInTheChest() {
        GameDriver game = GameDriver.start(NO_FREE_CELL.write("collect-no-free"));
        game.click(NO_FREE_CELL.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertTrue(game.itemsOnGround().isEmpty());
        assertEquals("player=[-, -] chest=[Carrot, -]", game.inventories());
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "it still holds something: the button stays");
    }

    /** A dead end: from {@code P} the only free cell is {@code a}, and from {@code a} only {@code P}. */
    private static final TestMap DEAD_END = TestMap.of(
            "~~~~~",
            "~~a~~",
            "~~P~~",
            "~~~~~");

    @Test
    void aFullBackpackThrowsOutItsFirstItemToTakeTheNewOne() {
        GameDriver game = GameDriver.start(DEAD_END.write("collect-dead-end"));
        game.putCarrotIntoBackpack();
        game.putCarrotIntoBackpack();
        List<Object> carrots = game.backpackItems(); // [first, second]
        game.tapBackpackSlot(0); // the first one jumps onto a
        game.seconds(1);
        assertEquals(DEAD_END.cell('a'), game.itemCell());
        game.putCarrotIntoBackpack(); // a third one fills the free slot: the backpack is full again
        List<Object> full = game.backpackItems(); // [third, second]

        String hud = game.describeHud();
        game.click(DEAD_END.cell('a'));
        for (int frame = 0; frame < 120 && game.backpackItems().equals(full); frame++) {
            game.frames(1);
        }
        game.frames(1); // the HUD notices it in its next frame
        assertTrue(game.backpackSlotShaking(0), "the slot its oldest item jumped out of shakes");
        assertFalse(game.backpackSlotShaking(1));
        assertEquals(Arrays.asList(null, carrots.get(1)), game.backpackItems(),
                "first the one in the first slot (the third) goes out; the second stays where it is");
        assertEquals(List.of(carrots.get(0), full.get(0)), game.itemsOnGroundThemselves(), "the first still lies on a");
        game.seconds(0.9f);
        assertEquals(Arrays.asList(null, carrots.get(1)), game.backpackItems(), "still waiting");
        game.seconds(0.2f);
        assertEquals(Arrays.asList(carrots.get(0), carrots.get(1)), game.backpackItems(),
                "a second later it comes into the first slot");
        game.seconds(1);
        assertFalse(game.backpackSlotShaking(0));
        assertEquals(slotPlace(hud), slotPlace(game.describeHud()), "and is back in its place");
        assertEquals(DEAD_END.cell('a'), game.playerCell());
        assertEquals(List.of(full.get(0)), game.itemsOnGroundThemselves(), "the third one lies on the ground now");
        assertEquals(DEAD_END.cell('P'), game.itemCell(), "on the only free cell next to the player");
        assertFalse(game.crashed());
    }

    /** Where the HUD's first backpack slot is: "at=(x,y) size=WxH" from its line. */
    private static String slotPlace(String hud) {
        String line = hud.lines().filter(l -> l.startsWith("backpack:")).findFirst().orElseThrow();
        return line.substring(line.indexOf("at="));
    }

    @Test
    void anEmptySlotThrowsNothing() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.tapBackpackSlot(1);
        game.seconds(1);
        assertTrue(game.itemsOnGround().isEmpty());
        assertEquals(MAP.cell('P'), game.playerCell(), "nor does it walk the player");
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

    /**
     * Bug: clicking the button while it faded (on the chest's corner cell) went through it to the board below and walked
     * the player there. The fading button catches the click and does nothing: the chest stays shut, the walk goes on.
     */
    @Test
    void clickOnFadingButtonDoesNothing() {
        GameDriver game = GameDriver.start(TALL_MAP.write("collect-tall"));
        game.click(TALL_MAP.cell('C'));
        game.seconds(2);

        Cell leftOfTheChest = new Cell(2, 10);
        game.click(leftOfTheChest);
        while (game.buttonAlphas("COLLECT").get(0) > 0.7f) {
            game.frames(1);
        }
        assertEquals(new Cell(2, 9), game.playerCell(), "on the corner cell, out of reach");
        game.clickButton("COLLECT");
        game.seconds(2);
        assertEquals(leftOfTheChest, game.playerCell(), "the click didn't walk the player anywhere else");
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "still shut");
        assertTrue(game.itemsOnGround().isEmpty());
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));
    }

    /**
     * The open chest's button closes it; opened again, nothing comes out of it, and the button goes on toggling. While
     * the lid moves the button fades a little and comes back with its new picture; open, it is over the lid, higher.
     */
    @Test
    void theOpenChestClosesAndOpensAgainEmpty() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        assertEquals("open", game.chestButton());
        float closedY = y(game);
        game.clickButton("COLLECT");
        game.frames(20);
        float faded = game.buttonAlphas("COLLECT").get(0);
        assertTrue(faded > 0 && faded < 1, "it fades a little while the lid lifts: " + faded);
        assertEquals("open", game.chestButton(), "with its old picture");
        game.seconds(2);
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "and comes back");
        assertEquals("chest/open_front", game.chestAnimation());
        assertEquals("close", game.chestButton());
        assertTrue(y(game) < closedY, "higher on the screen: over the open lid");
        assertEquals(1, game.itemsOnGround().size());

        game.clickButton("COLLECT");
        game.seconds(1);
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "the lid shuts");
        assertEquals("open", game.chestButton(), "the button opens it again");
        assertEquals("player=[-, -] chest=[-, -]", game.inventories(), "it doesn't refill");

        game.clickButton("COLLECT");
        game.frames(3);
        assertEquals("chest/open_front", game.chestAnimation(), "the lid lifts");
        assertEquals(0, game.starsOnScreen(), "empty: no stars");
        game.seconds(2);
        assertEquals(1, game.itemsOnGround().size(), "and nothing more comes out");
        assertEquals("close", game.chestButton());
        assertFalse(game.crashed());
    }

    /** The top of the chest's button on the screen (screen y grows downwards). */
    private static float y(GameDriver game) {
        return Float.parseFloat(game.buttonOnScreen("COLLECT").split("[, ]")[1]);
    }

    /** A chest is used only from its left, its right and below it: diagonally next to it there is no button. */
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
        assertTrue(game.usedFromNeighbours(MAP.cell('C')).contains(game.playerCell()),
                "clicking the chest walks to its side");
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));
    }

    /** Above the chest there is no button: the button over the chest would cover the player. */
    @Test
    void noButtonAboveTheChest() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        Cell chest = MAP.cell('C');
        Cell aboveTheChest = new Cell(chest.col(), chest.row() + 1);
        game.click(aboveTheChest);
        game.seconds(3);
        assertEquals(aboveTheChest, game.playerCell());
        assertTrue(game.buttonAlphas("COLLECT").isEmpty(), "no button above the chest");

        game.click(chest);
        game.seconds(2);
        assertTrue(game.usedFromNeighbours(chest).contains(game.playerCell()), "clicking the chest walks beside it");
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"));
        assertFalse(game.crashed());
    }

    /** The button clicked while walking away (still next to the chest) opens it only once the player stops next to it. */
    @Test
    void openingWhileWalkingAwayStopsNextToTheChest() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        Cell besideTheChest = game.playerCell();

        game.click(new Cell(3, 1));
        game.frames(3);
        game.clickButton("COLLECT");
        assertTrue(game.itemsOnGround().isEmpty(), "nothing opens while the player is still walking");

        game.seconds(1);
        assertEquals(besideTheChest, game.playerCell());
        assertEquals("IDLE", game.playerStatus());
        assertEquals("chest/open_front", game.chestAnimation(), "it opens once the player stands next to the chest");
        assertEquals(1, game.itemsOnGround().size());
    }
}
