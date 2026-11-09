package com.mantkowicz.light.actor;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.resources.Thumbnail;

public interface Collectible extends Thumbnail, InventoryOwner {
    Collectible DEFAULT = null;

    /**
     * Defines whether the collectible should collide with neighbour tiles
     */
    boolean isDistant();

    /**
     * The collect window for this collectible opens (e.g. a chest opens its lid).
     */
    void beforeCollect();

    /**
     * The collect window for this collectible closes, with OK or CANCEL (e.g. a chest shuts its lid).
     */
    void afterCollect();

    String getName();

    Tile getTile();
}
