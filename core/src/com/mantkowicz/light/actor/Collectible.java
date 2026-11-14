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
     * The picture on its action button, for what the button does now: e.g. the open chest (opens it), or the closed
     * chest once it is open (closes it).
     */
    TextureRegion getActionIcon();

    /**
     * After opening it can be closed again and opened again, also empty (a chest): its button stays while the player
     * is next to it. Otherwise the button goes once nothing is left inside.
     */
    default boolean canBeClosed() {
        return false;
    }

    /** It is open now (a chest with its lid up): its button closes it. */
    default boolean isOpen() {
        return false;
    }

    /** It is opening or closing right now (a chest's lid moves): its button waits, faded a little. */
    default boolean isBusy() {
        return false;
    }

    /** How high over its actor's bottom it is drawn: its action button goes over that (e.g. over an open lid). */
    default float getTopY() {
        return getActor().getHeight();
    }

    /** What is drawn for it on the map: its action button sits over it. */
    Actor getActor();

    String getName();

    Tile getTile();
}
