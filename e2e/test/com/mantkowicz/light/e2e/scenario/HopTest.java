package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rabbit hops while it walks: its picture rises up to 10 px and lands again, two hops per tile. Its position on the board is
 * not affected. When the walk ends it comes down (it never starts another hop after stopping) and rests on the ground.
 */
class HopTest {
    private static final Cell EAST = WalkingTest.CORRIDOR.cell('e');
    private static final float HOP_HEIGHT = 10f;

    @Test
    void hopsWhileWalkingAndLandsAtTheEnd() {
        GameDriver game = GameDriver.start(WalkingTest.CORRIDOR.write("corridor"));
        assertEquals(0f, game.playerHopHeight(), "standing still: no hop");

        game.click(EAST);
        List<Float> heights = new ArrayList<>();
        List<Float> afterStop = new ArrayList<>();
        for (int frame = 0; frame < 180; frame++) {
            boolean stopped = frame > 5 && "IDLE".equals(game.playerStatus());
            (stopped ? afterStop : heights).add(game.playerHopHeight());
            game.frames(1);
        }
        float highest = 0;
        int landings = 0;
        for (int i = 1; i < heights.size() - 1; i++) {
            highest = Math.max(highest, heights.get(i));
            boolean lowPoint = heights.get(i) < heights.get(i - 1) && heights.get(i) <= heights.get(i + 1);
            if (lowPoint && heights.get(i) < 2f) {
                landings++;
            }
        }
        assertTrue(highest > HOP_HEIGHT * 0.9f && highest <= HOP_HEIGHT + 0.01f, "hop height " + highest);
        assertTrue(landings >= 5, "the rabbit lands between hops (" + landings + " landings)");

        assertTrue(!afterStop.isEmpty(), "the walk ended");
        for (int i = 1; i < afterStop.size(); i++) {
            assertTrue(afterStop.get(i) <= afterStop.get(i - 1),
                    "no hop after stopping: " + afterStop.subList(0, Math.min(15, afterStop.size())));
        }
        assertEquals(0f, afterStop.get(Math.min(12, afterStop.size() - 1)),
                "landed within half a hop: " + afterStop.subList(0, Math.min(15, afterStop.size())));

        assertEquals(EAST, game.playerCell());
        assertEquals(0f, game.playerHopHeight(), "after the walk the rabbit rests on the ground");
    }
}
