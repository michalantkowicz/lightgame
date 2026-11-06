package com.mantkowicz.light.map;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.game.GameContext;

public abstract class TileObject {
    private final TileObjectType objectType;

    public TileObject(TileObjectType objectType) {
        this.objectType = objectType;
    }

    public abstract void prepare(Tile tile, GameContext context);

    public abstract boolean isAccessible();

    /**
     * Draws what the object shows on its tile, after all tile backgrounds. Most objects spawn actors instead and draw nothing here.
     */
    public void draw(Tile tile, Batch batch) {
    }
}
