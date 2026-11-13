package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Plugin;

import java.util.function.BooleanSupplier;

/**
 * Fades the actor out and removes it once it is no longer available (e.g. a COLLECT button when the player leaves the
 * chest's reach). If it becomes available again while fading out, it fades back in and stays.
 * <p>
 * It stays touchable while fading, so a click on it never falls through to whatever is under it (e.g. the board, which
 * would walk the player there); the actor's own listener decides whether the click does anything.
 */
public class RemoveWhenUnavailablePlugin implements Plugin {
    private static final float DURATION = 0.75f;

    private final GameActor actor;
    private final BooleanSupplier available;
    private boolean leaving;

    public RemoveWhenUnavailablePlugin(GameActor actor, BooleanSupplier available) {
        this.actor = actor;
        this.available = available;
    }

    @Override
    public void run(float delta) {
        boolean isAvailable = available.getAsBoolean();
        if (!leaving && !isAvailable) {
            leaving = true;
            actor.clearActions();
            actor.addAction(Actions.sequence(
                    Actions.fadeOut(DURATION, Interpolation.sineOut),
                    Actions.removeActor()));
        } else if (leaving && isAvailable) {
            leaving = false;
            actor.clearActions();
            actor.addAction(Actions.fadeIn(DURATION * (1 - actor.getColor().a), Interpolation.sineIn));
        }
    }
}
