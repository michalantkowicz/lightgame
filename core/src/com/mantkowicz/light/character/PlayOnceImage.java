package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/** A picture that plays an animation once, then removes itself (and says so, if asked). */
class PlayOnceImage extends Image {
    private final Animation<TextureRegion> animation;
    private final Runnable onFinished;
    private float time;

    /** @param onFinished told once it has played and is gone, or null */
    PlayOnceImage(Animation<TextureRegion> animation, Runnable onFinished) {
        super(animation.getKeyFrame(0));
        this.animation = animation;
        this.onFinished = onFinished;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        if (animation.isAnimationFinished(time)) {
            remove();
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        ((TextureRegionDrawable) getDrawable()).setRegion(animation.getKeyFrame(time));
    }
}
