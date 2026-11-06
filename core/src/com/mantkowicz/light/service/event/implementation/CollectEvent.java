package com.mantkowicz.light.service.event.implementation;

import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.service.event.GameEvent;

public class CollectEvent extends GameEvent {
    private final Collectible collectible;

    public CollectEvent(Collectible collectible) {
        this.collectible = collectible;
    }

    public Collectible getCollectible() {
        return collectible;
    }
}
