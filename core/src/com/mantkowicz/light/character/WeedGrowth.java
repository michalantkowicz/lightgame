package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/**
 * A weed growing on a field instead of its crop: after the field's sprout, the rest of the sprout's row (a blue
 * flower). Ripe, it gives nothing; pulled out, a copy of it rises a little and fades out where it grew.
 */
class WeedGrowth extends Image implements Growth {
    /** A pulled weed rises a little (art pixels) while it fades out, so it doesn't just vanish. */
    private static final float PULL_RISE = 4, PULL_SECONDS = 0.5f;

    private final TextureRegion[] frames;
    /** The pulled weed fading out, while it does; null otherwise. */
    private Image pulled;

    WeedGrowth(ResourcesService resources) {
        this(resources.getAnimation(Sprite.PLANT_WEED, Action.GROW).getKeyFrames());
    }

    private WeedGrowth(TextureRegion[] frames) {
        super(frames[0]);
        this.frames = frames;
        setTouchable(Touchable.disabled);
    }

    /** It is its own picture. */
    @Override
    public Image getPlant() {
        return this;
    }

    @Override
    public int getRipePhase() {
        return frames.length;
    }

    /** The ripe weed. */
    @Override
    public TextureRegion getRipePicture() {
        return frames[frames.length - 1];
    }

    @Override
    public void showPhase(int phase) {
        ((TextureRegionDrawable) getDrawable()).setRegion(frames[Math.max(0, phase - 1)]);
    }

    /** Not the field's crop: the sign keeps its question mark. */
    @Override
    public boolean givesCrop() {
        return false;
    }

    /** Ripe, a weed gives nothing. */
    @Override
    public void ripe(Inventory inside) {
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
