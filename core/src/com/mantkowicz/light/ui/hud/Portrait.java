package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * A character's face in the HUD, drawn like the pack's Teemo emote preview: a dark wooden frame with posts at the
 * corners, the character's bust standing on its bottom edge. What it does when nothing happens is its {@link Idle},
 * e.g. mostly still with a gesture every few seconds (blinking, twitching an ear), in a fixed order so tests see the
 * same face every run; another idle can be set at any time ({@link #setIdle}, e.g. yawning when tired).
 * <p>
 * Anything can make the face react for a while ({@link #play(UiImage, float)}, e.g. cheering while it says something)
 * or once ({@link #play(UiImage)}); then it goes back to its idle by itself.
 */
public class Portrait extends Group {
    /** The frame's inside, in pack pixels: one emote frame (32x32). */
    private static final int INSIDE = 32;
    /** The frame's border in pack pixels (posts and bars), the split of {@link UiImage#PORTRAIT_FRAME}. */
    private static final int BORDER = 7;
    public static final float WIDTH = (INSIDE + 2 * BORDER) * Hud.SCALE, HEIGHT = WIDTH;

    /**
     * What the face does when nothing happens: plays {@code intro} once (or nothing when null), then over and over
     * shows the {@code still} face for {@code pause} seconds and plays the next of its {@code gestures}. With no pause
     * the gestures follow each other without a still face between them (e.g. sleeping).
     *
     * @param name for tests and descriptions, e.g. "awake"
     */
    public record Idle(String name, UiImage intro, UiImage still, float pause, UiImage... gestures) {
    }

    private final ResourcesService resources;
    private final String name;
    private final Image picture;
    private Idle idle;
    private Animation<TextureRegion> intro, still;
    private Animation<TextureRegion>[] gestures;
    private boolean introPlaying;
    private float time;
    private int gesture = -1, nextGesture;
    /** What the face plays instead of its idle, or null; how long it has played and how long it plays. */
    private UiImage reaction;
    private Animation<TextureRegion> reactionAnimation;
    private float reactionTime, reactionSeconds;

    /**
     * @param name what the portrait shows, e.g. "teemo" (for tests and descriptions)
     */
    public Portrait(ResourcesService resources, String name, Idle idle) {
        this.resources = resources;
        this.name = name;
        setSize(WIDTH, HEIGHT);

        NinePatch frame = new NinePatch(resources.getUiNinePatch(UiImage.PORTRAIT_FRAME));
        frame.scale(Hud.SCALE, Hud.SCALE);
        Image background = new Image(new NinePatchDrawable(frame));
        background.setSize(WIDTH, HEIGHT);
        addActor(background);

        picture = new Image(new TextureRegionDrawable());
        addActor(picture);
        setIdle(idle);
        TextureRegion first = (intro != null ? intro : still != null ? still : gestures[0]).getKeyFrame(0);
        show(first);
        picture.setSize(first.getRegionWidth() * Hud.SCALE, first.getRegionHeight() * Hud.SCALE);
        // standing on the frame's bottom bar, in the middle of the inside
        picture.setPosition(BORDER * Hud.SCALE, BORDER * Hud.SCALE);
    }

    /** What the face does from now on when nothing happens (setting the same idle again changes nothing). */
    @SuppressWarnings("unchecked")
    public void setIdle(Idle idle) {
        if (idle == this.idle) {
            return;
        }
        this.idle = idle;
        intro = idle.intro() == null ? null : resources.getUiAnimation(idle.intro());
        still = idle.still() == null ? null : resources.getUiAnimation(idle.still());
        gestures = new Animation[idle.gestures().length];
        for (int i = 0; i < gestures.length; i++) {
            gestures[i] = resources.getUiAnimation(idle.gestures()[i]);
        }
        introPlaying = intro != null;
        time = 0;
        gesture = -1;
        nextGesture = 0;
    }

    /**
     * Plays an animation over and over for a while instead of the idle (a new one replaces it), then goes back to the
     * idle.
     *
     * @param animation a face animation from the pack, e.g. {@link UiImage#TEEMO_CHEER}
     */
    public void play(UiImage animation, float seconds) {
        reaction = animation;
        reactionAnimation = resources.getUiAnimation(animation);
        reactionTime = 0;
        reactionSeconds = seconds;
    }

    /** Plays an animation once instead of the idle (a new one replaces it), then goes back to the idle. */
    public void play(UiImage animation) {
        play(animation, resources.getUiAnimation(animation).getAnimationDuration());
    }

    @Override
    // baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public void act(float delta) {
        super.act(delta);
        if (reaction != null) {
            reactionTime += delta;
            if (reactionTime < reactionSeconds) {
                show(reactionAnimation.getKeyFrame(reactionTime, true));
                return;
            }
            reaction = null;
            time = 0; // back to the idle, a full pause before the next gesture
            gesture = -1;
        }
        time += delta;
        if (introPlaying) {
            if (!intro.isAnimationFinished(time)) {
                show(intro.getKeyFrame(time, false));
                return;
            }
            introPlaying = false;
            time = 0;
        }
        if (gesture >= 0 && gestures[gesture].isAnimationFinished(time)) {
            time = 0;
            gesture = -1;
        }
        if (gesture < 0 && time >= idle.pause() && gestures.length > 0) {
            time -= idle.pause();
            gesture = nextGesture++ % gestures.length;
        }
        if (gesture >= 0) {
            show(gestures[gesture].getKeyFrame(time, false));
        } else if (still != null) {
            show(still.getKeyFrame(time, true));
        }
    }

    private void show(TextureRegion frame) {
        ((TextureRegionDrawable) picture.getDrawable()).setRegion(frame);
    }

    /** What the face plays instead of its idle right now, or null when idle. */
    public UiImage getReaction() {
        return reaction;
    }

    /** What the face does when nothing happens, e.g. "awake". */
    public Idle getIdle() {
        return idle;
    }

    public String getPictureName() {
        return name;
    }
}
