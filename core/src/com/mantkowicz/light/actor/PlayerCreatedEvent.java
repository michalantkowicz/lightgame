package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

public class PlayerCreatedEvent extends GameEvent {
    private final Player player;

    public PlayerCreatedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
