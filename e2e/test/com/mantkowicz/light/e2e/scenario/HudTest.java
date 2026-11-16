package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HUD's bottom row and the settings gear (docs/hud-design.md, step 1): the face in the bottom centre, the two backpack
 * slots on its left showing what the player carries, the stamina bar under the face, the hint button on its right (what
 * there is to do is on buttons over the things on the map), the gear at the top right. The gear opens the settings only after a 3-second hold; letting go earlier empties its circle. Taps on
 * the bottom row don't walk the player.
 */
class HudTest {
    /** Grass all around the rabbit, also far below it (behind the HUD's bottom row). */
    private static final TestMap FIELD = TestMap.of(
            "~~~~~~~~~~~",
            "~.........~",
            "~.........~",
            "~....P....~",
            "~.........~",
            "~.........~",
            "~.........~",
            "~.........~",
            "~.........~",
            "~.........~",
            "~~~~~~~~~~~");

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

    /** Bug: with the settings open, a tap on the map around the window walked the player there. */
    @Test
    void tapsOnTheMapDontWalkWhileTheSettingsAreOpen() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        Cell start = game.playerCell();
        game.pressGear();
        game.seconds(3.1f);
        game.release();
        Cell elsewhere = new Cell(1, 4);
        game.click(elsewhere);
        game.seconds(2);
        assertEquals(start, game.playerCell(), "the dimmed screen takes the tap");

        game.closeSettings();
        game.click(elsewhere);
        game.seconds(3);
        assertEquals(elsewhere, game.playerCell(), "closed: the map walks again");
        assertFalse(game.crashed());
    }

    @Test
    void theRowSitsInTheBottomCentreAndTheGearTopRight() {
        String hud = GameDriver.start(MAP.write("hud")).describeHud();
        assertTrue(hud.contains("face: teemo at=(308.0,74.0) size=184.0x184.0"), hud); // (800 - 184) / 2 = 308
        assertTrue(hud.contains("gear: at=(664.0,1136.0) size=88.0x96.0"), hud); // 48 px from the top-right corner (room for its circle)
    }

    @Test
    void theBackpackShowsWhatThePlayerCarries() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertTrue(game.describeHud().contains("backpack: [-, -]"), game.describeHud());
        game.click(MAP.cell('C'));
        game.seconds(3);
        game.clickButton("COLLECT"); // the crop jumps out of the chest
        game.seconds(1.5f);
        game.click(game.itemCell());
        game.seconds(2);
        assertTrue(game.describeHud().matches("(?s).*backpack: \\[(Corn|Carrot|Tomato|Pumpkin|Cucumber), -].*"),
                game.describeHud());
        game.tapBackpackSlot(0); // and out again
        game.frames(2);
        assertTrue(game.describeHud().contains("backpack: [-, -]"), game.describeHud());
    }

    @Test
    void theActionButtonAppearsOverTheChest() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        assertFalse(game.snapshot("start").contains("action button"), "nothing to do yet");
        game.click(MAP.cell('C'));
        game.seconds(3);
        String next = game.snapshot("next to the chest");
        // the chest's cell (3,4) is 384..512 x 512..640 on the map, the camera's middle (448,320) is the screen's (400,640):
        // centred over the chest (400 - 90 / 2 = 355), 16 px over its top (640 + 16 - 320 + 640 = 976)
        assertTrue(next.contains("action button on ui stage: COLLECT (a picture) at=(355.0,976.0) alpha=1.0"), next);
        assertTrue(game.describeHud().contains("tool button: hint at=(628.0,74.0) size=108.0x120.0"),
                "the tool button (the hint) stays right of the face");
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

    /**
     * A tap on the face (or the stamina bar under it) doesn't go unanswered: the face plays a gesture once, picked at
     * random, never the same one twice in a row.
     */
    @Test
    void tappingTheFacePlaysAGesture() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        List<String> gestures = List.of("TEEMO_HAPPY", "TEEMO_CHEER", "TEEMO_EAR", "TEEMO_BLINK");
        Set<String> played = new HashSet<>();
        String last = "idle";
        for (int tap = 0; tap < 12; tap++) {
            if (tap % 2 == 0) {
                game.tapFace();
            } else {
                game.tapStaminaBar();
            }
            game.frames(2);
            String now = game.faceReaction();
            assertTrue(gestures.contains(now), "a gesture: " + now);
            assertNotEquals(last, now, "not the same twice in a row");
            played.add(now);
            last = now;
        }
        assertTrue(played.size() > 2, "picked at random: " + played);
        game.seconds(3);
        assertEquals("idle", game.faceReaction(), "once, then back to idle");
        assertFalse(game.crashed());
    }

    /** A tap doesn't cut short what the face is busy with (e.g. sweating over a weed). */
    @Test
    void tappingTheFaceDoesntInterruptAReaction() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        game.faceReacts("TEEMO_SWEAT", 3f);
        game.tapFace();
        game.frames(2);
        assertEquals("TEEMO_SWEAT", game.faceReaction());
        assertFalse(game.crashed());
    }

    /**
     * Was a bug (found by the owner: a chick near the bottom of the screen couldn't be tapped): the bottom row caught
     * every tap inside its whole box, also between its pieces where the map shows. Now a tap there reaches the map.
     */
    @Test
    void aTapBetweenTheHudsPiecesReachesTheMap() {
        GameDriver game = GameDriver.start(FIELD.write("hud-field"));
        Cell start = game.playerCell();
        game.tapBetweenBackpackAndFace();
        game.seconds(3);
        assertTrue(!start.equals(game.playerCell()), "the rabbit walked to the cell tapped there");
        assertFalse(game.crashed());
    }

    @Test
    void theHudKeepsToTheScreenEdgesAfterAResize() {
        GameDriver game = GameDriver.start(MAP.write("hud"));
        game.resize(500, 640); // wider: the UI's world gets wider
        game.frames(1);
        String hud = game.describeHud();
        assertTrue(hud.contains("face: teemo at=(408.0,74.0)"), hud); // (1000 - 184) / 2: still centred
        assertTrue(hud.contains("gear: at=(864.0,1136.0)"), hud); // still 48 px from the top-right corner
        assertFalse(game.crashed());
    }
}
