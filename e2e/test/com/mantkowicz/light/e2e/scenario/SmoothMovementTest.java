package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player walks at one constant speed (128 px in 0.4 s), never stops between tiles, and cuts straight lines
 * where the floor allows it (here: a diagonal line three cells left and one up).
 */
class SmoothMovementTest {
    /** k is next to the chest; u is three cells up from k, on the other side of the chest. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~~~",
            "~...u...~",
            "~.......~",
            "~..C....~",
            "~..k....~",
            "~.....P.~",
            "~~~~~~~~~");
    private static final Cell START = MAP.cell('P');
    private static final Cell NEXT_TO_CHEST = MAP.cell('k');
    private static final Cell PAST_THE_CHEST = MAP.cell('u');
    private static final float PX_PER_FRAME = 128f / (0.4f * 60);

    @Test
    void constantSpeedNoStopsAndADiagonal() {
        GameDriver game = GameDriver.start(MAP.write("smooth-movement"));
        assertEquals(START, game.playerCell());
        game.click(NEXT_TO_CHEST);

        List<float[]> positions = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        for (int frame = 0; frame < 240; frame++) {
            positions.add(game.playerXY());
            statuses.add(game.playerStatus());
            game.frames(1);
        }

        int arrival = statuses.lastIndexOf("MOVEMENT");
        assertTrue(arrival > 0, "the player walked");
        assertFalse(statuses.subList(statuses.indexOf("MOVEMENT"), arrival + 1).contains("IDLE"),
                "the player never stops on the way");
        boolean diagonal = false;
        for (int i = statuses.indexOf("MOVEMENT") + 1; i < arrival; i++) {
            float dx = positions.get(i)[0] - positions.get(i - 1)[0];
            float dy = positions.get(i)[1] - positions.get(i - 1)[1];
            assertEquals(PX_PER_FRAME, (float) Math.sqrt(dx * dx + dy * dy), 0.05f, "speed at frame " + i);
            diagonal |= Math.abs(dx) > 1 && Math.abs(dy) > 1;
        }
        assertTrue(diagonal, "part of the walk is a straight diagonal line");
        assertEquals(NEXT_TO_CHEST, game.playerCell());
        assertEquals("IDLE", game.playerStatus());
    }

    @Test
    void walkingPastTheChestLeavesNoCollectButton() {
        GameDriver game = GameDriver.start(MAP.write("smooth-movement"));
        game.click(NEXT_TO_CHEST); // next to the chest: the COLLECT button shows
        game.seconds(3);
        assertTrue(game.snapshot("next to the chest").contains("COLLECT"));
        game.click(PAST_THE_CHEST); // walks on past the chest without stopping
        game.seconds(3);
        String scene = game.snapshot("up the corridor");
        assertEquals(PAST_THE_CHEST, game.playerCell());
        assertFalse(scene.contains("COLLECT"), "the COLLECT button fades out once the player walks on:\n" + scene);
    }
}
