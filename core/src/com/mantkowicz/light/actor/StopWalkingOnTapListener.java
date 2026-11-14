package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileTouchedEvent;
import com.mantkowicz.light.event.GameEventService;

/**
 * Tapping a walking actor (the player) stops it in the middle of the tile it is on, as if that tile was tapped. A tap
 * on the player's own tile lands on the player, not on the board, so without this it couldn't stop a walk.
 */
public class StopWalkingOnTapListener extends ClickListener {
    private final GameBoardActor actor;
    private final BoardService boardService;
    private final GameEventService gameEventService;

    public StopWalkingOnTapListener(GameBoardActor actor, BoardService boardService,
            GameEventService gameEventService) {
        this.actor = actor;
        this.boardService = boardService;
        this.gameEventService = gameEventService;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        if (!Status.MOVEMENT.equals(actor.getStatus())) {
            return;
        }
        Tile under = boardService.getTileAt(new Vector2(actor.getX(Align.center), actor.getY(Align.center)));
        gameEventService.post(new TileTouchedEvent(under != null ? under : actor.getTile()));
    }
}
