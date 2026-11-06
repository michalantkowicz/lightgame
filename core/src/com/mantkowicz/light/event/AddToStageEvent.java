package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.GameActorDefinition;

public class AddToStageEvent extends GameEvent {
    private final GameActorDefinition definition;

    public AddToStageEvent(GameActorDefinition definition) {
        this.definition = definition;
    }

    public GameActorDefinition getDefinition() {
        return definition;
    }
}
