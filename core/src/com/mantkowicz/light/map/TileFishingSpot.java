package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;

/**
 * A place to fish from, put on a tile in Tiled (objectClass FISHING_SPOT): standing on it, the rabbit has the fishing
 * rod on the HUD's tool button and casts into the water two tiles to its left. It is only a mark: the fishing comes
 * with {@code feature.FishingFeature} (switched off with the feature, it does nothing). It blocks nothing, and no grass
 * grows on it ({@link #keepsClear()}).
 */
public class TileFishingSpot extends TileObject {
    public TileFishingSpot() {
        super(TileObjectType.FISHING_SPOT);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        // nothing: the fishing feature finds it
    }

    @Override
    public boolean keepsClear() {
        return true; // no grass on a fishing spot
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
