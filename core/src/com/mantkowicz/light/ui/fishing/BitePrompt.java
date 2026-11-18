package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * A fish bites: a big paw in the bottom right of the screen points at its middle (where the rabbit is: casting centres
 * the camera on it), a little off it, pulsing, with a circle under it that empties (the hold circle of the settings
 * gear, backwards). A tap anywhere on the screen in time hooks the fish; when the circle
 * is empty, it is gone. While it is shown, taps don't reach the map or the HUD.
 */
public class BitePrompt extends Group {
    /** Pixels per pack pixel for the paw and the circle. */
    private static final float SCALE = 12;
    /** The paw's finger is this far right of and below the screen's middle (not over the rabbit). */
    private static final float RIGHT = 50, DOWN = 70;
    private static final float PULSE = 1.2f, PULSE_SECONDS = 0.25f;

    private final TextureRegion[] circle;
    private final Image ring, paw;
    private final float seconds;
    private final Runnable tapped, tooLate;
    private float left;
    private boolean done;

    /**
     * @param ui      the UI stage: it covers what that shows
     * @param seconds how long the player has to tap
     * @param tapped  told when the screen is tapped in time
     * @param tooLate told when the time is up
     */
    public BitePrompt(ResourcesService resources, Stage ui, float seconds, Runnable tapped, Runnable tooLate) {
        this.seconds = seconds;
        this.left = seconds;
        this.tapped = tapped;
        this.tooLate = tooLate;
        setName("BITE");
        setTouchable(Touchable.enabled);

        TextureRegion pawPicture = resources.getUiImage(UiImage.PAW_POINTING);
        paw = new Image(pawPicture);
        paw.setSize(pawPicture.getRegionWidth() * SCALE, pawPicture.getRegionHeight() * SCALE);
        paw.setOrigin(0, paw.getHeight());
        paw.setTouchable(Touchable.disabled);
        paw.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                Actions.scaleTo(1, 1, PULSE_SECONDS, Interpolation.sine))));
        addActor(paw);

        circle = resources.getUiAnimation(UiImage.HOLD_CIRCLE).getKeyFrames();
        ring = new Image(new TextureRegionDrawable(circle[circle.length - 1]));
        ring.setSize(circle[0].getRegionWidth() * SCALE, circle[0].getRegionHeight() * SCALE);
        ring.setTouchable(Touchable.disabled);
        addActor(ring);
        cover(ScreenArea.of(ui));

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!done) {
                    done = true;
                    remove();
                    BitePrompt.this.tapped.run();
                }
                return true;
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (done) {
            return;
        }
        followTheScreen();
        left -= delta;
        float full = Math.max(0, left / seconds);
        ((TextureRegionDrawable) ring.getDrawable()).setRegion(circle[Math.round(full * (circle.length - 1))]);
        if (left <= 0) {
            done = true;
            remove();
            tooLate.run();
        }
    }

    /** Still over the whole screen after the window was resized (a phone turned), so any tap hooks. */
    private void followTheScreen() {
        Rectangle screen = ScreenArea.of(getStage());
        if (screen.x != getX() || screen.y != getY() || screen.width != getWidth() || screen.height != getHeight()) {
            cover(screen);
        }
    }

    /** Over this area; the paw's finger (the picture's top left) a little right of and below its middle. */
    private void cover(Rectangle screen) {
        setBounds(screen.x, screen.y, screen.width, screen.height);
        paw.setPosition(screen.width / 2f + RIGHT, screen.height / 2f - DOWN - paw.getHeight());
        ring.setPosition(paw.getX() + (paw.getWidth() - ring.getWidth()) / 2f, paw.getY() - ring.getHeight() - 16);
    }

    /** Seconds left to tap. */
    public float getSecondsLeft() {
        return Math.max(0, left);
    }
}
