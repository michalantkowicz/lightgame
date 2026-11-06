package com.mantkowicz.light.e2e;

import com.mantkowicz.light.e2e.headless.HeadlessGame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class HeadlessGameSmokeTest {
    @Test
    void gameStartsRunsAndStops() {
        assertDoesNotThrow(() -> {
            HeadlessGame game = new HeadlessGame();
            for (int i = 0; i < 60; i++) {
                game.frame();
            }
            game.dispose();
        });
    }
}
