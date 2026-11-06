package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.Player;

public class PlayerCreatedEvent extends GameEvent {
    private final Player player;

    public PlayerCreatedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
