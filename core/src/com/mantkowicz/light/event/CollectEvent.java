package com.mantkowicz.light.event;

import com.mantkowicz.light.actor.Collectible;

public class CollectEvent extends GameEvent {
    private final Collectible collectible;

    public CollectEvent(Collectible collectible) {
        this.collectible = collectible;
    }

    public Collectible getCollectible() {
        return collectible;
    }
}
