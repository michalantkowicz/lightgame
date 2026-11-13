package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Plugin;
import com.mantkowicz.light.resources.ResourcesService;

/**
 * Plays one sprite animation on an actor's picture: looping ones (a cow's idle) run forever, the others (a chest
 * opening) stop on their last frame. {@link #play} switches to another animation, {@link #playBackwards} runs one in
 * reverse (a chest closing).
 */
public class SpriteAnimationPlugin implements Plugin {
    private final GameActor actor;
    private Animation<TextureRegion> animation;
    private String name;
    private float stateTime;

    public SpriteAnimationPlugin(GameActor actor) {
        this.actor = actor;
    }

    /**
     * Plays the animation from its start.
     *
     * @param name what it is called, e.g. "cow_brown/idle" (see {@link ResourcesService#animationName})
     */
    public void play(String name, Animation<TextureRegion> animation) {
        this.name = name;
        this.animation = animation;
        stateTime = 0;
    }

    /**
     * Plays the animation backwards, from its last frame to its first, and stops there.
     */
    public void playBackwards(String name, Animation<TextureRegion> animation) {
        Array<TextureRegion> frames = new Array<>(animation.getKeyFrames());
        play(name + " (backwards)", new Animation<>(animation.getFrameDuration(), frames, Animation.PlayMode.REVERSED));
    }

    /**
     * Jumps to the end of the current animation (e.g. a chest that starts closed shows its closed frame at once).
     */
    public void finish() {
        if (animation != null) {
            stateTime = animation.getAnimationDuration();
        }
    }

    @Override
    public void run(float delta) {
        Image avatar = actor.getAvatar();
        if (animation == null || avatar == null) {
            return;
        }
        stateTime += delta;
        ((TextureRegionDrawable) avatar.getDrawable()).setRegion(animation.getKeyFrame(stateTime));
    }

    /**
     * The animation being played, e.g. "chest/open_front" or "chest/open_front (backwards)".
     */
    public String getCurrentAnimation() {
        return name;
    }
}
