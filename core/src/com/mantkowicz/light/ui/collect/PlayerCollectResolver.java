package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.event.TileTouchedEvent;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.ActionButton;

import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.actor.Status.IDLE;

/**
 * Shows a COLLECT button for a collectible the player reaches. The button stays while the player is within reach (on
 * the collectible's tile, or for a distant one like a chest also on a neighbouring tile), even while walking from one
 * such tile to another, and fades out once the player leaves.
 * <p>
 * Clicking COLLECT opens the collect window only where the player stands still: during a walk the click works like a
 * click on the collectible, so the player stops on (or steps back to) the nearest cell in reach, and the window opens
 * on arrival. Every click opens a fresh window, so a cancelled window never shows its unsaved changes again.
 */
public class PlayerCollectResolver implements CollectResolver {
    private final GameContext context;
    private final Player player;
    private final Map<Collectible, ActionButton> buttons = new HashMap<>();
    /** Clicked COLLECT during a walk: open its window once the player stands still in reach. */
    private Collectible openOnArrival;

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

        ClickListener onClick = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                requestWindow(collectible);
            }
        };
        ActionButton actionButton = new ActionButton(context, "COLLECT", onClick, () -> isInReach(collectible));
        buttons.put(collectible, actionButton);

        context.getUiStage().addActor(actionButton);
    }

    @Override
    public void update() {
        if (openOnArrival != null && IDLE.equals(player.getStatus())) {
            Collectible collectible = openOnArrival;
            openOnArrival = null;
            if (isInReach(collectible)) {
                openWindow(collectible);
            }
        }
    }

    private void requestWindow(Collectible collectible) {
        if (IDLE.equals(player.getStatus())) {
            openWindow(collectible);
        } else {
            openOnArrival = collectible;
            context.getGameEventService().post(new TileTouchedEvent(collectible.getTile()));
        }
    }

    private void openWindow(Collectible collectible) {
        context.getUiStage().addActor(new CollectWindow(context, player, collectible));
    }

    private boolean isInReach(Collectible collectible) {
        Tile playerTile = player.getTile();
        Tile collectibleTile = collectible.getTile();
        return playerTile == collectibleTile
                || (collectible.isDistant() && collectibleTile.getNeighbours().contains(playerTile));
    }
}
