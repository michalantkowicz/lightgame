package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The camera zoom (Main.CAMERA_ZOOM) changes only the world: zoomed in, the board is drawn bigger and still answers
 * clicks, while buttons keep their place and size on the screen (the UI has its own camera).
 */
class CameraZoomTest {
    private static final Cell START = new Cell(10, 22);
    private static final Cell CHEST = new Cell(8, 24);
    /** The chest blocks its cell: clicking it walks to the nearest free cell next to it. */
    private static final Cell NEXT_TO_CHEST = new Cell(8, 23);

    @Test
    void zoomingInMakesTheWorldBiggerButNotTheButtons() {
        GameDriver normal = GameDriver.start();
        assertEquals(2 * normal.cellSizeOnScreen(START), GameDriver.startZoomed(0.5f).cellSizeOnScreen(START), 0.01f,
                "cells twice as big");

        GameDriver close = GameDriver.startZoomed(0.75f); // close enough, and the chest is still on screen
        assertEquals(normal.cellSizeOnScreen(START) / 0.75f, close.cellSizeOnScreen(START), 0.01f);

        for (GameDriver game : new GameDriver[]{normal, close}) {
            game.click(CHEST);
            game.seconds(2);
            assertEquals(NEXT_TO_CHEST, game.playerCell(), "clicks hit the right cell at any zoom");
            assertFalse(game.crashed());
        }
        assertEquals(normal.buttonOnScreen("COLLECT"), close.buttonOnScreen("COLLECT"), "the button doesn't move or grow");
    }

    @Test
    void zoomingOutShowsMoreOfTheWorld() {
        GameDriver normal = GameDriver.start();
        GameDriver far = GameDriver.startZoomed(2f);
        assertEquals(normal.cellSizeOnScreen(START) / 2, far.cellSizeOnScreen(START), 0.01f, "cells half as big");
    }
}
