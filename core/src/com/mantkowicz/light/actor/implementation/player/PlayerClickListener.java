package com.mantkowicz.light.actor.implementation.player;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.camera.LookAtMeEvent;

public class PlayerClickListener extends ClickListener {
    private GameBoardActor boardGameActor;
    private GameEventService gameEventService;

    public PlayerClickListener(GameBoardActor boardGameActor, GameEventService gameEventService) {
        this.boardGameActor = boardGameActor;
        this.gameEventService = gameEventService;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        gameEventService.post(new LookAtMeEvent(boardGameActor));
    }
}