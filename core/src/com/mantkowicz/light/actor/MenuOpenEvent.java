package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

public class MenuOpenEvent extends GameEvent {
    private final GameBoardActor actor;

    public MenuOpenEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
