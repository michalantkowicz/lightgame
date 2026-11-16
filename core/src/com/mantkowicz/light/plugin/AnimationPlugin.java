package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Plugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Direction;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

import static com.mantkowicz.light.actor.Status.MOVEMENT;
import static com.mantkowicz.light.resources.Direction.DOWN;
import static com.mantkowicz.light.resources.Direction.LEFT;
import static com.mantkowicz.light.resources.Direction.RIGHT;
import static com.mantkowicz.light.resources.Direction.UP;

/**
 * Animates an actor's picture from its sprite sheet: WALK in the direction it moves while it moves (e.g. the rabbit's
 * "walk_left"), and when it stops it turns back to the default IDLE facing DOWN. The direction follows the way the
 * actor actually moves; on a diagonal it keeps the direction it had if that one still fits, so it doesn't flicker.
 * Standing, it can play another action once ({@link #playOnce}, e.g. watering), then it stands as before.
 */
public class AnimationPlugin implements Plugin {
    /** How much one axis must dominate to switch the direction to it. */
    private static final float DOMINANCE = 1.2f;

    private final GameActor actor;
    private final ResourcesService resourcesService;
    private final Sprite sprite;
    private Vector2 lastCenter;
    private Direction direction = DOWN;
    private String current;
    private Animation<TextureRegion> animation;
    private float stateTime;
    /** An action played once while standing (e.g. "rabbit/water_left"), or null. */
    private Action once;
    private Direction onceDirection;

    public AnimationPlugin(GameActor actor, ResourcesService resourcesService, Sprite sprite) {
        this.actor = actor;
        this.resourcesService = resourcesService;
        this.sprite = sprite;
    }

    @Override
    public void run(float delta) {
        Image avatar = actor.getAvatar();
        if (avatar == null) {
            return;
        }
        boolean walking = MOVEMENT.equals(actor.getStatus());
        followMovement(walking);
        if (walking) {
            once = null; // walking off stops it
        }
        Action action = once != null ? once : walking ? Action.WALK : Action.IDLE;
        Direction facing = once != null ? onceDirection : direction;
        String name = ResourcesService.animationName(sprite, action, facing);
        if (!name.equals(current)) {
            current = name;
            animation = resourcesService.getAnimation(sprite, action, facing);
            stateTime = 0;
        } else {
            stateTime += delta;
        }
        ((TextureRegionDrawable) avatar.getDrawable()).setRegion(animation.getKeyFrame(stateTime));
        if (once != null && stateTime >= animation.getAnimationDuration()) {
            once = null; // played once: standing again from the next frame
        }
    }

    /**
     * Plays an action once in a direction, e.g. (WATER, LEFT) "rabbit/water_left", unless the actor walks off
     * meanwhile; then it stands as before.
     */
    public void playOnce(Action action, Direction direction) {
        once = action;
        onceDirection = direction;
        current = null; // starts from its first frame, also when the same action is played again
    }

    /** Is it playing this action once right now (not finished, not stopped by walking off)? */
    public boolean isPlayingOnce(Action action) {
        return once == action;
    }

    /** Walking, it faces the way it moves; standing, it faces down (towards the player). */
    private void followMovement(boolean walking) {
        Vector2 center = actor.getCenter();
        if (!walking) {
            direction = DOWN; // standing: the default pose, facing the player
        } else if (lastCenter != null) {
            updateDirection(center.x - lastCenter.x, center.y - lastCenter.y);
        }
        lastCenter = center;
    }

    private void updateDirection(float dx, float dy) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        if (ax < 0.001f && ay < 0.001f) {
            return;
        }
        Direction horizontal = dx > 0 ? RIGHT : LEFT;
        Direction vertical = dy > 0 ? UP : DOWN;
        if (ax > ay * DOMINANCE) {
            direction = horizontal;
        } else if (ay > ax * DOMINANCE) {
            direction = vertical;
        } else if (direction != horizontal && direction != vertical) {
            direction = horizontal;
        }
    }

    /**
     * The direction the actor faces.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * The animation being played, e.g. "rabbit/walk_left".
     */
    public String getCurrentAnimation() {
        return current;
    }
}
