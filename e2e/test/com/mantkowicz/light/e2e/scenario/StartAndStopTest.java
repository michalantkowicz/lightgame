package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

/** S1, S16: what the player sees right after start, and that closing the game is clean. */
class StartAndStopTest {
    /** A room with everything a player meets at the start: the rabbit, the dog, a chest and a torch. */
    static final TestMap ROOM = TestMap.of(
            "~~~~~~~",
            "~..*..~",
            "~.D...~",
            "~..P.C~",
            "~.....~",
            "~~~~~~~");
    @Test
    void start() {
        GameDriver game = GameDriver.start(ROOM.write("room"));
        game.snapshot("start");
        Golden.assertMatches("start", game.timeline());
    }

    @Test
    void stop() {
        GameDriver game = GameDriver.start(ROOM.write("room"));
        game.stop();
        game.note("crashed on stop: " + game.crashed());
        Golden.assertMatches("stop", game.timeline());
    }
}
