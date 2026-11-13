package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * What the player does with a creature (a cow) next to the player (the player left of it, right of it or below it:
 * {@link Tile#getUsedFromNeighbours()}), on the HUD's round button:
 * <ul>
 * <li>the creature wishes for something the player carries: the button shows that item; tapping it takes the item out
 * of the backpack (its slot shakes) and gives it to the creature;</li>
 * <li>it wishes for something the player doesn't carry: the hint button (the question mark) stays, and tapping it says
 * "find" with the item's picture;</li>
 * <li>it wishes for nothing and can be stroked: the button shows a hand; tapping it strokes the creature, and a heart
 * floats up over the player.</li>
 * </ul>
 * The buttons fade out when what they do is no longer possible (the player walks away, the creature walks or eats).
 */
public class CreatureFeature implements Feature {
    /** What the hint says next to a creature wishing for something the player doesn't carry; the item's picture follows. */
    static final String FIND = "find";
    /** The heart floating up over the player after a stroke: screen pixels per pack pixel, and how far over its tile (just over its ears). */
    private static final float HEART_SCALE = 2, HEART_GAP = -24;

    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final TextureRegion hand, heart;
    private ActionButton strokeButton, giveButton;

    public CreatureFeature(GameContext context, Player player, Hud hud) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        hand = context.getResourcesService().getUiImage(UiImage.HAND_ICON);
        heart = context.getResourcesService().getUiImage(UiImage.HEART);
    }

    @Override
    public void run(float delta) {
        Creature creature = creatureNextToPlayer();
        Item wish = creature == null ? null : creature.getWish();
        if (wish != null && slotWith(wish) < 0) {
            hud.setHint(FIND, wish.getPicture());
        } else {
            hud.resetHint();
        }
        if (canGive() && !isShown(giveButton)) {
            giveButton = addButton("GIVE", wish.getPicture(), this::give, this::canGive);
        }
        if (canStroke() && !isShown(strokeButton)) {
            strokeButton = addButton("STROKE", hand, this::stroke, this::canStroke);
        }
    }

    private boolean canGive() {
        Creature creature = creatureNextToPlayer();
        return creature != null && creature.getWish() != null && slotWith(creature.getWish()) >= 0;
    }

    private void give() {
        if (!canGive()) {
            return; // a fading button catches the click but does nothing
        }
        Creature creature = creatureNextToPlayer();
        int slot = slotWith(creature.getWish());
        Item item = player.getInventory().take(slot);
        context.getGameEventService().post(new BackpackItemEjectedEvent(slot)); // the HUD shakes its slot
        creature.give(item);
    }

    private boolean canStroke() {
        Creature creature = creatureNextToPlayer();
        return creature != null && creature.getWish() == null && creature.canBeStroked();
    }

    private void stroke() {
        if (!canStroke()) {
            return;
        }
        creatureNextToPlayer().stroke();
        new FloatingPicture("heart", heart, HEART_SCALE).showOver(player, HEART_GAP);
    }

    /** The creature the player stands left of, right of or below (the first found), or null. */
    private Creature creatureNextToPlayer() {
        Tile tile = player.getTile();
        if (tile == null) {
            return null;
        }
        for (Tile neighbour : tile.getSideNeighbours()) {
            if (!neighbour.getUsedFromNeighbours().contains(tile)) {
                continue; // the player stands above it
            }
            for (TileOccupant occupant : context.getBoardService().getOccupants(neighbour)) {
                if (occupant instanceof Creature creature) {
                    return creature;
                }
            }
        }
        return null;
    }

    /** The backpack slot holding an item of the wished kind, or -1. */
    private int slotWith(Item wish) {
        List<Item> items = player.getInventory().getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot) != null && items.get(slot).getClass() == wish.getClass()) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isShown(ActionButton button) {
        return button != null && button.isOnStage();
    }

    private ActionButton addButton(String name, TextureRegion picture, Runnable onClick, BooleanSupplier available) {
        ActionButton button = new ActionButton(context, name, picture, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        }, available);
        context.getUiStage().addActor(button);
        return button;
    }
}
