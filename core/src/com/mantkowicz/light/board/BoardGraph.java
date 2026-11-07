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
 * The board as a gdx-ai graph over the square grid's 8 neighbours. A step to a side neighbour costs 1, a diagonal step costs √2
 * (its real length in tiles). A diagonal step is only allowed when both tiles it squeezes between are accessible, so paths never cut
 * the corner of furniture or walls. Nothing leaves an inaccessible tile.
 * Connections are built once, because tile accessibility is fixed when the map is loaded.
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
                        float cost = tile.getCenter().dst(neighbour.getCenter()) / stepLength;
                        tileConnections.add(new Step(tile, neighbour, cost));
                    }
                }
            }
            connections.put(tile, tileConnections);
        }
        // straight-line distance in steps; never more than the real cost, so A* still finds a shortest path
        heuristic = (node, endNode) -> stepLength == 0 ? 0 : node.getCenter().dst(endNode.getCenter()) / stepLength;
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

    /** A connection with its own cost (gdx-ai's DefaultConnection always costs 1). */
    private static final class Step extends DefaultConnection<Tile> {
        private final float cost;

        Step(Tile from, Tile to, float cost) {
            super(from, to);
            this.cost = cost;
        }

        @Override
        public float getCost() {
            return cost;
        }
    }
}
