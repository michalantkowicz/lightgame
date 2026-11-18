package com.mantkowicz.light.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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
        setColor(0, 0, 0, 0);
        stage.addActor(this);
        coverTheScreen();
        addAction(Actions.sequence(
                Actions.alpha(1, seconds),
                Actions.run(whenDark),
                Actions.alpha(0, seconds),
                Actions.run(() -> {
                    remove(); // in the same step: an actor off the stage doesn't act any more
                    whenLight.run();
                })));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null) {
            coverTheScreen(); // the window may have changed meanwhile
        }
    }

    /**
     * Exactly what the stage shows, whatever the window: a wide one shows much more than the stage's minimum size
     * (its {@link ExtendViewport} grows), so fixed bounds left a strip at the side uncovered.
     */
    private void coverTheScreen() {
        Viewport viewport = getStage().getViewport();
        Vector3 centre = viewport.getCamera().position;
        setBounds(centre.x - viewport.getWorldWidth() / 2 - 1, centre.y - viewport.getWorldHeight() / 2 - 1,
                viewport.getWorldWidth() + 2, viewport.getWorldHeight() + 2);
    }

    void dispose() {
        pixel.dispose();
    }
}
