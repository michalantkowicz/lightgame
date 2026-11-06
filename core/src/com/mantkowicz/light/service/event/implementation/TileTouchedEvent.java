package com.mantkowicz.light.service.event.implementation;

import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.service.event.GameEvent;

public class TileTouchedEvent extends GameEvent {
    private final Tile tile;

    public TileTouchedEvent(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }
}
