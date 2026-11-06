package com.mantkowicz.light.service.event.implementation;

import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.service.event.GameEvent;

public class PlayerCreatedEvent extends GameEvent {
    private final Player player;

    public PlayerCreatedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
