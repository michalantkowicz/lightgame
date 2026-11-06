package com.mantkowicz.light.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Goldens only make sense if the same script always produces the same game state. */
class DeterminismTest {
    @Test
    void sameScriptGivesSameState() {
        assertEquals(play(), play());
    }

    private static String play() {
        GameDriver game = GameDriver.start();
        game.click(new Cell(13, 24));
        game.seconds(0.4f);
        game.snapshot("walking");
        game.seconds(2);
        game.snapshot("arrived");
        game.stop();
        return game.timeline();
    }
}
