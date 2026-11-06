package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/**
 * S2-S5: walking along the board. Targets have a single shortest path, so the expected route is the same
 * for any correct path finding. The chest blocks the only corridor south (see CollectTest), so walks go east/west.
 */
class WalkingTest {
    private static final Cell EAST_ROOM = new Cell(13, 24);
    private static final Cell WEST_ROOM = new Cell(7, 23);
    private static final Cell FURNITURE = new Cell(12, 24);
    private static final Cell NO_TILE = new Cell(11, 22);

    @Test
    void walkToClickedTile() {
        GameDriver game = GameDriver.start();
        game.click(EAST_ROOM);
        traceWalk(game, 2.5f);
        game.snapshot("arrived");
        Golden.assertMatches("walk", game.timeline());
    }

    @Test
    void clickingDuringWalkReroutes() {
        GameDriver game = GameDriver.start();
        game.click(EAST_ROOM);
        game.seconds(0.1f);
        game.click(WEST_ROOM);
        traceWalk(game, 3);
        game.snapshot("rerouted");
        Golden.assertMatches("reroute", game.timeline());
    }

    @Test
    void clickingBlockedTileShowsNoPath() {
        GameDriver game = GameDriver.start();
        game.click(FURNITURE);
        game.snapshot("just clicked");
        game.seconds(1);
        game.snapshot("after 1 s");
        game.seconds(1.1f);
        game.snapshot("after 2.1 s");
        Golden.assertMatches("no-path", game.timeline());
    }

    @Test
    void clickingWhereThereIsNoTileDoesNothing() {
        GameDriver game = GameDriver.start();
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
