package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.CollectPlugin;
import com.mantkowicz.light.ui.collect.PlayerCollectResolver;

/**
 * How the player collects: the chest's button, items picked up from the ground, items thrown out of the backpack
 * ({@link PlayerCollectResolver}), run every frame. Kept out of {@link Player}, so the actor knows nothing about the
 * collecting UI.
 */
public class CollectFeature implements Feature {
    private final PlayerCollectResolver resolver;
    private final CollectPlugin plugin;

    public CollectFeature(GameContext context, Player player) {
        resolver = new PlayerCollectResolver(player, context);
        plugin = new CollectPlugin(resolver, context);
    }

    @Override
    public void run(float delta) {
        plugin.run(delta);
    }

    /** Throws the item in a backpack slot out onto a free tile next to the player (nothing happens without one). */
    public void throwOut(int slot) {
        resolver.throwOut(slot);
    }
}
