package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player walks at one constant speed (128 px in 0.4 s), never stops between tiles, and cuts straight lines
 * where the floor allows it (here: a diagonal into the kitchen).
 */
class SmoothMovementTest {
    private static final Cell START = new Cell(10, 22);
    private static final Cell KITCHEN = new Cell(7, 23);
    /** Up the corridor, away from the chest (the chest at (8,24) is used from (7,23), (7,24) or (8,23)). */
    private static final Cell UP_THE_CORRIDOR = new Cell(10, 26);
    private static final float PX_PER_FRAME = 128f / (0.4f * 60);

    @Test
    void constantSpeedNoStopsAndADiagonal() {
        GameDriver game = GameDriver.start();
        assertEquals(START, game.playerCell());
        game.click(KITCHEN);

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
        assertEquals(KITCHEN, game.playerCell());
        assertEquals("IDLE", game.playerStatus());
    }

    @Test
    void walkingPastTheChestLeavesNoCollectButton() {
        GameDriver game = GameDriver.start();
        game.click(KITCHEN); // next to the chest: the COLLECT button shows
        game.seconds(3);
        assertTrue(game.snapshot("next to the chest").contains("COLLECT"));
        game.click(UP_THE_CORRIDOR); // walks on past the chest without stopping
        game.seconds(3);
        String scene = game.snapshot("up the corridor");
        assertEquals(UP_THE_CORRIDOR, game.playerCell());
        assertFalse(scene.contains("COLLECT"), "the COLLECT button fades out once the player walks on:\n" + scene);
    }
}
