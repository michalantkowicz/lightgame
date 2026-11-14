package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.board.TileTouchedEvent;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.ui.hud.ActionButton;

import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.actor.Status.IDLE;

/**
 * How the player gets things. Next to a collectible that still holds something (a chest: on a tile it is used from)
 * its button appears over it (the open chest); it stays while the player is within reach and fades out once the player leaves. Coming back
 * before it is gone brings the same button back, so walking around a chest (past its corner cells) never shows two
 * buttons.
 * <p>
 * The button opens the collectible only where the player stands still: during a walk the click works like a click on
 * the collectible, so the player stops on (or steps back to) the nearest cell in reach, and it opens on arrival. While
 * the button fades (out of reach) it still catches clicks, so they don't walk the player to the board under it, but
 * does nothing. Opening it (a chest lifts its lid and stays open) pops stars and throws everything inside out onto
 * free tiles around it. A chest then keeps its button, showing the closed chest: it closes the lid, and the next tap
 * opens it again (with nothing left inside: no stars, nothing comes out). Something that can't be closed loses its
 * button once it is empty.
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
    /** Clicked during a walk: open or close it once the player stands still in reach. */
    private Collectible useOnArrival;
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
        if (shown != null && shown.isOnStage() || !isUsable(collectible) || !isInReach(collectible)) {
            return; // not in reach: something else stepped next to it (a wandering cow)
        }

        ClickListener onClick = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                requestUse(collectible);
            }
        };
        ActionButton actionButton = new ActionButton(context, "COLLECT", collectible.getActionIcon(), onClick,
                () -> isReady(collectible), collectible.getActor());
        actionButton.setTopY(collectible::getTopY);
        buttons.put(collectible, actionButton);

        context.getUiStage().addActor(actionButton);
    }

    @Override
    public void update(float delta) {
        if (useOnArrival != null && IDLE.equals(player.getStatus())) {
            Collectible collectible = useOnArrival;
            useOnArrival = null;
            if (isInReach(collectible)) {
                use(collectible);
            }
        }
        for (Map.Entry<Collectible, ActionButton> entry : buttons.entrySet()) {
            showReady(entry.getKey(), entry.getValue());
        }
        pickUpPause = Math.max(0, pickUpPause - delta);
        if (pickUpPause == 0) {
            pickUp();
        }
    }

    /**
     * Throws the item in a backpack slot out onto a free tile next to the player.
     *
     * @return false when there is an item but no free tile for it (it stays in the backpack)
     */
    public boolean throwOut(int slot) {
        Inventory backpack = player.getInventory();
        Item item = backpack.getItems().get(slot);
        if (item == null) {
            return true; // an empty slot: nothing to do
        }
        if (!thrower.throwOut(item, player.getTile(), player.getCenter())) {
            return false;
        }
        backpack.take(slot);
        return true;
    }

    /**
     * Once a chest's lid has stopped, its button shows what it does now (open or close), fading back in if it faded
     * out meanwhile (or coming back if it was gone).
     */
    private void showReady(Collectible collectible, ActionButton button) {
        if (collectible.isBusy()) {
            return;
        }
        button.setPicture(collectible.getActionIcon());
        if (!button.isOnStage()) {
            resolveCollect(collectible);
        }
    }

    private void requestUse(Collectible collectible) {
        if (!isReady(collectible)) {
            return; // a fading button (out of reach, empty, the lid moving) catches the click but does nothing
        }
        if (IDLE.equals(player.getStatus())) {
            use(collectible);
        } else {
            useOnArrival = collectible;
            context.getGameEventService().post(new TileTouchedEvent(collectible.getTile()));
        }
    }

    /** Closes it when it is open (a chest shuts its lid), opens it otherwise. */
    private void use(Collectible collectible) {
        if (collectible.isOpen()) {
            collectible.afterCollect();
        } else {
            open(collectible);
        }
    }

    private void open(Collectible collectible) {
        collectible.beforeCollect(); // a chest lifts its lid (and keeps it open)
        Tile tile = collectible.getTile();
        if (!holdsSomething(collectible)) {
            return; // opened again, empty: nothing comes out, no stars
        }
        Sparkles.burst(context.getGameStage(), context.getResourcesService(), tile.getCenter());
        Inventory inside = collectible.getInventory();
        for (int slot = 0; slot < inside.getItems().size(); slot++) {
            Item item = inside.getItems().get(slot);
            if (item != null && thrower.throwOut(item, tile, tile.getCenter(), player.getCenter())) {
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
        for (TileOccupant actor : context.getBoardService().getOccupants(tile)) {
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

    /** The player can use it now: in reach, something to do with it, not opening or closing. */
    private boolean isReady(Collectible collectible) {
        return isInReach(collectible) && isUsable(collectible) && !collectible.isBusy();
    }

    /** It holds something, or it can be closed and opened again (a chest, even empty). */
    private static boolean isUsable(Collectible collectible) {
        return holdsSomething(collectible) || collectible.canBeClosed();
    }

    private static boolean holdsSomething(Collectible collectible) {
        Inventory inside = collectible.getInventory();
        return inside != null && inside.getItems().stream().anyMatch(item -> item != null);
    }

    private boolean isInReach(Collectible collectible) {
        Tile playerTile = player.getTile();
        Tile collectibleTile = collectible.getTile();
        return playerTile == collectibleTile
                || (collectible.isDistant() && collectibleTile.getUsedFromNeighbours().contains(playerTile));
    }
}
