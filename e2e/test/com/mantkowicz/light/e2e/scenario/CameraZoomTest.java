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
    private static final Cell CHEST = new Cell(10, 24);

    @Test
    void zoomingInMakesTheWorldBiggerButNotTheButtons() {
        GameDriver normal = GameDriver.start();
        GameDriver close = GameDriver.startZoomed(0.5f);
        assertEquals(2 * normal.cellSizeOnScreen(START), close.cellSizeOnScreen(START), 0.01f, "cells twice as big");

        for (GameDriver game : new GameDriver[]{normal, close}) {
            game.click(CHEST);
            game.seconds(2);
            assertEquals(CHEST, game.playerCell(), "clicks hit the right cell at any zoom");
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
