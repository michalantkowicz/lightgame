package com.mantkowicz.light.event;

import com.mantkowicz.light.board.Tile;

/**
 * The player clicked a tile nobody can walk to (water, a wall...): {@link com.mantkowicz.light.feature.NoPathFeature}
 * says so over it.
 */
public class NoPathEvent extends GameEvent {
    private final Tile tile;

    public NoPathEvent(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }
}
