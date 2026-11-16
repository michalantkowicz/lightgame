package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The right mouse button does exactly what the left one does: it walks on the board and it presses buttons (before, a
 * right click walked but buttons ignored it). Pressing both buttons is one press: one click.
 */
class MouseButtonsTest {
    /** The rabbit starts two cells below a chest. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..C..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~");

    @Test
    void theRightButtonWalksAndPressesButtons() {
        GameDriver game = GameDriver.start(MAP.write("mouse-buttons"));
        Cell chest = MAP.cell('C');
        game.rightClick(chest);
        game.seconds(2);
        assertTrue(game.usedFromNeighbours(chest).contains(game.playerCell()), "walked next to the chest");
        assertEquals("treasure", game.chestButton());

        game.rightClickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(1, game.itemsOnGround().size(), "the chest opened: " + game.itemsOnGround());
        assertEquals("none", game.chestButton(), "empty: its button is gone");

        game.rightClickHint();
        assertEquals("TEEMO_CHEER", game.faceReaction(), "the hint button too");
        assertFalse(game.crashed());
    }

    @Test
    void bothButtonsTogetherAreOneClick() {
        GameDriver game = GameDriver.start(MAP.write("mouse-buttons"));
        game.click(MAP.cell('C'));
        game.seconds(2);
        game.clickButtonWithBothButtons("COLLECT");
        game.seconds(1.5f);
        assertEquals("chest/open_front", game.chestAnimation(), "opened once (not opened and closed again)");
        assertEquals(1, game.itemsOnGround().size());
        assertFalse(game.crashed());
    }
}
