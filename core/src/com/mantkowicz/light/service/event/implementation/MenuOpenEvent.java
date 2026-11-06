package com.mantkowicz.light.service.event.implementation;

import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.service.event.GameEvent;

public class MenuOpenEvent extends GameEvent {
    private final GameBoardActor actor;

    public MenuOpenEvent(GameBoardActor actor) {
        this.actor = actor;
    }

    public GameBoardActor getActor() {
        return actor;
    }
}
