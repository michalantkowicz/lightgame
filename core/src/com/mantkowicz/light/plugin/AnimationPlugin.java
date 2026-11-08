package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.resources.ResourcesService;

import static com.mantkowicz.light.actor.Status.MOVEMENT;

/**
 * Animates an actor's picture from its sprite sheet: "walk_&lt;direction&gt;" while it moves (e.g. "rabbit/walk_left"),
 * and when it stops it turns back to the default "idle_down". The direction follows the way the actor actually moves;
 * on a diagonal it keeps the direction it had if that one still fits, so it doesn't flicker between two.
 */
public class AnimationPlugin implements Plugin {
    static final String DOWN = "down", UP = "up", LEFT = "left", RIGHT = "right";
    /** How much one axis must dominate to switch the direction to it. */
    private static final float DOMINANCE = 1.2f;

    private final GameActor actor;
    private final ResourcesService resourcesService;
    private final String sheet;
    private Vector2 lastCenter;
    private String direction = DOWN;
    private String current;
    private Animation<TextureRegion> animation;
    private float stateTime;

    public AnimationPlugin(GameActor actor, ResourcesService resourcesService, String sheet) {
        this.actor = actor;
        this.resourcesService = resourcesService;
        this.sheet = sheet;
    }

    @Override
    public void run(float delta) {
        Image avatar = actor.getAvatar();
        if (avatar == null) {
            return;
        }
        Vector2 center = actor.getCenter();
        boolean walking = MOVEMENT.equals(actor.getStatus());
        if (!walking) {
            direction = DOWN; // standing: the default pose, facing the player
        } else if (lastCenter != null) {
            updateDirection(center.x - lastCenter.x, center.y - lastCenter.y);
        }
        lastCenter = center;

        String name = sheet + "/" + (walking ? "walk_" : "idle_") + direction;
        if (!name.equals(current)) {
            current = name;
            animation = resourcesService.getAnimation(name);
            stateTime = 0;
        } else {
            stateTime += delta;
        }
        ((TextureRegionDrawable) avatar.getDrawable()).setRegion(animation.getKeyFrame(stateTime));
    }

    private void updateDirection(float dx, float dy) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        if (ax < 0.001f && ay < 0.001f) {
            return;
        }
        String horizontal = dx > 0 ? RIGHT : LEFT;
        String vertical = dy > 0 ? UP : DOWN;
        if (ax > ay * DOMINANCE) {
            direction = horizontal;
        } else if (ay > ax * DOMINANCE) {
            direction = vertical;
        } else if (!direction.equals(horizontal) && !direction.equals(vertical)) {
            direction = horizontal;
        }
    }

    /**
     * The direction the actor faces: "down", "up", "left" or "right".
     */
    public String getDirection() {
        return direction;
    }

    /**
     * The animation being played, e.g. "rabbit/walk_left".
     */
    public String getCurrentAnimation() {
        return current;
    }
}
