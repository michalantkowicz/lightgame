package com.mantkowicz.light.board;

import com.mantkowicz.light.actor.GameActor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GameActorsByTile {
    private final Map<Tile, List<GameActor>> gameActorsByTile = new HashMap<>();

    GameActorsByTile(List<Tile> tiles) {
        for (Tile tile : tiles) {
            gameActorsByTile.put(tile, new ArrayList<GameActor>());
        }
    }

    void putGameActor(Tile tile, GameActor gameActor) {
        gameActorsByTile.get(tile).add(gameActor);
    }

    void removeGameActor(Tile tile, GameActor gameActor) {
        if (tileContainsGameActor(tile, gameActor)) {
            gameActorsByTile.get(tile).remove(gameActor);
        }
    }

    private boolean tileContainsGameActor(Tile tile, GameActor gameActor) {
        return gameActorsByTile.get(tile).contains(gameActor);
    }

    List<GameActor> getGameActors(Tile tile) {
        return gameActorsByTile.get(tile);
    }
}
