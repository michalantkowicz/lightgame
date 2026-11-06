package com.mantkowicz.light.board;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The board as a gdx-ai graph: every step between two accessible neighbouring tiles costs 1, and nothing leaves an inaccessible tile.
 * Connections are built once, because tile accessibility is fixed when the map is loaded.
 */
public class BoardGraph implements IndexedGraph<Tile> {
    private final Map<Tile, Integer> indices = new HashMap<>();
    private final Map<Tile, Array<Connection<Tile>>> connections = new HashMap<>();
    private final Heuristic<Tile> heuristic;

    public BoardGraph(List<Tile> tiles) {
        float longestStep = 0;
        for (Tile tile : tiles) {
            indices.put(tile, indices.size());
            Array<Connection<Tile>> tileConnections = new Array<>();
            if (tile.isAccessible()) {
                for (Tile neighbour : tile.getNeighbours()) {
                    if (neighbour.isAccessible()) {
                        tileConnections.add(new DefaultConnection<>(tile, neighbour));
                    }
                    longestStep = Math.max(longestStep, tile.getCenter().dst(neighbour.getCenter()));
                }
            }
            connections.put(tile, tileConnections);
        }
        float stepLength = longestStep;
        // straight-line distance in steps; never more than the real number of steps, so A* still finds a shortest path
        heuristic = (node, endNode) -> stepLength == 0 ? 0 : node.getCenter().dst(endNode.getCenter()) / stepLength;
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
