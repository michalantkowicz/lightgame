package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

/**
 * Lives for one loop: everything interested in "the player started moving" checks it in that loop. It carries the
 * moving actor (the player) as a {@link GameBoardActor}, so the plugins that walk it don't need to know the player.
 */
public class PlayerMoveEvent extends GameEvent {
    private static final int MAX_AGE = 1;

    private final GameBoardActor player;

    public PlayerMoveEvent(GameBoardActor player) {
        super(MAX_AGE);
        this.player = player;
    }

    public GameBoardActor getPlayer() {
        return player;
    }
}
