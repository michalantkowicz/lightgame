package com.mantkowicz.light.board;

import com.mantkowicz.light.event.GameEvent;

public class TileTouchedEvent extends GameEvent {
    private final Tile tile;

    public TileTouchedEvent(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }
}
