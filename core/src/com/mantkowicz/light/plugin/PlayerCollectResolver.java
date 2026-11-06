package com.mantkowicz.light.plugin;

import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.button.ActionButton;
import com.mantkowicz.light.ui.window.CollectWindow;

public class PlayerCollectResolver implements CollectResolver {
    private final GameContext context;
    private final Player player;

    public PlayerCollectResolver(Player player, GameContext context) {
        this.player = player;
        this.context = context;
    }

    @Override
    public void resolveCollect(Collectible collectible) {
        CollectWindow collectWindow = new CollectWindow(context, player, collectible);

        OpenCollectWindowButtonListener openCollectWindowButtonListener = new OpenCollectWindowButtonListener(collectWindow, context.getMenuStage());

        ActionButton actionButton = new ActionButton(context, "COLLECT", openCollectWindowButtonListener);

        context.getMenuStage().addActor(actionButton);
    }
}
