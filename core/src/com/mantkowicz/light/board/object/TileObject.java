package com.mantkowicz.light.board.object;

import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;

public abstract class TileObject {
    private TileObjectType objectType;

    public TileObject(TileObjectType objectType) {
        this.objectType = objectType;
    }

    public abstract void prepare(Tile tile, GameContext context);

    public abstract boolean isAccessible();
}
