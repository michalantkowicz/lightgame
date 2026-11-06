package com.mantkowicz.light.e2e.scenario;

import com.badlogic.gdx.Input.Keys;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S12, S13: Q/W toggle the fade shader; torches flicker. */
class ShaderAndLightTest {
    @Test
    void qFadesAndWRestores() {
        GameDriver game = GameDriver.start();
        game.pressKey(Keys.Q);
        game.snapshot("after Q");
        game.pressKey(Keys.W);
        game.snapshot("after W");
        Golden.assertMatches("fade-shader", game.timeline());
    }

    @Test
    void torchFlickers() {
        GameDriver game = GameDriver.start();
        for (int frame = 0; frame < 72; frame++) {
            game.note("frame " + frame + ": torch distance " + game.torchDistances());
            game.frames(1);
        }
        Golden.assertMatches("torch-flicker", game.timeline());
    }
}
