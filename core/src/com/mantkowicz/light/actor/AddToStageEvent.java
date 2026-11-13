package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

public class AddToStageEvent extends GameEvent {
    private final GameActorDefinition definition;

    public AddToStageEvent(GameActorDefinition definition) {
        this.definition = definition;
    }

    public GameActorDefinition getDefinition() {
        return definition;
    }
}
