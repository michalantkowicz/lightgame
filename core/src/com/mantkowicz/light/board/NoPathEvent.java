package com.mantkowicz.light.board;

import com.mantkowicz.light.event.GameEvent;

/**
 * The player clicked a tile nobody can walk to (water, a wall...): the game says so over it (a feature listens).
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
