package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.MenuCloseEvent;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;

/**
 * Holding the actor for half a second opens its menu; releasing closes it.
 */
public class OpenMenuOnHoldListener extends InputListener {
    private static final float HOLD_DURATION = .5f;

    private final GameBoardActor actor;
    private final GameEventService gameEventService;
    private DelayAction menuOpenAction;

    public OpenMenuOnHoldListener(GameBoardActor actor, GameEventService gameEventService) {
        this.actor = actor;
        this.gameEventService = gameEventService;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        menuOpenAction = delay(HOLD_DURATION, run(() -> gameEventService.post(new MenuOpenEvent(actor))));
        actor.addAction(menuOpenAction);
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        super.touchUp(event, x, y, pointer, button);
        actor.removeAction(menuOpenAction);
        gameEventService.post(new MenuCloseEvent(new Vector2(x, y)));
    }
}
