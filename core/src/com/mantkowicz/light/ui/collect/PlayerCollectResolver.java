package com.mantkowicz.light.ui.collect;

import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.ActionButton;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows a COLLECT button for a collectible the player reaches. The button stays while the player is within reach (on
 * the collectible's tile, or for a distant one like a chest also on a neighbouring tile), even while walking from one
 * such tile to another, and fades out once the player leaves.
 */
public class PlayerCollectResolver implements CollectResolver {
    private final GameContext context;
    private final Player player;
    private final Map<Collectible, ActionButton> buttons = new HashMap<>();

    public PlayerCollectResolver(Player player, GameContext context) {
        this.player = player;
        this.context = context;
    }

    @Override
    public void resolveCollect(Collectible collectible) {
        ActionButton shown = buttons.get(collectible);
        if (shown != null && shown.isShown()) {
            return;
        }

        OpenCollectWindowButtonListener openCollectWindowButtonListener = new OpenCollectWindowButtonListener(
                () -> new CollectWindow(context, player, collectible), context.getUiStage());

        ActionButton actionButton = new ActionButton(context, "COLLECT", openCollectWindowButtonListener,
                () -> isInReach(collectible));
        buttons.put(collectible, actionButton);

        context.getUiStage().addActor(actionButton);
    }

    private boolean isInReach(Collectible collectible) {
        Tile playerTile = player.getTile();
        Tile collectibleTile = collectible.getTile();
        return playerTile == collectibleTile
                || (collectible.isDistant() && collectibleTile.getNeighbours().contains(playerTile));
    }
}
