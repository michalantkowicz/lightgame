package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S15: collecting. Clicking the chest walks next to it (it blocks its cell) and shows its button; the button opens the
 * chest: the lid lifts and stays open, stars pop out, and the crop inside (picked at random: corn, a carrot, a tomato,
 * a pumpkin or a cucumber) jumps out onto a free cell next to
 * the chest, one of those closest to the player; then the button shows the closed chest: it closes the lid, and opens
 * it again (empty: nothing comes out, no stars). Stepping onto the crop puts it into the backpack (the HUD's
 * slots); tapping its slot makes it jump out again, from the player onto a free cell next to it (no stars), and it can
 * be picked up again. With the backpack full nothing jumps out by itself: the item stepped on stays where it lies, and
 * standing on it the tool button widens into a swap (see {@link #aFullBackpackLeavesTheItemAndOffersASwap}). An item
 * never lands on water, on a blocked cell or on someone; with no free cell it stays where
 * it is (a chest with nowhere for its crop to land stays shut, its button shakes). A chest next to the player at
 * the start has its button at once.
 * <p>
 * The chest is used only from its left, its right and below it (no button diagonally next to it or above it, where the
 * button would cover the player); walking around it keeps one button,
 * walking away removes it; a click on the fading button does nothing; clicked while walking away, the player stops next
 * to the chest and it opens there. (Was bug B1: the game crashed when the button appeared.)
 */
class CollectTest {
    /** How a crop is listed, any of them. */
    private static final String CROP = "(Corn|Carrot|Tomato|Pumpkin|Cucumber)";

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

    /** Next to the chest only {@code a} is free once the player stands below it: the crop can land nowhere else. */
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

    /** Many chests: each holds one crop of its own, picked at random. */
    private static final TestMap CHESTS = TestMap.of(
            "~~~~~~~~~~~~~",
            "~C.C.C.C.C.C~",
            "~.....P.....~",
            "~C.C.C.C.C.C~",
            "~~~~~~~~~~~~~");

    @Test
    void everyChestHoldsACropPickedAtRandom() {
        GameDriver game = GameDriver.start(CHESTS.write("collect-chests"));
        List<String> chests = game.chestContents();
        assertEquals(12, chests.size());
        Set<String> crops = new HashSet<>();
        for (String chest : chests) {
            assertTrue(chest.matches("\\[" + CROP + ", -]"), "one crop in each: " + chests);
            crops.add(chest);
        }
        assertTrue(crops.size() > 1, "not all the same: " + chests);
        assertFalse(game.crashed());
    }

    @Test
    void theChestThrowsOutTheCropThePlayerPicksItUpAndThrowsItOutAgain() {
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
        game.snapshot("the crop has landed");
        game.note("inventories: " + game.inventories());
        Cell crop = game.itemCell();
        assertTrue(game.neighbours(chest).contains(crop), "it lands next to the chest");
        assertTrue(game.neighbours(game.playerCell()).contains(crop), "close to the player");
        assertEquals("chest/open_front", game.chestAnimation(), "it stays open, without a button");

        game.click(crop);
        game.seconds(2);
        game.snapshot("walked onto the crop");
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
    void theCropLandsOnlyOnAFreeCell() {
        GameDriver game = GameDriver.start(ONE_FREE_CELL.write("collect-one-free"));
        game.click(ONE_FREE_CELL.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(ONE_FREE_CELL.cell('a'), game.itemCell(), "not on water, not on the player");
    }

    @Test
    void withNoFreeCellTheCropStaysInTheChest() {
        GameDriver game = GameDriver.start(NO_FREE_CELL.write("collect-no-free"));
        game.click(NO_FREE_CELL.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertTrue(game.itemsOnGround().isEmpty());
        assertTrue(game.inventories().matches("player=\\[-, -, -] chest=\\[" + CROP + ", -]"), game.inventories());
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "it still holds something: the button stays");
    }

    /**
     * Bug: with nowhere for the carrot to land (the player stands on the only free cell) the chest opened, popped stars
     * and kept the carrot. It stays shut and its button shakes, like the cow's carrot button without a carrot.
     */
    @Test
    void withNoFreeCellTheChestStaysShutAndItsButtonShakes() {
        GameDriver game = GameDriver.start(NO_FREE_CELL.write("collect-no-free"));
        game.click(NO_FREE_CELL.cell('C'));
        game.seconds(2);
        game.clickButton("COLLECT");
        game.frames(3);
        assertTrue(game.actionButtonShaking("COLLECT"), "the button shakes");
        assertEquals(0, game.starsOnScreen(), "no stars");
        game.seconds(1);
        assertEquals("chest/open_front (backwards)", game.chestAnimation(), "the lid stays shut");
        assertEquals("treasure", game.chestButton(), "the button still offers to open it");
        assertFalse(game.crashed());
    }

    /** Bug: a chest next to the player at the start (right of it, or above it) had no button until the player moved. */
    @Test
    void aChestNextToThePlayerAtTheStartHasItsButton() {
        for (TestMap map : List.of(
                TestMap.of("~~~~~", "~.PC~", "~...~", "~~~~~"),
                TestMap.of("~~~~~", "~.C.~", "~.P.~", "~~~~~"),
                TestMap.of("~~~~~", "~CP.~", "~...~", "~~~~~"))) {
            GameDriver game = GameDriver.start(map.write("collect-start"));
            assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "button at the start: " + map.cell('C'));
            assertFalse(game.crashed());
        }
    }

    /** A dead end: from {@code P} the only free cell is {@code a}, and from {@code a} only {@code P}. */
    private static final TestMap DEAD_END = TestMap.of(
            "~~~~~",
            "~~a~~",
            "~~P~~",
            "~~~~~");

    /**
     * With the backpack full, the item the player walks onto stays where it lies and nothing jumps out of the backpack.
     * Standing on it, the tool button widens to the left (its right edge stays): the left slot's item, which pulses
     * (dark brown and back) in its slot and on the button together, would go for it; the item on the ground doesn't.
     */
    @Test
    void aFullBackpackLeavesTheItemAndOffersASwap() {
        GameDriver game = fullBackpackNextToATomato();
        List<Object> full = game.backpackItems();
        String roundBox = game.toolButtonBox();
        game.click(DEAD_END.cell('a'));
        game.seconds(2);
        assertEquals(DEAD_END.cell('a'), game.playerCell());
        assertEquals(full, game.backpackItems(), "nothing jumps out of the backpack");
        assertFalse(game.backpackSlotShaking(0));
        assertEquals(List.of("Tomato cell=" + DEAD_END.cell('a')), game.itemsOnGround(), "it stays where it lies");

        assertEquals("swap", game.toolButton());
        assertEquals(List.of("Carrot", "Tomato"), game.toolButtonSwap(), "the left slot's carrot would go");
        assertEquals(0, game.swapSlot());
        assertEquals("at=(528.0,65.0) size=252.0x108.0", game.toolButtonBox(),
                "wide: its right edge where it was, the middle of its face on the slots' middle");
        assertEquals("at=(672.0,54.0) size=108.0x120.0", roundBox,
                "round: the middle of its face on the slots' middle");
        Set<String> tints = new HashSet<>();
        for (int frame = 0; frame < 72; frame++) {
            tints.add(game.backpackItemTint(0));
            assertEquals("ffffffff", game.backpackItemTint(1), "the others keep their colours");
            assertEquals(game.backpackItemTint(0), game.toolButtonGoingTint(), "on the button it pulses with its slot");
            game.frames(1);
        }
        assertTrue(tints.contains("ffffffff") && tints.size() > 10, "the carrot goes dark and back: " + tints);
        assertFalse(game.crashed());
    }

    /**
     * Tapping a slot only chooses what goes (nothing moves); tapping the tool button swaps: the item from the ground goes
     * into that slot and the slot's item lies on the ground instead, so the same tap swaps them back.
     */
    @Test
    void aSlotChoosesWhatGoesAndTheToolButtonSwaps() {
        GameDriver game = fullBackpackNextToATomato();
        List<Object> full = game.backpackItems(); // [carrot, corn, pumpkin]
        Object tomato = game.itemsOnGroundThemselves().get(0);
        game.click(DEAD_END.cell('a'));
        game.seconds(2);

        game.tapBackpackSlot(1);
        game.seconds(1);
        assertEquals(1, game.swapSlot());
        assertEquals(List.of("Corn", "Tomato"), game.toolButtonSwap());
        assertEquals(full, game.backpackItems(), "nothing moved");
        assertEquals(List.of(tomato), game.itemsOnGroundThemselves(), "nothing thrown out");
        assertEquals(DEAD_END.cell('a'), game.playerCell(), "the tap didn't walk the player");

        game.tapToolButton();
        assertEquals(Arrays.asList(full.get(0), tomato, full.get(2)), game.backpackItems(),
                "the tomato in the corn's slot");
        game.seconds(1);
        assertEquals(List.of("Corn cell=" + DEAD_END.cell('a')), game.itemsOnGround(), "the corn lies there instead");
        assertEquals("swap", game.toolButton());
        assertEquals(List.of("Tomato", "Corn"), game.toolButtonSwap(), "the same tap swaps them back");
        assertEquals(1, game.swapSlot());

        game.tapToolButton();
        game.seconds(1);
        assertEquals(full, game.backpackItems());
        assertEquals(List.of("Tomato cell=" + DEAD_END.cell('a')), game.itemsOnGround());
        assertFalse(game.crashed());
    }

    /** Stepping off the item the tool button is round again and nothing pulses; the next time the left slot is first. */
    @Test
    void steppingOffEndsTheSwap() {
        GameDriver game = fullBackpackNextToATomato();
        game.click(DEAD_END.cell('a'));
        game.seconds(2);
        game.tapBackpackSlot(2);
        game.frames(2);
        assertEquals(2, game.swapSlot());

        game.click(DEAD_END.cell('P'));
        game.seconds(2);
        assertEquals("hint", game.toolButton());
        assertEquals(List.of(), game.toolButtonSwap());
        assertEquals(-1, game.swapSlot());
        assertEquals("ffffffff", game.backpackItemTint(2));
        assertEquals("ffffffff", game.toolButtonGoingTint());
        assertEquals("at=(672.0,54.0) size=108.0x120.0", game.toolButtonBox(), "round, where it was");

        game.tapBackpackSlot(2); // no swap on offer: the slot does what it always does
        game.frames(2);
        assertTrue(game.backpackSlotShaking(2), "nowhere to throw the pumpkin (the tomato lies on a): it shakes");
        game.seconds(1);

        game.click(DEAD_END.cell('a'));
        game.seconds(2);
        assertEquals(0, game.swapSlot(), "the left slot first again");
        assertFalse(game.crashed());
    }

    /** Only standing still: walking across an item with the backpack full, the tool button stays round. */
    @Test
    void noSwapWhileWalkingAcrossAnItem() {
        TestMap row = TestMap.of("~~~~~", "~Pab~", "~~~~~");
        GameDriver game = GameDriver.start(row.write("collect-row"));
        game.putIntoBackpack("carrot");
        game.putIntoBackpack("corn");
        game.putIntoBackpack("pumpkin");
        game.dropCrop("tomato", row.cell('a'));
        game.seconds(1);
        game.click(row.cell('b'));
        for (int frame = 0; frame < 90; frame++) {
            game.frames(1);
            assertEquals("hint", game.toolButton(), "frame " + frame);
        }
        assertEquals(row.cell('b'), game.playerCell());
        assertEquals(List.of("Tomato cell=" + row.cell('a')), game.itemsOnGround());
        assertFalse(game.crashed());
    }

    /** The swap comes before the scythe: on grass with an item the tool button swaps; on grass alone it cuts. */
    @Test
    void theSwapComesBeforeTheScythe() {
        TestMap meadow = TestMap.of("~~~~~", "~Pgh~", "~~~~~")
                .with('g', "GRASS", Map.of())
                .with('h', "GRASS", Map.of());
        GameDriver game = GameDriver.start(meadow.write("collect-meadow"));
        game.putIntoBackpack("carrot");
        game.putIntoBackpack("corn");
        game.putIntoBackpack("pumpkin");
        game.dropCrop("tomato", meadow.cell('g'));
        game.seconds(1);
        game.click(meadow.cell('h'));
        game.seconds(2);
        assertEquals("scythe", game.toolButton(), "grass alone: the scythe");

        game.click(meadow.cell('g'));
        game.seconds(2);
        assertEquals("swap", game.toolButton(), "grass with an item: the swap");
        game.tapToolButton();
        game.seconds(1);
        assertEquals("[Tomato, Corn, Pumpkin]", game.describeHud().lines()
                .filter(line -> line.startsWith("backpack:")).findFirst().orElseThrow().split(" at=")[0]
                .substring("backpack: ".length()));

        game.click(meadow.cell('h'));
        game.seconds(2);
        assertEquals("scythe", game.toolButton(), "off the item, the scythe again");
        assertFalse(game.crashed());
    }

    /** The rabbit next to a tomato on {@code a}, with a carrot, corn and a pumpkin in its backpack (full). */
    private static GameDriver fullBackpackNextToATomato() {
        GameDriver game = GameDriver.start(DEAD_END.write("collect-dead-end"));
        game.putIntoBackpack("carrot");
        game.putIntoBackpack("corn");
        game.putIntoBackpack("pumpkin");
        game.dropCrop("tomato", DEAD_END.cell('a'));
        game.seconds(1);
        return game;
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
     * The chest's button shows a gold star (treasure inside). Tapped, the lid lifts (the button fades a little
     * meanwhile) and stays up, what is inside jumps out, and the empty chest has no button any more: nothing to close,
     * nothing to open again.
     */
    @Test
    void theOpenedChestStaysOpenWithoutAButton() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        assertEquals("treasure", game.chestButton());
        game.clickButton("COLLECT");
        game.frames(20);
        float faded = game.buttonAlphas("COLLECT").get(0);
        assertTrue(faded > 0 && faded < 1, "it fades while the lid lifts: " + faded);
        game.seconds(2);
        assertEquals("chest/open_front", game.chestAnimation(), "the lid stays up");
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "empty: no button");
        assertEquals("none", game.chestButton());
        assertEquals(1, game.itemsOnGround().size());
        assertEquals("player=[-, -, -] chest=[-, -]", game.inventories());
        game.seconds(10);
        assertEquals("chest/open_front", game.chestAnimation(), "still open");
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "still no button");
        assertFalse(game.crashed());
    }

    /** A button fades in when it appears, as it fades out when it goes. */
    @Test
    void theButtonFadesIn() {
        GameDriver game = GameDriver.start(MAP.write("collect"));
        game.click(MAP.cell('C'));
        List<Float> first = List.of();
        for (int frame = 0; frame < 180 && first.isEmpty(); frame++) {
            game.frames(1);
            first = game.buttonAlphas("COLLECT");
        }
        assertEquals(1, first.size(), "the button appeared");
        assertTrue(first.get(0) < 0.5f, "faint at first: " + first);
        game.seconds(0.5f);
        assertEquals(List.of(1f), game.buttonAlphas("COLLECT"), "then fully there");
    }

    /** With nowhere for it to land, a tapped slot keeps its item and shakes. */
    @Test
    void aSlotWithNowhereToThrowShakes() {
        GameDriver game = GameDriver.start(TestMap.of("~~~~~", "~~P~~", "~~~~~").write("collect-island"));
        game.putCarrotIntoBackpack();
        game.frames(2);
        game.tapBackpackSlot(0);
        game.frames(2);
        assertTrue(game.backpackSlotShaking(0), "the slot shakes");
        assertTrue(game.itemsOnGround().isEmpty(), "nothing was thrown");
        assertEquals(1, game.backpackItems().stream().filter(item -> item != null).count(), "the carrot stays");
        assertFalse(game.crashed());
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
