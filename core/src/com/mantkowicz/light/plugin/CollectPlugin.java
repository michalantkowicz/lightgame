package com.mantkowicz.light.plugin;

import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.event.CollectEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.collect.CollectResolver;

public class CollectPlugin implements Plugin {
    private final CollectResolver collectResolver;
    private final GameEventService gameEventService;

    public CollectPlugin(CollectResolver collectResolver, GameContext context) {
        this.collectResolver = collectResolver;
        this.gameEventService = context.getGameEventService();
    }

    @Override
    public void run(float delta) {
        CollectEvent gameEvent = gameEventService.poll(CollectEvent.class);
        if (gameEvent != null) {
            Collectible collectible = gameEvent.getCollectible();
            collectResolver.resolveCollect(collectible);
        }
        collectResolver.update(delta);
    }
}
