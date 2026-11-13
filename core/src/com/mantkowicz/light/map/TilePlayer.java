package com.mantkowicz.light.map;

import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.character.PlayerCreatedEvent;
import com.mantkowicz.light.context.GameContext;

import static com.mantkowicz.light.map.TileObjectType.PLAYER;

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
