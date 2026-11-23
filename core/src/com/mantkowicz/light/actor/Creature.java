package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;

/**
 * An animal (later maybe a person) the player can care for, standing on a tile next to it: stroke it, give it its
 * favourite thing, start and finish its quests (a farm animal's). What it does about it is its own business (its state machine); the
 * action buttons over it only ask and tell.
 */
public interface Creature extends TileOccupant {
    Tile getTile();

    /** Where it is drawn right now (it may be between two tiles). */
    Vector2 getCenter();

    /** Its favourite thing (one of a kind: any carrot will do): the player can give it one when it can be fed. */
    Item getFavourite();

    /** Can it be given something now (its favourite, or what its quest waits for)? */
    boolean canBeFed();

    /** Gives it its favourite thing, or what its quest waited for: it eats it, happy. */
    void give(Item item);

    /** Would it like to be stroked right now? */
    boolean canBeStroked();

    void stroke();

    /** What is drawn for it on the map: the action buttons sit over it. */
    Actor getActor();

    /** Shows or hides the bubble with the quest it offers (hidden while the button with it is over it). */
    void setOfferShown(boolean shown);
}
