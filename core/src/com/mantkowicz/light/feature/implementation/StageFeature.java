package com.mantkowicz.light.feature.implementation;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.GameActorDefinition;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.AddToStageEvent;
import com.mantkowicz.light.stage.AbstractStage;
import com.mantkowicz.light.stage.StageType;

import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.service.event.GameEventType.ADD_TO_STAGE;

public class StageFeature implements Feature {
    private final Map<StageType, Stage> stageMap = new HashMap<>();
    private GameEventService eventService;

    public StageFeature(GameContext context) {
        this.eventService = context.getGameEventService();

        for (AbstractStage stage : context.getStages()) {
            stageMap.put(stage.getStageType(), stage);
        }
    }

    @Override
    public void run(float delta) {
        if (eventService.containsEvent(ADD_TO_STAGE)) {
            AddToStageEvent event = eventService.removeEventFromQueue(ADD_TO_STAGE, AddToStageEvent.class);
            GameActorDefinition builder = event.getEventObject();

            StageType targetStageType = builder.getTargetStageType();
            if (stageMap.containsKey(targetStageType)) {
                GameActor actor = builder.getActor();
                stageMap.get(targetStageType).addActor(actor);
            } else {
                throw new IllegalArgumentException("There is no stage registered for the stage type: [" + targetStageType + "]");
            }
        }
    }
}
