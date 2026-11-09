package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S6, S7: the camera follows the player softly and glides to the player when clicked. */
class CameraTest {
    @Test
    void clickingPlayerCentersCamera() {
        GameDriver game = GameDriver.start(WalkingTest.CORRIDOR.write("corridor"));
        game.click(WalkingTest.CORRIDOR.cell('e'));
        game.seconds(2.5f);
        game.snapshot("after walk");
        game.click(Kind.PLAYER);
        game.seconds(0.5f);
        game.snapshot("camera moving");
        game.seconds(0.6f);
        game.snapshot("camera centered");
        Golden.assertMatches("look-at-player", game.timeline());
    }

    @Test
    void cameraFollowsWalkingPlayer() {
        GameDriver game = GameDriver.start(WalkingTest.CORRIDOR.write("corridor"));
        game.click(WalkingTest.CORRIDOR.cell('e'));
        traceCamera(game, 2.5f);
        game.click(WalkingTest.CORRIDOR.cell('P')); // back where it started
        traceCamera(game, 4);
        game.snapshot("end");
        Golden.assertMatches("camera-tracking", game.timeline());
    }

    private static void traceCamera(GameDriver game, float seconds) {
        int frames = Math.round(seconds * 60);
        for (int frame = 0; frame < frames; frame += 10) {
            game.note("frame " + frame + ": camera=" + game.cameraPosition() + " player=" + game.playerPosition());
            game.frames(10);
        }
    }
}
