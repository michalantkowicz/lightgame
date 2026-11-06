package com.mantkowicz.light.board.object.implementation.player;

import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.event.implementation.PlayerCreatedEvent;

import static com.mantkowicz.light.board.object.TileObjectType.PLAYER;

public class TilePlayer extends TileObject {
    public TilePlayer() {
        super(PLAYER);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        Player player = new Player(context);
        player.setTile(tile);

        context.getGameStage().addActor(player);

        context.getGameEventService().post(new PlayerCreatedEvent(player));
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
