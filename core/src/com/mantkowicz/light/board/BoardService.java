package com.mantkowicz.light.board;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.event.GameEventService;

import java.util.ArrayList;
import java.util.List;

public class BoardService {
    private static final float LINE_SAMPLE_STEP = 4f;

    private final List<Tile> tiles;
    private final GameActorsByTile gameActorsByTile;
    private final GameEventService gameEventService;
    private BoardGraph boardGraph;
    private IndexedAStarPathFinder<Tile> pathFinder;
    private OccupantListener occupantListener;

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

    /**
     * Blocks or frees a floor tile while the game runs, for what blocks a tile and moves (a wandering cow blocks the
     * tile it steps onto and frees the one it left). Paths found from now on know about it.
     */
    public void setAccessible(Tile tile, boolean accessible) {
        if (!(tile instanceof FloorTile floor)) {
            throw new IllegalArgumentException("Only a floor tile can be blocked or freed");
        }
        floor.setAccessible(accessible);
        // the whole graph again: simple, and cheap for small maps (see BoardGraph for the numbers and the alternative)
        boardGraph = new BoardGraph(tiles);
        pathFinder = new IndexedAStarPathFinder<>(boardGraph);
    }

    /** Who is told when something steps onto a tile (the game screen sets it before anything is on the board). */
    public void setOccupantListener(OccupantListener occupantListener) {
        this.occupantListener = occupantListener;
    }

    /** Puts something on a tile and tells the listener. */
    public void registerOccupant(Tile tile, TileOccupant occupant) {
        gameActorsByTile.putGameActor(tile, occupant);
        if (occupantListener != null) {
            occupantListener.entered(occupant, tile);
        }
    }

    /**
     * Whether the tile holds something used from a side neighbour (a chest): clicking it means "walk next to it".
     */
    public boolean isUsedFromNeighbour(Tile tile) {
        for (TileOccupant occupant : gameActorsByTile.getGameActors(tile)) {
            if (occupant.isUsedFromNeighbour()) {
                return true;
            }
        }
        return false;
    }

    /** What is on a tile right now (the player, a dog, items lying there...), as a copy. */
    public List<TileOccupant> getOccupants(Tile tile) {
        return new ArrayList<>(gameActorsByTile.getGameActors(tile));
    }

    public void unregisterOccupant(Tile tile, TileOccupant occupant) {
        gameActorsByTile.removeGameActor(tile, occupant);
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
