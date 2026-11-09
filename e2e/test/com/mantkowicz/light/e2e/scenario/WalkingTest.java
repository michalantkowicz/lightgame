package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

/**
 * S2-S5: walking along the board. Targets have a single shortest path, so the expected route is the same
 * for any correct path finding.
 */
class WalkingTest {
    /**
     * A corridor across the water, so every walk along it has one shortest route: w and e three cells either side of
     * the rabbit, a blocked cell above it (x) and a hole without a tile below it.
     */
    static final TestMap CORRIDOR = TestMap.of(
            "~~~~~~~~~",
            "~~~~x~~~~",
            "~w..P..e~",
            "~~~~ ~~~~",
            "~~~~~~~~~");
    private static final Cell EAST = CORRIDOR.cell('e');
    private static final Cell WEST = CORRIDOR.cell('w');
    private static final Cell BLOCKED = CORRIDOR.cell('x');
    private static final Cell NO_TILE = new Cell(4, 1);

    @Test
    void walkToClickedTile() {
        GameDriver game = GameDriver.start(CORRIDOR.write("corridor"));
        game.click(EAST);
        traceWalk(game, 2.5f);
        game.snapshot("arrived");
        Golden.assertMatches("walk", game.timeline());
    }

    @Test
    void clickingDuringWalkReroutes() {
        GameDriver game = GameDriver.start(CORRIDOR.write("corridor"));
        game.click(EAST);
        game.seconds(0.1f);
        game.click(WEST);
        traceWalk(game, 3);
        game.snapshot("rerouted");
        Golden.assertMatches("reroute", game.timeline());
    }

    @Test
    void clickingBlockedTileShowsNoPath() {
        GameDriver game = GameDriver.start(CORRIDOR.write("corridor"));
        game.click(BLOCKED);
        game.snapshot("just clicked");
        game.seconds(1);
        game.snapshot("after 1 s");
        game.seconds(1.1f);
        game.snapshot("after 2.1 s");
        Golden.assertMatches("no-path", game.timeline());
    }

    @Test
    void clickingWhereThereIsNoTileDoesNothing() {
        GameDriver game = GameDriver.start(CORRIDOR.write("corridor"));
        game.click(NO_TILE);
        game.seconds(1);
        game.snapshot("after click");
        Golden.assertMatches("no-tile", game.timeline());
    }

    /** Player cell, position and status every 5 frames. */
    static void traceWalk(GameDriver game, float seconds) {
        int frames = Math.round(seconds * 60);
        for (int frame = 0; frame < frames; frame += 5) {
            game.note("frame " + frame + ": player cell=" + game.playerCell() + " at=" + game.playerPosition());
            game.frames(5);
        }
    }
}
