package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.board.BoardPath;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.TileTouchedEvent;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.actor.Status.IDLE;
import static com.mantkowicz.light.actor.Status.MOVEMENT;
import static com.mantkowicz.light.board.TilePoint.CENTER;

/**
 * Walks the player tile by tile to the touched tile. A new touch is only taken while the player is idle,
 * which happens between two steps, so a walk can be re-routed at the next tile.
 */
public class BoardMovementPlugin implements Plugin {
    private final Player player;
    private final GameEventService gameEventService;
    private final BoardService boardService;
    private final float speed;

    public BoardMovementPlugin(Player player, GameContext context, float speed) {
        this.player = player;
        this.gameEventService = context.getGameEventService();
        this.boardService = context.getBoardService();
        this.speed = speed;
    }

    @Override
    public void run(float delta) {
        if (IDLE.equals(player.getStatus())) {
            TileTouchedEvent gameEvent = gameEventService.poll(TileTouchedEvent.class);
            if (gameEvent != null) {
                player.addAction(getMoveByPathAction(gameEvent));
            }
        }
    }

    private SequenceAction getMoveByPathAction(TileTouchedEvent gameEvent) {
        player.clearActions();

        SequenceAction sequence = Actions.sequence();
        BoardPath path = boardService.getPath(player.getTile(), gameEvent.getTile());

        Tile previous = player.getTile();
        for (Tile pathNode : path.getPathNodes()) {
            Vector2 position = pathNode.calculatePositionForActorAt(player, CENTER);
            // a diagonal step is longer, so it takes longer: the walking speed stays the same in every direction
            float duration = speed * previous.getCenter().dst(pathNode.getCenter()) / previous.getHeight();
            previous = pathNode;

            SequenceAction moveToTileAction = Actions.sequence(
                    Actions.run(() -> player.setStatus(MOVEMENT)),
                    Actions.moveTo(position.x, position.y, duration, Interpolation.sineOut),
                    Actions.run(() -> player.setTile(pathNode)),
                    Actions.run(() -> player.setStatus(IDLE))
            );
            sequence.addAction(moveToTileAction);
        }
        return sequence;
    }
}
