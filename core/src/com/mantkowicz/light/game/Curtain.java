package com.mantkowicz.light.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

/**
 * Black over the whole screen while the player goes through a door: it darkens the screen, the player goes to the
 * other map meanwhile, and it lightens again ({@link GameScreen#CURTAIN_SECONDS} each way). A white pixel, stretched
 * and tinted black.
 */
final class Curtain extends Image {
    private final Texture pixel;

    private Curtain(Texture pixel) {
        super(pixel);
        this.pixel = pixel;
        setTouchable(Touchable.disabled);
    }

    static Curtain create() {
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(Color.WHITE);
        white.fill();
        Texture texture = new Texture(white);
        white.dispose();
        return new Curtain(texture);
    }

    /** Is it over the screen now (going down or up)? */
    boolean isDrawn() {
        return getStage() != null;
    }

    /**
     * Darkens the screen over everything on this stage, then (once black) does {@code whenDark}, lightens it again and
     * then does {@code whenLight}.
     */
    void fall(Stage stage, float seconds, Runnable whenDark, Runnable whenLight) {
        setBounds(-SCREEN_WIDTH, -SCREEN_HEIGHT, 3 * SCREEN_WIDTH, 3 * SCREEN_HEIGHT); // whatever the window
        setColor(0, 0, 0, 0);
        stage.addActor(this);
        addAction(Actions.sequence(
                Actions.alpha(1, seconds),
                Actions.run(whenDark),
                Actions.alpha(0, seconds),
                Actions.run(() -> {
                    remove(); // in the same step: an actor off the stage doesn't act any more
                    whenLight.run();
                })));
    }

    void dispose() {
        pixel.dispose();
    }
}
