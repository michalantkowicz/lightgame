package com.mantkowicz.light.actor;

import com.mantkowicz.light.stage.StageType;

public interface GameActorDefinition<T extends GameActor> {
    T getActor();

    StageType getTargetStageType();
}
