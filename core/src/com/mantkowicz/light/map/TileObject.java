package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.context.GameContext;

/**
 * A map object on a tile: what it is ({@link TileObjectType}) and how it comes to life once the game world exists.
 */
public abstract class TileObject implements TileContent {
    private final TileObjectType objectType;

    public TileObject(TileObjectType objectType) {
        this.objectType = objectType;
    }

    public TileObjectType getObjectType() {
        return objectType;
    }

    public abstract void prepare(Tile tile, GameContext context);

    @Override
    public abstract boolean isAccessible();
}
