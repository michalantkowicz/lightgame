package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.LookAtMeEvent;

public class LookAtMeOnClickListener extends ClickListener {
    private final GameBoardActor actor;
    private final GameEventService gameEventService;

    public LookAtMeOnClickListener(GameBoardActor actor, GameEventService gameEventService) {
        this.actor = actor;
        this.gameEventService = gameEventService;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        gameEventService.post(new LookAtMeEvent(actor));
    }
}
