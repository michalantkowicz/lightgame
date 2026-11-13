package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The camera zoom (Main.CAMERA_ZOOM) changes only the world: zoomed in, the board is drawn bigger and still answers
 * clicks, while buttons keep their place and size on the screen (the UI has its own camera).
 */
class CameraZoomTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..C..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~");
    private static final Cell START = MAP.cell('P');
    private static final Cell CHEST = MAP.cell('C');
    /** The chest blocks its cell: clicking it walks to the nearest free cell next to it. */
    private static final Cell NEXT_TO_CHEST = new Cell(3, 3);

    @Test
    void zoomingInMakesTheWorldBiggerButNotTheButtons() {
        GameDriver normal = GameDriver.start(MAP.write("zoom"));
        assertEquals(2 * normal.cellSizeOnScreen(START),
                GameDriver.startZoomed(MAP.write("zoom"), 0.5f).cellSizeOnScreen(START), 0.01f,
                "cells twice as big");

        GameDriver close = GameDriver.startZoomed(MAP.write("zoom"), 0.75f); // close enough, and the chest is still on screen
        assertEquals(normal.cellSizeOnScreen(START) / 0.75f, close.cellSizeOnScreen(START), 0.01f);

        for (GameDriver game : new GameDriver[]{normal, close}) {
            game.click(CHEST);
            game.seconds(2);
            assertEquals(NEXT_TO_CHEST, game.playerCell(), "clicks hit the right cell at any zoom");
            assertFalse(game.crashed());
        }
        assertEquals(normal.buttonOnScreen("COLLECT"), close.buttonOnScreen("COLLECT"),
                "the button doesn't move or grow");
    }

    @Test
    void zoomingOutShowsMoreOfTheWorld() {
        GameDriver normal = GameDriver.start(MAP.write("zoom"));
        GameDriver far = GameDriver.startZoomed(MAP.write("zoom"), 2f);
        assertEquals(normal.cellSizeOnScreen(START) / 2, far.cellSizeOnScreen(START), 0.01f, "cells half as big");
    }
}
