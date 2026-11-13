package com.mantkowicz.light.board;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The board as a gdx-ai graph over the square grid's 8 neighbours. Every step costs 1, a diagonal one too, so a path's cost is its
 * number of steps. A diagonal step is only allowed when both tiles it squeezes between are accessible, so paths never cut the corner
 * of furniture or walls. Nothing leaves an inaccessible tile.
 * <p>
 * A graph never changes: its connections are built in the constructor from what is accessible then. When a tile gets
 * blocked or freed during the game (a wandering cow), {@link BoardService#setAccessible} throws the graph away and
 * builds a new one for the whole map. That is the simple and safe way, and cheap while maps are small: 0.2 ms for
 * 24x16 tiles, 0.8 ms for 50x50, 2.2 ms for 100x100 (measured on a desktop JVM; expect several times more on a phone
 * or in the browser), twice per step of each creature. If maps or herds grow, rebuild only what a tile can change:
 * the connections leaving that tile and its 8 neighbours (a diagonal step between two of its neighbours depends on
 * it too).
 */
public class BoardGraph implements IndexedGraph<Tile> {
    private final Map<Tile, Integer> indices = new HashMap<>();
    private final Map<Tile, Array<Connection<Tile>>> connections = new HashMap<>();
    private final Heuristic<Tile> heuristic;

    public BoardGraph(List<Tile> tiles) {
        float stepLength = shortestStep(tiles);
        for (Tile tile : tiles) {
            indices.put(tile, indices.size());
            Array<Connection<Tile>> tileConnections = new Array<>();
            if (tile.isAccessible()) {
                for (Tile neighbour : tile.getNeighbours()) {
                    if (neighbour.isAccessible() && !cutsCorner(tile, neighbour, stepLength)) {
                        tileConnections.add(new DefaultConnection<>(tile, neighbour));
                    }
                }
            }
            connections.put(tile, tileConnections);
        }
        // the fewest steps when diagonals cost 1 (Chebyshev distance); never more than the real cost, so A* still finds a shortest path
        heuristic = (node, endNode) -> {
            if (stepLength == 0) {
                return 0;
            }
            Vector2 from = node.getCenter();
            Vector2 to = endNode.getCenter();
            return Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y)) / stepLength;
        };
    }

    /** Distance between the centres of side neighbours (the tile size). */
    private static float shortestStep(List<Tile> tiles) {
        float shortest = 0;
        for (Tile tile : tiles) {
            for (Tile neighbour : tile.getNeighbours()) {
                float distance = tile.getCenter().dst(neighbour.getCenter());
                if (shortest == 0 || distance < shortest) {
                    shortest = distance;
                }
            }
        }
        return shortest;
    }

    /**
     * A diagonal step passes between the two tiles that touch both ends. It cuts a corner when one of them is missing or blocked.
     */
    private static boolean cutsCorner(Tile from, Tile to, float stepLength) {
        boolean diagonal = from.getCenter().dst(to.getCenter()) > stepLength * 1.01f;
        if (!diagonal) {
            return false;
        }
        int openSides = 0;
        for (Tile side : from.getNeighbours()) {
            boolean touchesBoth = to.getNeighbours().contains(side)
                    && side.getCenter().dst(from.getCenter()) <= stepLength * 1.01f
                    && side.getCenter().dst(to.getCenter()) <= stepLength * 1.01f;
            if (touchesBoth && side.isAccessible()) {
                openSides++;
            }
        }
        return openSides < 2;
    }

    public Heuristic<Tile> getHeuristic() {
        return heuristic;
    }

    @Override
    public int getIndex(Tile node) {
        return indices.get(node);
    }

    @Override
    public int getNodeCount() {
        return indices.size();
    }

    @Override
    public Array<Connection<Tile>> getConnections(Tile fromNode) {
        return connections.get(fromNode);
    }
}
