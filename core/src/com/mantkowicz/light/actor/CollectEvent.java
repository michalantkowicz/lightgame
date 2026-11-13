package com.mantkowicz.light.actor;

import com.mantkowicz.light.event.GameEvent;

public class CollectEvent extends GameEvent {
    private final Collectible collectible;

    public CollectEvent(Collectible collectible) {
        this.collectible = collectible;
    }

    public Collectible getCollectible() {
        return collectible;
    }
}
