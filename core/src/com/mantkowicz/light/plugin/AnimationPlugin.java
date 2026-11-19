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
    /**
     * A pose from another sheet of the same character (e.g. the rabbit fishing), held until {@link #endPose()} or
     * walking off: looping, or played once and then still on its last frame; null for none.
     */
    private Sprite poseSprite;
    private Action pose;
    private Direction poseDirection;
    private boolean poseLoops;
    /** The sheet the picture is sized for now. */
    private Sprite fitted;

    public AnimationPlugin(GameActor actor, ResourcesService resourcesService, Sprite sprite) {
        this.actor = actor;
        this.resourcesService = resourcesService;
        this.sprite = sprite;
        this.fitted = sprite;
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
            pose = null;
        }
        Sprite sheet = pose != null ? poseSprite : sprite;
        advance(sheet, action(walking), facing(), delta);
        show(avatar, sheet);
        if (once != null && stateTime >= animation.getAnimationDuration()) {
            once = null; // played once: standing again from the next frame
        }
    }

    /** What it shows: its pose, else what it plays once, else walking or standing. */
    private Action action(boolean walking) {
        if (pose != null) {
            return pose;
        }
        if (once != null) {
            return once;
        }
        return walking ? Action.WALK : Action.IDLE;
    }

    private Direction facing() {
        if (pose != null) {
            return poseDirection;
        }
        return once != null ? onceDirection : direction;
    }

    /** The animation of this sheet, action and direction, from its start when it is a new one. */
    private void advance(Sprite sheet, Action action, Direction facing, float delta) {
        String name = ResourcesService.animationName(sheet, action, facing);
        if (name.equals(current)) {
            stateTime += delta;
            return;
        }
        current = name;
        animation = resourcesService.getAnimation(sheet, action, facing);
        stateTime = 0;
    }

    /** Its frame now (a pose played once stays on its last), the picture fitted to its sheet when that changed. */
    private void show(Image avatar, Sprite sheet) {
        TextureRegion frame = animation.getKeyFrame(stateTime, pose == null || poseLoops);
        ((TextureRegionDrawable) avatar.getDrawable()).setRegion(frame);
        if (sheet != fitted) {
            fitted = sheet;
            actor.fitAvatar(frame, resourcesService.getSpriteSheet(sheet));
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

    /**
     * Holds a pose from another sheet of the same character, e.g. the rabbit fishing ({@code RABBIT_FISHING}, CAST,
     * LEFT), until {@link #endPose()} or walking off: looping, or once and then still on its last frame.
     */
    public void pose(Sprite sheet, Action action, Direction direction, boolean loops) {
        poseSprite = sheet;
        pose = action;
        poseDirection = direction;
        poseLoops = loops;
        once = null;
        current = null; // from its first frame
    }

    /** Back to its own sheet: standing (or walking) as before. */
    public void endPose() {
        pose = null;
    }

    /** The pose it holds now, or null. */
    public Action getPose() {
        return pose;
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
