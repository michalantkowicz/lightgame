package com.mantkowicz.light.service.event.implementation.camera;

import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.service.event.GameEvent;

public class LookAtMeEvent extends GameEvent {
    private final GameBoardActor actor;

    public LookAtMeEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
