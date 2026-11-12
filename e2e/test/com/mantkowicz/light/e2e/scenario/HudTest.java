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
 * The HUD's bottom row and the settings gear (docs/hud-design.md, step 1): the face in the bottom centre, the two backpack
 * slots on its left showing what the player carries, the stamina bar under the face, the action button on its right when
 * there is something to do, the gear at the top right. The gear opens the settings only after a 3-second hold; letting go earlier empties its circle. Taps on
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
        story.append("## let go: the circle empties, nothing opens\n").append(game.describeHud());

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
        assertTrue(hud.contains("face: teemo at=(308.0,74.0) size=184.0x184.0"), hud);       // (800 - 184) / 2 = 308
        assertTrue(hud.contains("gear: at=(664.0,1136.0) size=88.0x96.0"), hud);                  // 48 px from the top-right corner (room for its circle)
    }

    @Test
    void theBackpackShowsWhatThePlayerCarries() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertTrue(game.describeHud().contains("backpack: [-, -]"), game.describeHud());
        game.click(MAP.cell('C'));
        game.seconds(3);
        game.clickButton("COLLECT"); // the carrot jumps out of the chest
        game.seconds(1.5f);
        game.click(game.itemCell());
        game.seconds(2);
        assertTrue(game.describeHud().contains("backpack: [Carrot, -]"), game.describeHud());
        game.tapBackpackSlot(0); // and out again
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -]"), game.describeHud());
    }

    @Test
    void theActionButtonAppearsRightOfTheFace() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertFalse(game.snapshot("start").contains("action button"), "nothing to do yet");
        game.click(MAP.cell('C'));
        game.seconds(3);
        String next = game.snapshot("next to the chest");
        // its right edge 64 px from the screen's edge, like the slots' left edge (800 - 64 - 108 = 628), its bottom on
        // the face's bottom
        assertTrue(next.contains("action button on ui stage: COLLECT (a picture) at=(628.0,74.0) alpha=1.0"), next);
        assertTrue(game.describeHud().contains("hint: at=(628.0,74.0) size=108.0x120.0"), "the action button covers the hint");
    }

    @Test
    void theHintMakesTheFaceSayPlayAround() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        Cell start = game.playerCell();
        StringBuilder story = new StringBuilder("## at the start\n").append(game.describeHud());
        game.tapHint();
        game.frames(2);
        story.append("## hint tapped: a bubble over the face\n").append(game.describeHud());
        assertEquals(start, game.playerCell(), "the tap stays in the HUD: the player doesn't walk");
        game.seconds(2.6f);
        story.append("## after 2.6 s: fading\n").append(game.describeHud());
        game.seconds(0.5f);
        story.append("## after 3.1 s: gone\n").append(game.describeHud());
        Golden.assertMatches("hud-hint", story.toString());
        assertFalse(game.crashed());
    }

    @Test
    void theBubbleIsOverTheFace() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        game.tapHint();
        game.frames(2);
        String hud = game.describeHud();
        // its middle over the face's middle (x 400), its pointer's tip 6 px over the face's top (74 + 184 + 6 = 264)
        String bubble = hud.lines().filter(line -> line.startsWith("bubble:")).findFirst().orElseThrow();
        float x = Float.parseFloat(bubble.replaceAll(".*at=\\(([0-9.]+),.*", "$1"));
        float width = Float.parseFloat(bubble.replaceAll(".*size=([0-9.]+)x.*", "$1"));
        assertEquals(400, x + width / 2, 1, bubble);
        assertTrue(bubble.contains(",264.0) size="), bubble);
    }

    @Test
    void theFaceReactsForAWhileThenGoesBackToIdle() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertEquals("idle", game.faceReaction());
        game.faceReacts("TEEMO_CHEER", 1f);
        game.seconds(0.9f);
        assertEquals("TEEMO_CHEER", game.faceReaction());
        game.seconds(0.2f);
        assertEquals("idle", game.faceReaction(), "after its second");

        game.faceReacts("TEEMO_CHEER", null); // once: 7 frames of 0.12 s
        game.seconds(0.7f);
        assertEquals("TEEMO_CHEER", game.faceReaction());
        game.seconds(0.2f);
        assertEquals("idle", game.faceReaction(), "after one round");

        game.faceReacts("TEEMO_CHEER", 1f);
        game.seconds(0.8f);
        game.faceReacts("TEEMO_EAR", 1f); // a new one replaces it and gets its own second
        game.seconds(0.8f);
        assertEquals("TEEMO_EAR", game.faceReaction());
        game.seconds(0.3f);
        assertEquals("idle", game.faceReaction());
        assertFalse(game.crashed());
    }

    @Test
    void theFaceCheersWhileItSaysTheHint() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        game.tapHint();
        game.frames(2);
        assertEquals("TEEMO_CHEER", game.faceReaction());
        game.seconds(2.6f);
        assertEquals("idle", game.faceReaction(), "back to idle once the bubble goes");
    }

    @Test
    void theStaminaBarShowsWhatIsLeft() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        StringBuilder story = new StringBuilder("## at the start: full\n").append(staminaLine(game));
        game.useStamina(0.35f);
        game.frames(1);
        story.append("## used 0.35 (hard work): 0.65 left, still happy\n").append(staminaLine(game));
        game.useStamina(0.15f);
        game.frames(1);
        story.append("## used 0.15 more: half left, neutral\n").append(staminaLine(game));
        game.useStamina(0.25f);
        game.frames(1);
        story.append("## used 0.25 more: a quarter left, sad\n").append(staminaLine(game));
        game.useStamina(0.2f);
        game.frames(1);
        story.append("## used 0.2 more: a little left still shows one segment\n").append(staminaLine(game));
        game.useStamina(1);
        game.frames(1);
        story.append("## used more than is left: empty, not below\n").append(staminaLine(game));
        game.fillStamina();
        game.frames(1);
        story.append("## filled (slept)\n").append(staminaLine(game));
        Golden.assertMatches("hud-stamina", story.toString());
    }

    private static String staminaLine(GameDriver game) {
        // the bar, and the face whose idle follows it (awake, tired, asleep)
        return game.describeHud().lines().filter(line -> line.startsWith("stamina:") || line.startsWith("face:"))
                .reduce("", (lines, line) -> lines + line + "\n");
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
        assertTrue(hud.contains("face: teemo at=(408.0,74.0)"), hud);           // (1000 - 184) / 2: still centred
        assertTrue(hud.contains("gear: at=(864.0,1136.0)"), hud);                 // still 48 px from the top-right corner
        assertFalse(game.crashed());
    }
}
