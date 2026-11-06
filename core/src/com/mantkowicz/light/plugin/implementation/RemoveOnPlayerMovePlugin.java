package com.mantkowicz.light.plugin.implementation;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.PlayerMoveEvent;


public class RemoveOnPlayerMovePlugin implements Plugin {
    private final static float DURATION = 0.75f;

    private GameEventService gameEventService;
    private GameActor actor;

    public RemoveOnPlayerMovePlugin(GameEventService gameEventService, GameActor actor) {
        this.gameEventService = gameEventService;
        this.actor = actor;
    }

    @Override
    public void run(float delta) {
        if (gameEventService.has(PlayerMoveEvent.class)) {
            if (actor != null) {
                actor.setTouchable(Touchable.disabled);
                actor.addAction(Actions.sequence(
                        Actions.fadeOut(DURATION, Interpolation.sineOut),
                        getActorRemoveAction(actor)
                ));
            }
        }
    }

    private static Action getActorRemoveAction(final GameActor gameActor) {
        return new Action() {
            @Override
            public boolean act(float delta) {
                gameActor.remove();
                return true;
            }
        };
    }
}
