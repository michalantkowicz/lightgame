package com.mantkowicz.light.board;

import java.util.List;

public class BoardPath {
    private List<Tile> path;

    public BoardPath(List<Tile> path) {
        this.path = path;
    }

    public List<Tile> getPathNodes() {
        return path;
    }

    public void setPath(List<Tile> path) {
        this.path = path;
    }

    public boolean isEmpty() {
        return path == null || path.isEmpty();
    }

    /**
     * Same as {@code !isEmpty()}.
     */
    public boolean exists() {
        return !isEmpty();
    }

    public Tile getLastNode() {
        if (path != null && !path.isEmpty()) {
            return path.get(path.size() - 1);
        }
        return null;
    }
}
