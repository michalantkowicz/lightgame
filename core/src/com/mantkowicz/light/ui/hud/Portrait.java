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
 * corners, the character's bust standing on its bottom edge. The face is mostly still; every few seconds it plays one
 * of its gestures (blinking, twitching an ear), in a fixed order so tests see the same face every run.
 * <p>
 * Anything can make the face react for a while ({@link #play(UiImage, float)}, e.g. cheering while it says something)
 * or once ({@link #play(UiImage)}); then it goes back to being still and to its gestures by itself.
 */
public class Portrait extends Group {
    /** The frame's inside, in pack pixels: one emote frame (32x32). */
    private static final int INSIDE = 32;
    /** The frame's border in pack pixels (posts and bars), the split of {@link UiImage#PORTRAIT_FRAME}. */
    private static final int BORDER = 7;
    public static final float WIDTH = (INSIDE + 2 * BORDER) * Hud.SCALE, HEIGHT = WIDTH;
    /** Seconds of stillness between two gestures. */
    static final float STILL_SECONDS = 2.5f;

    private final ResourcesService resources;
    private final String name;
    private final Animation<TextureRegion> still;
    private final Animation<TextureRegion>[] gestures;
    private final Image picture;
    private float time;
    private int gesture = -1, nextGesture;
    /** What the face plays instead of its idle, or null; how long it has played and how long it plays. */
    private UiImage reaction;
    private Animation<TextureRegion> reactionAnimation;
    private float reactionTime, reactionSeconds;

    /**
     * @param name     what the portrait shows, e.g. "teemo" (for tests and descriptions)
     * @param gestures played one after another, each after {@link #STILL_SECONDS} of the still face
     */
    @SafeVarargs
    public Portrait(ResourcesService resources, String name, Animation<TextureRegion> still, Animation<TextureRegion>... gestures) {
        this.resources = resources;
        this.name = name;
        this.still = still;
        this.gestures = gestures;
        setSize(WIDTH, HEIGHT);

        NinePatch frame = new NinePatch(resources.getUiNinePatch(UiImage.PORTRAIT_FRAME));
        frame.scale(Hud.SCALE, Hud.SCALE);
        Image background = new Image(new NinePatchDrawable(frame));
        background.setSize(WIDTH, HEIGHT);
        addActor(background);

        TextureRegion first = still.getKeyFrame(0);
        picture = new Image(new TextureRegionDrawable(first));
        picture.setSize(first.getRegionWidth() * Hud.SCALE, first.getRegionHeight() * Hud.SCALE);
        // standing on the frame's bottom bar, in the middle of the inside
        picture.setPosition(BORDER * Hud.SCALE, BORDER * Hud.SCALE);
        addActor(picture);
    }

    /**
     * Plays an animation over and over for a while instead of the idle (a new one replaces it), then goes back to the
     * still face.
     *
     * @param animation a face animation from the pack, e.g. {@link UiImage#TEEMO_CHEER}
     */
    public void play(UiImage animation, float seconds) {
        reaction = animation;
        reactionAnimation = resources.getUiAnimation(animation);
        reactionTime = 0;
        reactionSeconds = seconds;
    }

    /** Plays an animation once instead of the idle (a new one replaces it), then goes back to the still face. */
    public void play(UiImage animation) {
        play(animation, resources.getUiAnimation(animation).getAnimationDuration());
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (reaction != null) {
            reactionTime += delta;
            if (reactionTime < reactionSeconds) {
                show(reactionAnimation.getKeyFrame(reactionTime, true));
                return;
            }
            reaction = null;
            time = 0; // back to the still face, a full pause before the next gesture
            gesture = -1;
        }
        time += delta;
        if (gesture < 0) {
            if (time >= STILL_SECONDS && gestures.length > 0) {
                time -= STILL_SECONDS;
                gesture = nextGesture++ % gestures.length;
            }
        } else if (gestures[gesture].isAnimationFinished(time)) {
            time = 0;
            gesture = -1;
        }
        Animation<TextureRegion> playing = gesture < 0 ? still : gestures[gesture];
        show(playing.getKeyFrame(time, gesture < 0));
    }

    private void show(TextureRegion frame) {
        ((TextureRegionDrawable) picture.getDrawable()).setRegion(frame);
    }

    /** What the face plays instead of its idle right now, or null when idle. */
    public UiImage getReaction() {
        return reaction;
    }

    public String getPictureName() {
        return name;
    }
}
