package com.mantkowicz.light.ui.collect;

import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.ActionButton;

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
