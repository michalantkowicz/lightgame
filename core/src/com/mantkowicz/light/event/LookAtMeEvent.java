package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.GameBoardActor;

public class LookAtMeEvent extends GameEvent {
    private final GameBoardActor actor;

    public LookAtMeEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
