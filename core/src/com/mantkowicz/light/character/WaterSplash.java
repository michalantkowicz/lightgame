package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/** Water falling from the can onto a field: plays once, then it is gone and the field is told. */
class WaterSplash extends Image {
    private final Animation<TextureRegion> falling;
    private final Runnable onFallen;
    private float time;

    WaterSplash(ResourcesService resources, Runnable onFallen) {
        this(resources.getAnimation(Sprite.WATERING, Action.SPLASH), onFallen);
    }

    private WaterSplash(Animation<TextureRegion> falling, Runnable onFallen) {
        super(falling.getKeyFrame(0));
        this.falling = falling;
        this.onFallen = onFallen;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        if (falling.isAnimationFinished(time)) {
            remove();
            onFallen.run();
            return;
        }
        ((TextureRegionDrawable) getDrawable()).setRegion(falling.getKeyFrame(time));
    }
}
