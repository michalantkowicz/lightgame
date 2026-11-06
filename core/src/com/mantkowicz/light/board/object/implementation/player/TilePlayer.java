package com.mantkowicz.light.board.object.implementation.player;

import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.board.object.TileActorObject;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.event.implementation.PlayerCreatedEvent;

import static com.mantkowicz.light.board.object.TileObjectType.PLAYER;

public class TilePlayer extends TileActorObject<Player> {
    public TilePlayer() {
        super(PLAYER);
    }

    @Override
    protected Player createActor(GameContext context) {
        return new Player(context);
    }

    @Override
    protected void prepareActor(Player player, GameContext context) {
        context.getGameEventService().post(new PlayerCreatedEvent(player));
    }
}
