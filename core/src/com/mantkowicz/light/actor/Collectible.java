package com.mantkowicz.light.actor;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.resources.Thumbnail;

public interface Collectible extends Thumbnail, InventoryOwner {
    Collectible DEFAULT = null;

    /**
     * Defines whether the collectible should collide with neighbour tiles
     */
    boolean isDistant();

    void beforeCollect();

    void afterCollect();

    String getName();

    Tile getTile();
}
