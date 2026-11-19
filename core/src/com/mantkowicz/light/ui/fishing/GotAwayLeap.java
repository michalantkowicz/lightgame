package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/**
 * The fish that got away (no tap in time after the bite): it leaps out of the water where the bobber went under,
 * almost straight up, head first, turns over at the top and drops back in head down a little to the side, fading as it
 * goes under, then it is gone. On the game stage, in its units; only a picture.
 */
final class GotAwayLeap extends Image {
    /** How long it is in the air; how high and how far to the side it leaps, in its own sizes; the part at the end it fades. */
    static final float SECONDS = 1.2f;
    private static final float HIGH = 1.1f, SIDE = 0.25f, FADE = 0.3f;
    /** On the sheet the fish face up and right: turned this much they face straight up, and this much straight down. */
    private static final float HEAD_UP = 45, HEAD_DOWN = -135;

    private final Vector2 water;
    private final float size;
    private float time;

    GotAwayLeap(TextureRegion fish, Vector2 water, float size) {
        super(fish);
        this.water = water.cpy();
        this.size = size;
        setTouchable(Touchable.disabled);
        setSize(size, size);
        setOrigin(size / 2f, size / 2f);
        place(0);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        float t = Math.min(1, time / SECONDS);
        place(t);
        if (t >= 1) {
            remove();
        }
    }

    private void place(float t) {
        float x = water.x + (t - 0.5f) * SIDE * size, y = water.y + MathUtils.sin(MathUtils.PI * t) * HIGH * size;
        setPosition(x - size / 2f, y - size / 2f);
        setRotation(MathUtils.lerp(HEAD_UP, HEAD_DOWN, Interpolation.smooth.apply(t))); // over the top
        getColor().a = MathUtils.clamp((1 - t) / FADE, 0, 1);
    }
}
