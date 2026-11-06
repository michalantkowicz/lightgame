package com.mantkowicz.light.actor;

import com.mantkowicz.light.ui.StageType;

public interface GameActorDefinition<T extends GameActor> {
    T getActor();

    StageType getTargetStageType();
}
