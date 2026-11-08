package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.GameActor;

import static com.mantkowicz.light.actor.Status.MOVEMENT;

/**
 * Makes an actor hop while it walks: its picture rises and falls in a small arc, one arc every {@code hopDuration} seconds.
 * Only the picture moves, so the actor's position, its tile and everything that follows it (camera, collisions) are unaffected.
 * When the walk ends the actor lands: if it is still rising, it comes straight down from where it is (no extra hop after stopping).
 */
public class HopPlugin implements Plugin {
    private final GameActor actor;
    private final float hopHeight;
    private final float hopDuration;
    /** Progress of the current hop: 0 is on the ground, 0.5 the top of the arc, 1 landed again. */
    private float progress;

    public HopPlugin(GameActor actor, float hopHeight, float hopDuration) {
        this.actor = actor;
        this.hopHeight = hopHeight;
        this.hopDuration = hopDuration;
    }

    @Override
    public void run(float delta) {
        Image avatar = actor.getAvatar();
        if (avatar == null) {
            return;
        }
        boolean walking = MOVEMENT.equals(actor.getStatus());
        if (!walking && progress > 0 && progress < 0.5f) {
            progress = 1 - progress; // same height, but on the way down
        }
        if (walking || progress > 0) {
            progress += delta / hopDuration;
            if (progress >= 1) {
                progress = walking ? progress - 1 : 0;
            }
        }
        avatar.setY(actor.getAvatarRestY() + hopHeight * MathUtils.sin(MathUtils.PI * progress));
    }
}
