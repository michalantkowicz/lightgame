package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S1, S16: what the player sees right after start, and that closing the game is clean. */
class StartAndStopTest {
    @Test
    void start() {
        GameDriver game = GameDriver.start();
        game.snapshot("start");
        Golden.assertMatches("start", game.timeline());
    }

    @Test
    void stop() {
        GameDriver game = GameDriver.start();
        game.stop();
        game.note("crashed on stop: " + game.crashed());
        Golden.assertMatches("stop", game.timeline());
    }
}
