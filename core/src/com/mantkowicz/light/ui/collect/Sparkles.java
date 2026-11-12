package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * A few small stars popping out of a point and fading away, e.g. when a chest opens.
 */
public final class Sparkles {
    static final int STARS = 7;
    /** A star's pixels per art pixel, how far it flies, and for how long. */
    private static final float SCALE = 4, MIN_DISTANCE = 60, MAX_DISTANCE = 130, MIN_SECONDS = 0.5f, MAX_SECONDS = 0.8f;

    private Sparkles() {
    }

    /** Stars fly out of {@code at} (a stage point) upwards and sideways, grow, then shrink and fade. */
    public static void burst(Stage stage, ResourcesService resources, Vector2 at) {
        TextureRegion star = resources.getUiImage(UiImage.SPARKLE);
        for (int i = 0; i < STARS; i++) {
            Image image = new Image(star);
            image.setSize(star.getRegionWidth() * SCALE, star.getRegionHeight() * SCALE);
            image.setOrigin(image.getWidth() / 2f, image.getHeight() / 2f);
            image.setPosition(at.x - image.getWidth() / 2f, at.y - image.getHeight() / 2f);
            image.setScale(0.3f);
            image.setTouchable(Touchable.disabled);

            float angle = MathUtils.random(10f, 170f) * MathUtils.degreesToRadians; // a fan over the point
            float distance = MathUtils.random(MIN_DISTANCE, MAX_DISTANCE);
            float seconds = MathUtils.random(MIN_SECONDS, MAX_SECONDS);
            image.addAction(sequence(
                    parallel(moveBy(MathUtils.cos(angle) * distance, MathUtils.sin(angle) * distance, seconds, Interpolation.pow2Out),
                            sequence(scaleTo(1, 1, seconds * 0.3f), scaleTo(0.2f, 0.2f, seconds * 0.7f)),
                            sequence(delay(seconds * 0.5f), fadeOut(seconds * 0.5f))),
                    Actions.removeActor()));
            stage.addActor(image);
        }
    }
}
