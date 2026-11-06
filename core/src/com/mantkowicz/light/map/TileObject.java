package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.game.GameContext;

public abstract class TileObject {
    private final TileObjectType objectType;

    public TileObject(TileObjectType objectType) {
        this.objectType = objectType;
    }

    public abstract void prepare(Tile tile, GameContext context);

    public abstract boolean isAccessible();
}
