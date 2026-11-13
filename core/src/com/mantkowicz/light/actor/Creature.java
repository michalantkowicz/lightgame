package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;

/**
 * An animal (later maybe a person) the player can care for, standing on a tile next to it: stroke it, or give it what
 * it wishes for. What it does about it is its own business (its state machine); the round button only asks and tells.
 */
public interface Creature extends TileOccupant {
    Tile getTile();

    /** Where it is drawn right now (it may be between two tiles). */
    Vector2 getCenter();

    /** What it wishes for right now (shown in a bubble over it), or null. An item of the same kind fulfils it. */
    Item getWish();

    /** Gives it what it wished for. */
    void give(Item item);

    /** Would it like to be stroked right now? */
    boolean canBeStroked();

    void stroke();
}
