package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/**
 * The fishing line's bobber in the water (the Ocean Pack's frames, 32 px), with its splashes and the line from the
 * rod's tip: it drops in with a
 * splash, bobs a few times, and is pulled under with a splash when a fish takes it (then it is gone). Only a picture:
 * it blocks nothing, taps go through it.
 */
public class Bobber extends Group {
    /** The fishing line: how thick, how see-through. */
    private static final float LINE_WIDTH = 8, LINE_ALPHA = 0.55f;
    private final Drawable lineTexture;
    private Image line;
    /** What the bobber does now, e.g. "bob"; "gone" once it is pulled under. */
    private String doing = "drop";
    private final Image bobber, splash;
    private final Animation<TextureRegion> drop, bob, dip, splashing;
    private Animation<TextureRegion> playing;
    private float time, splashTime = -1;
    private int bobsLeft;
    private Runnable then;

    /** @param middle where it is, a stage point on the water (the bobber's middle) */
    public Bobber(ResourcesService resources, Vector2 middle) {
        setTouchable(Touchable.disabled);
        lineTexture = resources.getDrawable("black.png");
        drop = resources.getAnimation(Sprite.FISHING_BOBBER, Action.DROP);
        bob = resources.getAnimation(Sprite.FISHING_BOBBER, Action.BOB);
        dip = resources.getAnimation(Sprite.FISHING_BOBBER, Action.DIP);
        splashing = resources.getAnimation(Sprite.FISHING_BOBBER, Action.SPLASH);
        float size = 32 * WorldScale.PIXEL_SCALE;
        setBounds(middle.x - size / 2f, middle.y - size / 2f, size, size);
        splash = picture(splashing.getKeyFrame(0), size);
        splash.setVisible(false);
        bobber = picture(drop.getKeyFrame(0), size);
        addActor(splash);
        addActor(bobber);
        playing = drop;
    }

    private Image picture(TextureRegion region, float size) {
        Image image = new Image(new TextureRegionDrawable(region));
        image.setSize(size, size);
        return image;
    }

    /**
     * The fishing line from the rod's tip (a stage point) to the bobber: thin and a little see-through, behind it.
     */
    public void lineFrom(Vector2 rodTip) {
        Vector2 from = rodTip.cpy().sub(getX(), getY());
        Vector2 to = new Vector2(getWidth() / 2f, getHeight() / 2f);
        line = new Image(lineTexture);
        line.setBounds(from.x, from.y - LINE_WIDTH / 2f, from.dst(to), LINE_WIDTH);
        line.setOrigin(0, LINE_WIDTH / 2f);
        line.setRotation(to.cpy().sub(from).angleDeg());
        line.getColor().a = LINE_ALPHA;
        addActorAt(0, line);
    }

    /** It drops into the water with a splash, then {@code done}. */
    public void dropIn(Runnable done) {
        play("drop", drop, 1, done);
        splash();
    }

    /** It bobs this many times, then {@code done}. */
    public void bob(int times, Runnable done) {
        play("bob", bob, times, done);
    }

    /** A fish takes it: pulled under with a splash, then it is gone and {@code done}. */
    public void pullUnder(Runnable done) {
        if (line != null) {
            line.remove(); // the fish has it: the line goes under too
        }
        play("dip", dip, 1, () -> {
            doing = "gone";
            bobber.setVisible(false);
            done.run();
        });
        splash();
    }

    private void play(String what, Animation<TextureRegion> animation, int times, Runnable done) {
        doing = what;
        playing = animation;
        bobsLeft = times;
        time = 0;
        then = done;
        bobber.setVisible(true);
    }

    private void splash() {
        splashTime = 0;
        splash.setVisible(true);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (splashTime >= 0) {
            splashTime += delta;
            if (splashing.isAnimationFinished(splashTime)) {
                splashTime = -1;
                splash.setVisible(false);
            } else {
                ((TextureRegionDrawable) splash.getDrawable()).setRegion(splashing.getKeyFrame(splashTime, false));
            }
        }
        if (then == null) {
            return;
        }
        time += delta;
        ((TextureRegionDrawable) bobber.getDrawable()).setRegion(playing.getKeyFrame(time, true));
        if (time >= playing.getAnimationDuration()) {
            time = 0;
            bobsLeft--;
            if (bobsLeft <= 0) {
                Runnable done = then;
                then = null;
                ((TextureRegionDrawable) bobber.getDrawable()).setRegion(bob.getKeyFrame(0));
                done.run();
            }
        }
    }

    /** What it does now: "drop", "bob", "dip", or "gone" once a fish pulled it under (for tests). */
    public String getDoing() {
        return doing;
    }

    /** Its middle on the stage. */
    public Vector2 getMiddle() {
        return new Vector2(getX() + getWidth() / 2f, getY() + getHeight() / 2f);
    }
}
