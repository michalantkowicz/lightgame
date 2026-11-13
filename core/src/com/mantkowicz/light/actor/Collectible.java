package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.resources.Thumbnail;

public interface Collectible extends Thumbnail, InventoryOwner, TileOccupant {
    Collectible DEFAULT = null;

    /**
     * Defines whether the collectible should collide with neighbour tiles
     */
    boolean isDistant();

    /** A distant collectible (a chest) is used from a side neighbour: clicking its tile walks next to it. */
    @Override
    default boolean isUsedFromNeighbour() {
        return isDistant();
    }

    /**
     * It is opened, just before what is inside comes out (e.g. a chest lifts its lid).
     */
    void beforeCollect();

    /**
     * The collect window for this collectible closes, with OK or CANCEL (e.g. a chest shuts its lid).
     */
    void afterCollect();

    /**
     * The picture on the action button that collects from it, e.g. the open chest.
     */
    TextureRegion getActionIcon();

    /** What is drawn for it on the map: its action button sits over it. */
    Actor getActor();

    String getName();

    Tile getTile();
}
