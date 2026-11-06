package com.mantkowicz.light.service.event.implementation;

import com.mantkowicz.light.actor.GameActorDefinition;
import com.mantkowicz.light.service.event.GameEvent;

public class AddToStageEvent extends GameEvent {
    private final GameActorDefinition definition;

    public AddToStageEvent(GameActorDefinition definition) {
        this.definition = definition;
    }

    public GameActorDefinition getDefinition() {
        return definition;
    }
}
