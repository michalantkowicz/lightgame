package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.map.TileDoor;

/**
 * Takes whoever went through a door to the map it leads to (the game screen, which has all the maps).
 */
public interface Travel {
    /**
     * @return false when it can't go now: no room on the other side, or the player is on its way already
     */
    boolean go(GameBoardActor traveller, TileDoor door);
}
