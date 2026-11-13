package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.mantkowicz.light.board.TileTouchedEvent;
import com.mantkowicz.light.event.GameEventService;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;

/**
 * A short tap on the actor works like a tap on its tile: the player walks there, or next to it when the actor is used
 * from a neighbouring tile (a cow, see {@link com.mantkowicz.light.board.TileOccupant#isUsedFromNeighbour()}). A hold
 * is left to {@link OpenMenuOnHoldListener}, which opens the actor's menu.
 */
public class WalkNextToOnTapListener extends InputListener {
    /** A press shorter than this is a tap (as long as {@link OpenMenuOnHoldListener} waits before opening the menu). */
    private static final float TAP_SECONDS = .5f;

    private final GameBoardActor actor;
    private final GameEventService gameEventService;
    private Action holding;
    private boolean held;

    public WalkNextToOnTapListener(GameBoardActor actor, GameEventService gameEventService) {
        this.actor = actor;
        this.gameEventService = gameEventService;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        held = false;
        holding = delay(TAP_SECONDS, run(() -> held = true));
        actor.addAction(holding);
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        actor.removeAction(holding);
        if (!held && actor.getTile() != null) {
            gameEventService.post(new TileTouchedEvent(actor.getTile()));
        }
    }
}
