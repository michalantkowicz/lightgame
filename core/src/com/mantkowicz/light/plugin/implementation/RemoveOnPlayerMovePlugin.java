package com.mantkowicz.light.plugin.implementation;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.PlayerMoveEvent;

/**
 * Fades the actor out and removes it as soon as the player starts moving.
 * Only checks the event, never consumes it: the event dies by itself after one loop.
 */
public class RemoveOnPlayerMovePlugin implements Plugin {
    private static final float DURATION = 0.75f;

    private final GameActor actor;
    private final GameEventService gameEventService;

    public RemoveOnPlayerMovePlugin(GameActor actor, GameContext context) {
        this.actor = actor;
        this.gameEventService = context.getGameEventService();
    }

    @Override
    public void run(float delta) {
        if (gameEventService.has(PlayerMoveEvent.class)) {
            actor.setTouchable(Touchable.disabled);
            actor.addAction(Actions.sequence(
                    Actions.fadeOut(DURATION, Interpolation.sineOut),
                    Actions.removeActor()
            ));
        }
    }
}
