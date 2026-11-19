package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.Color;
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
 * A fish bites: a big hand pointer just under the screen's middle (where the rabbit is: casting centres the camera on
 * it) points up at it, pulsing, and is the clock too: white at first, it turns grey from its finger's tip down as the
 * time passes, row by row of its pixels. A tap anywhere on the screen in time hooks the fish; when it is all grey, the
 * fish is gone. While it is shown, taps don't reach the map or the HUD.
 */
public class BitePrompt extends Group {
    /** Pixels per pack pixel for the hand. */
    private static final float SCALE = 12;
    /** The hand's finger tip is this far right of and below the screen's middle (just under the rabbit). */
    private static final float RIGHT = 30, DOWN = 80;
    private static final float PULSE = 1.2f, PULSE_SECONDS = 0.25f;
    /** The hand's part that is used up turns this grey. */
    private static final Color USED = new Color(0.55f, 0.55f, 0.55f, 1);

    /** The hand: a grey one, and over it the white part still left (its bottom, shorter and shorter). */
    private final Group hand = new Group();
    private final TextureRegion picture, whitePart;
    private final Image white;
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

        picture = resources.getUiImage(UiImage.HAND_POINTING);
        hand.setSize(picture.getRegionWidth() * SCALE, picture.getRegionHeight() * SCALE);
        Image grey = new Image(picture);
        grey.setSize(hand.getWidth(), hand.getHeight());
        grey.setColor(USED);
        hand.addActor(grey);
        whitePart = new TextureRegion(picture);
        white = new Image(new TextureRegionDrawable(whitePart));
        white.setSize(hand.getWidth(), hand.getHeight());
        hand.addActor(white);
        hand.setOrigin(UiImage.HAND_TIP_X * SCALE, hand.getHeight()); // it pulses from its finger's tip
        hand.setTouchable(Touchable.disabled);
        hand.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                Actions.scaleTo(1, 1, PULSE_SECONDS, Interpolation.sine))));
        addActor(hand);
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
        showTimeLeft(Math.max(0, left / seconds));
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

    /** Over this area; the hand's finger tip a little right of and below its middle, pointing up at it. */
    private void cover(Rectangle screen) {
        setBounds(screen.x, screen.y, screen.width, screen.height);
        hand.setPosition(screen.width / 2f + RIGHT - UiImage.HAND_TIP_X * SCALE,
                screen.height / 2f - DOWN - hand.getHeight());
    }

    /** The white part left: this share of the hand's rows from its bottom (whole pixels); grey above it. */
    private void showTimeLeft(float share) {
        int rows = picture.getRegionHeight(), whiteRows = Math.round(share * rows);
        whitePart.setRegion(picture.getRegionX(), picture.getRegionY() + rows - whiteRows, picture.getRegionWidth(),
                whiteRows);
        white.setHeight(whiteRows * SCALE);
        white.setVisible(whiteRows > 0);
    }

    /** Seconds left to tap. */
    public float getSecondsLeft() {
        return Math.max(0, left);
    }
}
