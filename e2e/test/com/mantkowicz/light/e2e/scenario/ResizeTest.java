package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S14: resizing the window keeps the view consistent and clicks still hit the right tiles. */
class ResizeTest {
    @Test
    void resizeKeepsGamePlayable() {
        GameDriver game = GameDriver.start(WalkingTest.CORRIDOR.write("corridor"));
        game.resize(800, 1280);
        game.snapshot("800x1280");
        game.click(WalkingTest.CORRIDOR.cell('e'));
        game.seconds(2.5f);
        game.snapshot("walked after resize");
        game.resize(300, 500);
        game.snapshot("300x500");
        Golden.assertMatches("resize", game.timeline());
    }
}
