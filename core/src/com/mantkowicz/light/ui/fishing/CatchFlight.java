package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/**
 * A caught fish flying out of the water to the rabbit: from where the bobber was, in an arc, turning, growing a little,
 * then gone and {@code landed} (it goes into the backpack then). On the game stage, in its units.
 */
public class CatchFlight extends Image {
    /** How high the arc goes over the straight line, in sizes of the fish; how many times it turns over. */
    private static final float ARC = 1.5f, TURNS = 1;

    private final Vector2 from, to;
    private final float size, seconds;
    private final Runnable landed;
    private float time;

    /**
     * @param from    where it leaps out (the bobber's middle), a game stage point
     * @param to      where it flies (the rabbit's middle)
     * @param size    how big it is drawn at the end
     * @param seconds how long it flies
     * @param landed  told when it is there (and gone)
     */
    public CatchFlight(TextureRegion picture, Vector2 from, Vector2 to, float size, float seconds, Runnable landed) {
        super(picture);
        this.from = from.cpy();
        this.to = to.cpy();
        this.size = size;
        this.seconds = seconds;
        this.landed = landed;
        setTouchable(Touchable.disabled);
        setSize(size, size);
        setOrigin(size / 2f, size / 2f);
        place(0);
    }

    /**
     * The fish that got away leaping out of the water at {@code water} (where the bobber went under) and back in, to
     * add to the game stage ({@link GotAwayLeap}).
     *
     * @param size how big it is drawn (a little more than a tile)
     */
    public static Image gotAway(ResourcesService resources, Vector2 water, float size) {
        TextureRegion[] fish = resources.getAnimation(Sprite.FISH_CATCH, Action.CAUGHT).getKeyFrames();
        TextureRegion[] more = resources.getAnimation(Sprite.FISH_CATCH, Action.SWIM).getKeyFrames();
        TextureRegion[] clearest = {fish[2], fish[4], more[2]}; // yellow, blue, a clownfish: as in the fishing game
        return new GotAwayLeap(clearest[MathUtils.random(clearest.length - 1)], water, size);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        float t = Math.min(1, time / seconds);
        place(t);
        if (t >= 1) {
            remove();
            landed.run();
        }
    }

    private void place(float t) {
        float along = Interpolation.sineOut.apply(t);
        float x = MathUtils.lerp(from.x, to.x, along);
        float y = MathUtils.lerp(from.y, to.y, along) + MathUtils.sin(MathUtils.PI * t) * ARC * size;
        setPosition(x - size / 2f, y - size / 2f);
        setRotation(-360 * TURNS * t);
        setScale(MathUtils.lerp(0.6f, 1, t));
    }
}
