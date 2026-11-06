package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.GameBoardActor;

public class MenuOpenEvent extends GameEvent {
    private final GameBoardActor actor;

    public MenuOpenEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
