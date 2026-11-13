package com.mantkowicz.light.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GameActorsByTile {
    private final Map<Tile, List<TileOccupant>> gameActorsByTile = new HashMap<>();

    GameActorsByTile(List<Tile> tiles) {
        for (Tile tile : tiles) {
            gameActorsByTile.put(tile, new ArrayList<TileOccupant>());
        }
    }

    void putGameActor(Tile tile, TileOccupant gameActor) {
        gameActorsByTile.get(tile).add(gameActor);
    }

    void removeGameActor(Tile tile, TileOccupant gameActor) {
        if (tileContainsGameActor(tile, gameActor)) {
            gameActorsByTile.get(tile).remove(gameActor);
        }
    }

    private boolean tileContainsGameActor(Tile tile, TileOccupant gameActor) {
        return gameActorsByTile.get(tile).contains(gameActor);
    }

    List<TileOccupant> getGameActors(Tile tile) {
        return gameActorsByTile.get(tile);
    }
}
