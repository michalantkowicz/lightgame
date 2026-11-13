package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.event.TileTouchedEvent;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.ui.ActionButton;

import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.actor.Status.IDLE;

/**
 * How the player gets things. Next to a collectible that still holds something (a chest: on one of its four sides)
 * its button appears; it stays while the player is within reach and fades out once the player leaves. Coming back
 * before it is gone brings the same button back, so walking around a chest (past its corner cells) never shows two
 * buttons.
 * <p>
 * The button opens the collectible only where the player stands still: during a walk the click works like a click on
 * the collectible, so the player stops on (or steps back to) the nearest cell in reach, and it opens on arrival. While
 * the button fades (out of reach) it still catches clicks, so they don't walk the player to the board under it, but
 * does nothing. Opening it (a chest lifts its lid and stays open) pops stars and throws everything inside out onto
 * free tiles around it; then the button goes, nothing is left.
 * <p>
 * Items lying on the ground are picked up when the player steps onto their tile; with the backpack full, the item in its
 * first slot jumps out to make room. The player throws an item from the backpack out onto a free tile next to itself.
 */
public class PlayerCollectResolver implements CollectResolver {
    private final GameContext context;
    private final Player player;
    private final ItemThrower thrower;
    private final Map<Collectible, ActionButton> buttons = new HashMap<>();
    /** After making room in a full backpack, nothing is picked up for this long: the old item clearly leaves first. */
    static final float MAKING_ROOM_SECONDS = 1;
    /** Clicked during a walk: open it once the player stands still in reach. */
    private Collectible openOnArrival;
    /** Seconds left until something can be picked up again (after making room). */
    private float pickUpPause;

    public PlayerCollectResolver(Player player, GameContext context) {
        this.player = player;
        this.context = context;
        this.thrower = new ItemThrower(context);
    }

    @Override
    public void resolveCollect(Collectible collectible) {
        ActionButton shown = buttons.get(collectible);
        if (shown != null && shown.isOnStage() || !holdsSomething(collectible)) {
            return;
        }

        ClickListener onClick = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                requestOpening(collectible);
            }
        };
        ActionButton actionButton = new ActionButton(context, "COLLECT", collectible.getActionIcon(), onClick,
                () -> isInReach(collectible) && holdsSomething(collectible));
        buttons.put(collectible, actionButton);

        context.getUiStage().addActor(actionButton);
    }

    @Override
    public void update(float delta) {
        if (openOnArrival != null && IDLE.equals(player.getStatus())) {
            Collectible collectible = openOnArrival;
            openOnArrival = null;
            if (isInReach(collectible)) {
                open(collectible);
            }
        }
        pickUpPause = Math.max(0, pickUpPause - delta);
        if (pickUpPause == 0) {
            pickUp();
        }
    }

    /** Throws the item in a backpack slot out onto a free tile next to the player (nothing happens without one). */
    public void throwOut(int slot) {
        Inventory backpack = player.getInventory();
        Item item = backpack.getItems().get(slot);
        if (item != null && thrower.throwOut(item, player.getTile(), player.getCenter())) {
            backpack.take(slot);
        }
    }

    private void requestOpening(Collectible collectible) {
        if (!isInReach(collectible) || !holdsSomething(collectible)) {
            return; // a fading button (the player is out of reach, or it's empty) catches the click but does nothing
        }
        if (IDLE.equals(player.getStatus())) {
            open(collectible);
        } else {
            openOnArrival = collectible;
            context.getGameEventService().post(new TileTouchedEvent(collectible.getTile()));
        }
    }

    private void open(Collectible collectible) {
        collectible.beforeCollect(); // a chest lifts its lid (and keeps it open)
        Tile tile = collectible.getTile();
        Sparkles.burst(context.getGameStage(), context.getResourcesService(), tile.getCenter());
        Inventory inside = collectible.getInventory();
        for (int slot = 0; slot < inside.getItems().size(); slot++) {
            Item item = inside.getItems().get(slot);
            if (item != null && thrower.throwOut(item, tile, tile.getCenter())) {
                inside.take(slot);
            }
        }
    }

    /**
     * Picks up what has landed on the player's tile. With the backpack full, the item in its first slot jumps out onto a
     * free tile next to the player to make room and the new one goes into that slot a moment later ({@link
     * #MAKING_ROOM_SECONDS}, if the player still stands there), so it is clear which goes and which comes; the other
     * slots stay as they are (the player empties them by tapping them). With no free tile around, nothing changes and
     * the item stays where it lies.
     */
    private void pickUp() {
        Tile tile = player.getTile();
        if (tile == null) {
            return;
        }
        Inventory backpack = player.getInventory();
        for (GameActor actor : context.getBoardService().getGameActors(tile)) {
            if (!(actor instanceof ItemOnGround lying) || !lying.isLanded()) {
                continue;
            }
            if (backpack.isFull()) {
                if (!thrower.throwOut(backpack.getItems().get(0), tile, player.getCenter())) {
                    continue;
                }
                backpack.take(0);
                context.getGameEventService().post(new BackpackItemEjectedEvent(0)); // the HUD shakes its slot
                pickUpPause = MAKING_ROOM_SECONDS; // the new one comes in after it
                return;
            }
            backpack.add(lying.getItem());
            lying.remove();
        }
    }

    private static boolean holdsSomething(Collectible collectible) {
        Inventory inside = collectible.getInventory();
        return inside != null && inside.getItems().stream().anyMatch(item -> item != null);
    }

    private boolean isInReach(Collectible collectible) {
        Tile playerTile = player.getTile();
        Tile collectibleTile = collectible.getTile();
        return playerTile == collectibleTile
                || (collectible.isDistant() && collectibleTile.getSideNeighbours().contains(playerTile));
    }
}
