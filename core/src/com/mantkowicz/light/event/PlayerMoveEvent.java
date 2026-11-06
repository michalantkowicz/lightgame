package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.Player;

/**
 * Lives for one loop: everything interested in "the player started moving" checks it in that loop.
 */
public class PlayerMoveEvent extends GameEvent {
    private static final int MAX_AGE = 1;

    private final Player player;

    public PlayerMoveEvent(Player player) {
        super(MAX_AGE);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
