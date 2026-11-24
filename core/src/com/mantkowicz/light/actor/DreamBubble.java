package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * What a sleeping animal dreams about: a cloud with a picture in it (its favourite thing), little circles trailing down
 * to its head. Not all night long, as the "zzz": now and then it fades in for {@link #SHOWN_SECONDS}, fades out, and
 * comes back after a while ({@link #MIN_PAUSE_SECONDS}..{@link #MAX_PAUSE_SECONDS}). On the map's game stage, with the
 * animals (and the "zzz").
 */
public class DreamBubble extends Group {
    /** How long a dream shows, and how long until the next one (at random between the two). */
    public static final float SHOWN_SECONDS = 3, MIN_PAUSE_SECONDS = 4, MAX_PAUSE_SECONDS = 8;
    /** The first dream comes this long after falling asleep. */
    static final float FIRST_PAUSE_SECONDS = 2;
    /** Screen pixels per pixel of the bubble and of the picture in it; how long it fades in and out. */
    private static final float SCALE = 3, PICTURE_SCALE = 3, FADE_SECONDS = 0.3f;
    /** The middle of the cloud in the bubble's picture (pixels from its bottom left). */
    private static final float CLOUD_X = 23, CLOUD_Y = 21;
    /**
     * Where the bubble's bottom-left corner (the end of the trail) is on the sleeper's tile, a part of its width and
     * height: over its head, just over the "zzz" (which bobs from half its height up).
     */
    private static final float ACROSS = 0.35f, UP = 1.0f;

    private final Stage stage;
    private final Image picture = new Image();
    private float timeLeft = FIRST_PAUSE_SECONDS;
    private boolean shown;

    /** @param stage where it is shown: the map's game stage, with the animals */
    public DreamBubble(ResourcesService resources, Stage stage) {
        this.stage = stage;
        setName("dream");
        setTouchable(Touchable.disabled);
        Image bubble = new Image(resources.getUiImage(UiImage.DREAM_BUBBLE));
        bubble.setSize(bubble.getPrefWidth() * SCALE, bubble.getPrefHeight() * SCALE);
        setSize(bubble.getWidth(), bubble.getHeight());
        addActor(bubble);
        addActor(picture);
    }

    /**
     * Asleep: now and then it dreams about this (see the class). Call it every frame while the sleeper sleeps (it
     * doesn't move meanwhile); {@link #wake()} when it wakes up.
     */
    public void dream(Group sleeper, TextureRegion about, float delta) {
        timeLeft -= delta;
        if (timeLeft > 0) {
            if (shown) {
                toFront();
            }
            return;
        }
        if (shown) {
            shown = false;
            timeLeft = MathUtils.random(MIN_PAUSE_SECONDS, MAX_PAUSE_SECONDS);
            addAction(Actions.sequence(Actions.fadeOut(FADE_SECONDS), Actions.removeActor()));
            return;
        }
        shown = true;
        timeLeft = SHOWN_SECONDS;
        picture.setDrawable(new TextureRegionDrawable(about));
        picture.setSize(about.getRegionWidth() * PICTURE_SCALE, about.getRegionHeight() * PICTURE_SCALE);
        picture.setPosition(Math.round(CLOUD_X * SCALE - picture.getWidth() / 2f),
                Math.round(CLOUD_Y * SCALE - picture.getHeight() / 2f));
        setPosition(Math.round(sleeper.getX() + sleeper.getWidth() * ACROSS),
                Math.round(sleeper.getY() + sleeper.getHeight() * UP));
        clearActions();
        getColor().a = 0;
        addAction(Actions.fadeIn(FADE_SECONDS));
        stage.addActor(this);
    }

    /** Awake: no dream any more (the next sleep starts with a pause). */
    public void wake() {
        clearActions();
        remove();
        shown = false;
        timeLeft = FIRST_PAUSE_SECONDS;
    }

    /** What it dreams about (the picture in the cloud), or null before its first dream. */
    public TextureRegion getPicture() {
        return picture.getDrawable() == null ? null : ((TextureRegionDrawable) picture.getDrawable()).getRegion();
    }

    /** Is a dream showing now (also while it fades out)? */
    public boolean isShown() {
        return shown;
    }
}
