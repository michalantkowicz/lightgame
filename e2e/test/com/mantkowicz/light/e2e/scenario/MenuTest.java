package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.Test;

/** S8-S11: press and hold an actor for the radial menu; release over a button for its description. */
class MenuTest {
    @Test
    void holdDogAndReleaseOverButtonShowsDescription() {
        GameDriver game = GameDriver.start();
        game.press(Kind.DOG);
        game.seconds(0.6f);
        game.snapshot("menu opening");
        game.seconds(0.5f);
        game.snapshot("menu open");
        game.dragToMenuButton(0);
        game.seconds(0.3f);
        game.snapshot("hovering button");
        game.release();
        game.seconds(1);
        game.snapshot("description after 1 s");
        game.seconds(2);
        game.snapshot("description after 3 s");
        game.seconds(1.1f);
        game.snapshot("description after 4.1 s");
        Golden.assertMatches("menu-dog-description", game.timeline());
    }

    @Test
    void releasingAwayFromButtonClosesMenu() {
        GameDriver game = GameDriver.start();
        game.press(Kind.DOG);
        game.seconds(1.1f);
        game.dragTo(new Cell(10, 23));
        game.release();
        game.seconds(0.5f);
        game.snapshot("released away");
        Golden.assertMatches("menu-cancel", game.timeline());
    }

    @Test
    void shortTapDoesNotOpenMenu() {
        GameDriver game = GameDriver.start();
        game.click(Kind.DOG);
        game.seconds(1);
        game.snapshot("after tap");
        Golden.assertMatches("menu-tap", game.timeline());
    }

    @Test
    void holdPlayerShowsPlayerDescription() {
        GameDriver game = GameDriver.start();
        game.press(Kind.PLAYER);
        game.seconds(1.1f);
        game.dragToMenuButton(0);
        game.seconds(0.3f);
        game.release();
        game.seconds(1);
        game.snapshot("description");
        Golden.assertMatches("menu-player-description", game.timeline());
    }
}
