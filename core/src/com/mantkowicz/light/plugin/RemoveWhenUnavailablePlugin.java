package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;

import java.util.function.BooleanSupplier;

/**
 * Fades the actor out and removes it once it is no longer available (e.g. a COLLECT button when the player leaves the
 * chest's reach). If it becomes available again while fading out, it fades back in and stays.
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
            actor.setTouchable(Touchable.disabled);
            actor.clearActions();
            actor.addAction(Actions.sequence(
                    Actions.fadeOut(DURATION, Interpolation.sineOut),
                    Actions.removeActor()
            ));
        } else if (leaving && isAvailable) {
            leaving = false;
            actor.setTouchable(Touchable.enabled);
            actor.clearActions();
            actor.addAction(Actions.fadeIn(DURATION * (1 - actor.getColor().a), Interpolation.sineIn));
        }
    }
}
