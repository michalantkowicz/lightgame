package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/**
 * A weed growing on a field instead of its crop: after the field's sprout, the rest of the sprout's row (a blue
 * flower). Pulled out, a copy of it rises a little and fades out where it grew.
 */
class WeedPlant extends Image {
    /** A pulled weed rises a little (art pixels) while it fades out, so it doesn't just vanish. */
    private static final float PULL_RISE = 4, PULL_SECONDS = 0.5f;

    private final TextureRegion[] frames;
    /** The pulled weed fading out, while it does; null otherwise. */
    private Image pulled;

    WeedPlant(ResourcesService resources) {
        this(resources.getAnimation(Sprite.PLANT_WEED, Action.GROW).getKeyFrames());
    }

    private WeedPlant(TextureRegion[] frames) {
        super(frames[0]);
        this.frames = frames;
        setTouchable(Touchable.disabled);
    }

    /** The field's phase it is ripe at: its pictures come after the sprout (phase 0). */
    int getRipePhase() {
        return frames.length;
    }

    /** The ripe weed: the picture on the field's button. */
    TextureRegion getRipePicture() {
        return frames[frames.length - 1];
    }

    /** Shows its picture for a phase of the field, the first one at phase 1. */
    void showPhase(int phase) {
        ((TextureRegionDrawable) getDrawable()).setRegion(frames[Math.max(0, phase - 1)]);
    }

    /** A copy of the ripe weed rises a little and fades out where it grew (the field shows its sprout under it). */
    void pullOut() {
        Image fading = new Image(getRipePicture());
        fading.setTouchable(Touchable.disabled);
        fading.setBounds(getX(), getY(), getWidth(), getHeight());
        getParent().addActor(fading);
        pulled = fading;
        fading.addAction(Actions.sequence(
                Actions.parallel(Actions.fadeOut(PULL_SECONDS),
                        Actions.moveBy(0, PULL_RISE * WorldScale.PIXEL_SCALE, PULL_SECONDS, Interpolation.pow2Out)),
                Actions.run(() -> {
                    if (pulled == fading) {
                        pulled = null;
                    }
                }), Actions.removeActor())); // told first: once removed, it acts no more
    }

    /** Is a pulled weed still fading out? */
    boolean isFadingOut() {
        return pulled != null;
    }
}
