package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;

import java.util.function.BooleanSupplier;

/**
 * Fades the actor out and removes it once it is no longer available (e.g. a COLLECT button when the player leaves the
 * chest's reach). Once leaving, it stays leaving: a new actor takes its place if it becomes available again.
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
        if (!leaving && !available.getAsBoolean()) {
            leaving = true;
            actor.setTouchable(Touchable.disabled);
            actor.addAction(Actions.sequence(
                    Actions.fadeOut(DURATION, Interpolation.sineOut),
                    Actions.removeActor()
            ));
        }
    }

    public boolean isLeaving() {
        return leaving;
    }
}
