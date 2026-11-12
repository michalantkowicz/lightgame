package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HUD's bottom row and the settings gear (docs/hud-design.md, step 1): the face in the bottom centre, the backpack
 * slot on its left showing what the player carries, the day picture on its right, apples under the face, the gear at
 * the top right. The gear opens the settings only after a 3-second hold; letting go earlier empties its ring. Taps on
 * the bottom row don't walk the player.
 */
class HudTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..C..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~");

    @Test
    void bottomRowAndGear() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        StringBuilder story = new StringBuilder("## at the start\n").append(game.describeHud());

        game.pressGear();
        game.seconds(2);
        story.append("## gear held for 2 s\n").append(game.describeHud());
        game.release();
        game.seconds(1);
        story.append("## let go: the ring empties, nothing opens\n").append(game.describeHud());

        game.pressGear();
        game.seconds(3.1f);
        game.release();
        story.append("## gear held for 3 s: the settings open\n").append(game.describeHud());
        game.closeSettings();
        story.append("## check mark: closed\n").append(game.describeHud());

        Golden.assertMatches("hud", story.toString());
        assertFalse(game.crashed());
    }

    @Test
    void theRowSitsInTheBottomCentreAndTheGearTopRight() {
        String hud = GameDriver.start(MAP.write("hud")).describeHud();
        assertTrue(hud.contains("face: rabbit/idle_down at=(312.0,74.0) size=176.0x184.0"), hud); // (800 - 176) / 2 = 312
        assertTrue(hud.contains("gear: at=(668.0,1140.0) size=88.0x96.0"), hud);                  // 44 px from the top-right corner (room for its ring)
    }

    @Test
    void theBackpackShowsWhatThePlayerCarries() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertTrue(game.describeHud().contains("backpack: [-]"), game.describeHud());
        game.click(MAP.cell('C'));
        game.seconds(3);
        game.clickButton("COLLECT");
        game.clickChestSlot(0);
        game.clickPlayerSlot(0);
        game.clickButton("OK");
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [Lighter]"), game.describeHud());
    }

    @Test
    void tappingTheFaceDoesntWalk() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        Cell start = game.playerCell();
        game.tapFace();
        game.seconds(2);
        assertEquals(start, game.playerCell(), "the face is in front of the map: the tap stays in the HUD");
        assertFalse(game.crashed());
    }

    @Test
    void theHudKeepsToTheScreenEdgesAfterAResize() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        game.resize(500, 640); // wider: the UI's world gets wider
        game.frames(1);
        String hud = game.describeHud();
        assertTrue(hud.contains("face: rabbit/idle_down at=(412.0,74.0)"), hud); // (1000 - 176) / 2: still centred
        assertTrue(hud.contains("gear: at=(868.0,1140.0)"), hud);                 // still 44 px from the top-right corner
        assertFalse(game.crashed());
    }
}
