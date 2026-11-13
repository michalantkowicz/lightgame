package com.mantkowicz.light.ui.menu;

import com.mantkowicz.light.actor.AddToStageEvent;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Interaction;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.ThumbnailType;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

public class DescriptionInteraction implements Interaction {
    private final GameEventService eventService;
    private final DescriptionDefinition descriptionDefinition;
    private final GameBoardActor actor;

    public DescriptionInteraction(GameBoardActor actor, GameContext context, String description) {
        this.eventService = context.getGameEventService();
        this.actor = actor;

        descriptionDefinition = new DescriptionDefinition();
        descriptionDefinition.setBackground(UiStyle.stretching(context.getResourcesService(), UiImage.DIALOG_BOX));
        descriptionDefinition.setFont(context.getResourcesService().getFont());
        descriptionDefinition.setDescription(description);
        descriptionDefinition.setParent(actor);
    }

    @Override
    public void interact() {
        eventService.post(new AddToStageEvent(descriptionDefinition));
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
