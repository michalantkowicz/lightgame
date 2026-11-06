package com.mantkowicz.light.board.service;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.board.path.BoardGraph;
import com.mantkowicz.light.board.path.BoardPath;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.CollectEvent;

import java.util.ArrayList;
import java.util.List;

public class BoardService {
    private final List<Tile> tiles;
    private final GameActorsByTile gameActorsByTile;
    private final GameEventService gameEventService;
    private final BoardGraph boardGraph;
    private final IndexedAStarPathFinder<Tile> pathFinder;

    public BoardService(GameEventService gameEventService, List<Tile> tiles) {
        this.gameEventService = gameEventService;
        this.tiles = tiles;

        this.gameActorsByTile = new GameActorsByTile(tiles);
        this.boardGraph = new BoardGraph(tiles);
        this.pathFinder = new IndexedAStarPathFinder<>(boardGraph);
    }

    public void registerGameActor(Tile tile, GameActor gameActor) {
        gameActorsByTile.putGameActor(tile, gameActor);
        checkCollisions(tile, gameActor);
    }

    private void checkCollisions(Tile tile, GameActor gameActor) {
        for (GameActor tileActor : gameActorsByTile.getGameActors(tile)) {
            if (!tileActor.equals(gameActor) && tileActor instanceof Collectible) {
                gameEventService.post(new CollectEvent((Collectible) tileActor));
            }
        }
        for (Tile neighbour : tile.getNeighbours()) {
            for (GameActor tileActor : gameActorsByTile.getGameActors(neighbour)) {
                if (!tileActor.equals(gameActor) && tileActor instanceof Collectible && ((Collectible) tileActor).isDistant()) {
                    gameEventService.post(new CollectEvent((Collectible) tileActor));
                }
            }
        }
    }

    public void unregisterGameActor(Tile tile, GameActor gameActor) {
        gameActorsByTile.removeGameActor(tile, gameActor);
    }

    /**
     * A shortest path (A*) from the start to the end tile, without the start tile. Empty when the end can't be reached or is the start.
     */
    public BoardPath getPath(Tile startTile, Tile endTile) {
        DefaultGraphPath<Tile> graphPath = new DefaultGraphPath<>();
        List<Tile> result = new ArrayList<>();
        if (pathFinder.searchNodePath(startTile, endTile, boardGraph.getHeuristic(), graphPath)) {
            for (int i = 1; i < graphPath.getCount(); i++) {
                result.add(graphPath.get(i));
            }
        }
        return new BoardPath(result);
    }
}
