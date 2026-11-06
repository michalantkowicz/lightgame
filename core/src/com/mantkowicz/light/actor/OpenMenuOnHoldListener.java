package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.MenuCloseEvent;
import com.mantkowicz.light.service.event.implementation.MenuOpenEvent;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;

public class OpenMenuOnHoldListener extends InputListener {
    private final GameBoardActor gameBoardActor;
    private final GameEventService gameEventService;
    private DelayAction menuOpenAction;

    public OpenMenuOnHoldListener(GameBoardActor gameBoardActor, GameEventService gameEventService) {
        this.gameBoardActor = gameBoardActor;
        this.gameEventService = gameEventService;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        menuOpenAction = delay(.5f, new MenuOpenAction());
        gameBoardActor.addAction(menuOpenAction);
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        super.touchUp(event, x, y, pointer, button);
        gameBoardActor.removeAction(menuOpenAction);
        gameEventService.post(new MenuCloseEvent(new Vector2(x, y)));
    }

    private class MenuOpenAction extends Action {
        @Override
        public boolean act(float delta) {
            gameEventService.post(new MenuOpenEvent(gameBoardActor));
            return true;
        }
    }
}
