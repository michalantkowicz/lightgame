package com.mantkowicz.light.event;

import com.mantkowicz.light.board.Tile;

public class TileTouchedEvent extends GameEvent {
    private final Tile tile;

    public TileTouchedEvent(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }
}
