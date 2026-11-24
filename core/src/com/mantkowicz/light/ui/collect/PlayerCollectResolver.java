package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.actor.Waterable;
import com.mantkowicz.light.actor.Weedable;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.board.TileTouchedEvent;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.quest.QuestJournal;
import com.mantkowicz.light.ui.hud.ActionButton;

import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.actor.Status.IDLE;

/**
 * How the player gets things. Next to a collectible that still holds something (a chest: on a tile it is used from)
 * its button appears over it (the open chest); it stays while the player is within reach and fades out once the
 * player leaves. Coming back before it is gone brings the same button back, so walking around a chest (past its corner
 * cells) never shows two buttons.
 * <p>
 * The button opens the collectible only where the player stands still: during a walk the click works like a click on
 * the collectible, so the player stops on (or steps back to) the nearest cell in reach, and it opens on arrival. While
 * the button fades (out of reach) it still catches clicks, so they don't walk the player to the board under it, but
 * does nothing. Opening it (a chest lifts its lid and stays open) pops stars and throws everything inside out onto
 * free tiles around it; with nowhere for them to land, it stays shut and its button shakes. A chest then keeps its
 * button, showing the closed chest: it closes the lid, and the next tap opens it again (with nothing left inside: no
 * stars, nothing comes out). Something that can't be closed loses its button once it is empty. Something that needs
 * water (a field just picked) keeps its button, the watering can: the player waters it. Watered, a field grows by
 * itself without a button, which comes back once the crop is ripe. A ripe weed instead keeps the button too, the weed:
 * the player pulls it out and it disappears.
 * <p>
 * Items lying on the ground are picked up when the player steps onto their tile; with the backpack full, they stay where
 * they lie, and the player standing still on one can swap it for a backpack item ({@link #swap}). The player throws an
 * item from the backpack out onto a free tile next to itself.
 */
public class PlayerCollectResolver implements CollectResolver {
    private final GameContext context;
    private final Player player;
    private final QuestJournal quests;
    /** {@link #throwOut}'s slot for the quest's slot (on the quest panel), besides the backpack's. */
    public static final int QUEST_SLOT = -1;
    private final ItemThrower thrower;
    private final Map<Collectible, ActionButton> buttons = new HashMap<>();
    /** Clicked during a walk: open or close it once the player stands still in reach. */
    private Collectible useOnArrival;
    /** What the last swap put down onto the player's tile, while it is still on its way there; else null. */
    private ItemOnGround dropped;
    /** Where {@link #dropped} lands. */
    private Tile droppedOn;

    /** @param quests the quests of the whole game: what the one going on waits for goes into its slot */
    public PlayerCollectResolver(Player player, GameContext context, QuestJournal quests) {
        this.player = player;
        this.context = context;
        this.quests = quests;
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
        pickUp();
    }

    /**
     * Throws the item in a backpack slot ({@link #QUEST_SLOT}: the quest's slot) out onto a free tile next to the
     * player.
     *
     * @return false when there is an item but no free tile for it (it stays in the backpack)
     */
    public boolean throwOut(int slot) {
        if (slot == QUEST_SLOT) {
            return throwOutQuestSlot();
        }
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
     * What the player could swap a backpack item for: the item lying on its tile while it stands still there with the
     * backpack full (also one a swap has just put down there, while it lands); null otherwise.
     */
    public ItemOnGround swapOffer() {
        Tile tile = player.getTile();
        if (tile == null || !IDLE.equals(player.getStatus()) || !player.getInventory().isFull()) {
            return null;
        }
        return dropped != null && dropped.hasParent() && droppedOn == tile ? dropped : lyingOn(tile);
    }

    /** An item that has landed on this tile, or null. */
    private ItemOnGround lyingOn(Tile tile) {
        for (TileOccupant actor : context.getBoardService().getOccupants(tile)) {
            if (actor instanceof ItemOnGround lying && lying.isLanded()) {
                return lying;
            }
        }
        return null;
    }

    /**
     * Swaps the item in a backpack slot for the one the player stands on ({@link #swapOffer}): that one goes into the
     * slot, and the slot's item hops down onto the player's tile, where it lies instead (the same swap takes it back).
     *
     * @return false when there is nothing to swap (or it is still landing)
     */
    public boolean swap(int slot) {
        ItemOnGround lying = swapOffer();
        Inventory backpack = player.getInventory();
        Item going = backpack.getItems().get(slot);
        if (lying == null || !lying.isLanded() || going == null) {
            return false;
        }
        backpack.put(slot, lying.getItem());
        lying.remove();
        ItemOnGround down = new ItemOnGround(context, going);
        context.getGameStage().addActor(down);
        dropped = down;
        droppedOn = player.getTile();
        down.flyOnto(player.getCenter(), player.getTile(), () -> {
            if (dropped == down) {
                dropped = null;
            }
        });
        return true;
    }

    /** What is in the quest's slot jumps out next to the player (picked up again, it goes back into it). */
    private boolean throwOutQuestSlot() {
        Item item = quests.getSlot();
        if (item == null) {
            return true;
        }
        if (!thrower.throwOut(item, player.getTile(), player.getCenter())) {
            return false;
        }
        quests.takeSlot();
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

    /**
     * Waters it when it needs water (a field just picked), pulls out a ripe weed, closes it when it is open (a chest's
     * lid), opens it otherwise.
     */
    private void use(Collectible collectible) {
        if (needsWater(collectible)) {
            player.water(collectible.getTile().getCenter());
            ((Waterable) collectible).water();
        } else if (hasWeed(collectible)) {
            ((Weedable) collectible).pullWeed();
        } else if (collectible.isOpen()) {
            collectible.afterCollect();
        } else {
            open(collectible);
        }
    }

    /**
     * Opens it and throws out what is inside. When nothing inside has anywhere to land (every tile next to it is taken,
     * e.g. the player stands on the only free one), it stays shut and its button shakes.
     */
    private void open(Collectible collectible) {
        Tile tile = collectible.getTile();
        if (holdsSomething(collectible) && !thrower.hasRoomNextTo(tile)) {
            ActionButton button = buttons.get(collectible);
            if (button != null) {
                button.shake();
            }
            return;
        }
        collectible.beforeCollect(); // a chest lifts its lid (and keeps it open)
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
     * Picks up what has landed on the player's tile: what the quest going on waits for into its slot while that is
     * empty, else into the backpack. With the backpack full nothing changes and the item stays where it lies: the player
     * can swap it for a backpack item ({@link #swap}) or make room by tapping a slot.
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
            if (quests.wantsInSlot(lying.getItem())) {
                quests.putInSlot(lying.getItem()); // what the quest going on waits for: into its slot first
                lying.remove();
                continue;
            }
            if (backpack.isFull()) {
                return; // it stays where it lies
            }
            backpack.add(lying.getItem());
            lying.remove();
        }
    }

    /** The player can use it now: in reach, something to do with it, not opening or closing. */
    private boolean isReady(Collectible collectible) {
        return isInReach(collectible) && isUsable(collectible) && !collectible.isBusy();
    }

    /**
     * It holds something, it can be closed and opened again (a chest, even empty), it needs water or a ripe weed
     * grows on it (a field).
     */
    private static boolean isUsable(Collectible collectible) {
        return holdsSomething(collectible) || collectible.canBeClosed() || needsWater(collectible)
                || hasWeed(collectible);
    }

    private static boolean hasWeed(Collectible collectible) {
        return collectible instanceof Weedable weedable && weedable.hasWeed();
    }

    private static boolean needsWater(Collectible collectible) {
        return collectible instanceof Waterable waterable && waterable.needsWater();
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
