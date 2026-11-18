package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S13: torches flicker. (S12, Q/W toggling the fade shader, went with those keys.) */
class ShaderAndLightTest {
    @Test
    void torchFlickers() {
        GameDriver game = GameDriver.start(StartAndStopTest.ROOM.write("room"));
        for (int frame = 0; frame < 72; frame++) {
            game.note("frame " + frame + ": torch distance " + game.torchDistances());
            game.frames(1);
        }
        Golden.assertMatches("torch-flicker", game.timeline());
    }
}
