package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

public class LookAtMeEvent extends GameEvent {
    private final GameBoardActor actor;

    public LookAtMeEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
