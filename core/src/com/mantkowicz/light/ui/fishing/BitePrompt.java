package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
 * A fish bites: the paw points at where the bobber went under, pulsing, with a circle next to it that empties (the
 * hold circle of the settings gear, backwards). A tap anywhere on the screen in time hooks the fish; when the circle
 * is empty, it is gone. While it is shown, taps don't reach the map or the HUD.
 */
public class BitePrompt extends Group {
    /** Pixels per pack pixel for the paw and the circle. */
    private static final float SCALE = 7;
    private static final float PULSE = 1.2f, PULSE_SECONDS = 0.25f;

    private final TextureRegion[] circle;
    private final Image ring;
    private final float seconds;
    private final Runnable tapped, tooLate;
    private float left;
    private boolean done;

    /**
     * @param ui      the UI stage: it covers what that shows
     * @param target  where the paw points, a point on the UI stage (the bobber's last place)
     * @param seconds how long the player has to tap
     * @param tapped  told when the screen is tapped in time
     * @param tooLate told when the time is up
     */
    public BitePrompt(ResourcesService resources, Stage ui, Vector2 target, float seconds, Runnable tapped,
            Runnable tooLate) {
        this.seconds = seconds;
        this.left = seconds;
        this.tapped = tapped;
        this.tooLate = tooLate;
        setName("BITE");
        Rectangle screen = ScreenArea.of(ui);
        setBounds(screen.x, screen.y, screen.width, screen.height);
        setTouchable(Touchable.enabled);

        TextureRegion pawPicture = resources.getUiImage(UiImage.PAW_POINTING);
        Image paw = new Image(pawPicture);
        paw.setSize(pawPicture.getRegionWidth() * SCALE, pawPicture.getRegionHeight() * SCALE);
        // its finger (the picture's top left) on the target
        paw.setPosition(target.x - screen.x, target.y - screen.y - paw.getHeight());
        paw.setOrigin(0, paw.getHeight());
        paw.setTouchable(Touchable.disabled);
        paw.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                Actions.scaleTo(1, 1, PULSE_SECONDS, Interpolation.sine))));
        addActor(paw);

        circle = resources.getUiAnimation(UiImage.HOLD_CIRCLE).getKeyFrames();
        ring = new Image(new TextureRegionDrawable(circle[circle.length - 1]));
        ring.setSize(circle[0].getRegionWidth() * SCALE, circle[0].getRegionHeight() * SCALE);
        ring.setPosition(paw.getX() + paw.getWidth() + 8, paw.getY() + paw.getHeight() - ring.getHeight());
        ring.setTouchable(Touchable.disabled);
        addActor(ring);

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

    /**
     * Still over the whole screen after the window was resized (a phone turned), so any tap hooks; the paw and the
     * circle stay where they were on the stage.
     */
    private void followTheScreen() {
        Rectangle screen = ScreenArea.of(getStage());
        float dx = screen.x - getX(), dy = screen.y - getY();
        if (dx == 0 && dy == 0 && screen.width == getWidth() && screen.height == getHeight()) {
            return;
        }
        setBounds(screen.x, screen.y, screen.width, screen.height);
        for (Actor child : getChildren()) {
            child.moveBy(-dx, -dy);
        }
    }

    /** Seconds left to tap. */
    public float getSecondsLeft() {
        return Math.max(0, left);
    }
}
