package com.mantkowicz.light.feature;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.GameActorDefinition;
import com.mantkowicz.light.event.AddToStageEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.AbstractStage;
import com.mantkowicz.light.ui.StageType;

import java.util.HashMap;
import java.util.Map;

public class StageFeature implements Feature {
    private final Map<StageType, Stage> stageMap = new HashMap<>();
    private final GameEventService eventService;

    public StageFeature(GameContext context) {
        this.eventService = context.getGameEventService();

        for (AbstractStage stage : context.getStages()) {
            stageMap.put(stage.getStageType(), stage);
        }
    }

    @Override
    public void run(float delta) {
        AddToStageEvent event = eventService.poll(AddToStageEvent.class);
        if (event != null) {
            GameActorDefinition builder = event.getDefinition();

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
