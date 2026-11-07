package com.mantkowicz.light.board;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.event.CollectEvent;
import com.mantkowicz.light.event.GameEventService;

import java.util.ArrayList;
import java.util.List;

public class BoardService {
    private static final float LINE_SAMPLE_STEP = 4f;

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

    /**
     * The tile under a stage point, or {@code null} where the map has no tile.
     */
    public Tile getTileAt(Vector2 point) {
        for (Tile tile : tiles) {
            if (tile.doesContainPoint(point)) {
                return tile;
            }
        }
        return null;
    }

    /**
     * Is the straight line between two stage points walkable for something {@code clearance} px wide on each side?
     * The line is checked every few px together with the corners of a square of that clearance around it, so it never
     * crosses or clips a missing or inaccessible tile.
     */
    public boolean isWalkableLine(Vector2 from, Vector2 to, float clearance) {
        int samples = Math.max(1, (int) Math.ceil(from.dst(to) / LINE_SAMPLE_STEP));
        Vector2 point = new Vector2();
        Vector2 probe = new Vector2();
        for (int i = 0; i <= samples; i++) {
            point.set(from).lerp(to, i / (float) samples);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Tile tile = getTileAt(probe.set(point.x + dx * clearance, point.y + dy * clearance));
                    if (tile == null || !tile.isAccessible()) {
                        return false;
                    }
                }
            }
        }
        return true;
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
