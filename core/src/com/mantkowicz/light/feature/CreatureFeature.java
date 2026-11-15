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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * What the player does with a creature (a cow) the player stands next to (on a tile it is used from:
 * {@link Tile#getUsedFromNeighbours()}), on action buttons in a row over the creature:
 * <ul>
 * <li>it can be stroked: a button with a hand (on the left); tapping it strokes the creature, and a heart floats up
 * over the player;</li>
 * <li>it wishes for something: a button with that item (on the right) instead of the bubble with the wish. Tapping it
 * takes the item out of the backpack (its slot shakes) and gives it to the creature; when the player doesn't carry
 * one, the button shakes and the face says "find" with the item's picture, as the HUD's hint button (the question
 * mark) does next to it.</li>
 * </ul>
 * The buttons fade out when what they do is no longer possible (the player walks away, the creature walks or eats);
 * once the button with the wish is gone, the bubble is back. With several creatures next to the player (restless
 * chickens) each button is for one of them and stays over it: the wish button for one wishing for something the
 * player carries (else any wishing one), the hand for that one too if it can be stroked (else any that can).
 */
public class CreatureFeature implements Feature {
    /** What the hint says next to a creature wishing for something the player doesn't carry; the item's picture follows. */
    static final String FIND = "find";
    /** The heart floating up over the player after a stroke: screen pixels per pack pixel, and how far over its tile (just over its ears). */
    private static final float HEART_SCALE = 2, HEART_GAP = -24;

    /** Places in the row of buttons over the creature: the hand left of the wish. */
    private static final int STROKE_PLACE = 0, GIVE_PLACE = 1;

    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final TextureRegion hand, heart;
    private ActionButton strokeButton, giveButton;
    /** The creature the give button is over (its bubble hidden meanwhile), or null. */
    private Creature givingTo;

    public CreatureFeature(GameContext context, Player player, Hud hud) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        hand = context.getResourcesService().getUiImage(UiImage.HAND_ICON);
        heart = context.getResourcesService().getUiImage(UiImage.HEART);
    }

    @Override
    public void run(float delta) {
        List<Creature> near = creaturesNextToPlayer();
        Creature wishing = wishingCreature(near);
        if (wishing != null && slotWith(wishing.getWish()) < 0) {
            hud.setHint(FIND, wishing.getWish().getPicture());
        } else {
            hud.resetHint();
        }
        updateGiveButton(wishing);
        Creature strokable = strokableCreature(near, wishing);
        if (strokable != null && !isShown(strokeButton)) {
            strokeButton = addButton("STROKE", hand, () -> stroke(strokable), () -> canStroke(strokable), strokable);
            strokeButton.setPlace(STROKE_PLACE);
        }
    }

    /**
     * The button with the wish takes the bubble's place over the creature; once it is gone, the bubble is back. A
     * button belongs to its creature: it fades out once that creature is out of reach or wishes no more, and a new
     * one comes over another creature only then.
     */
    private void updateGiveButton(Creature wishing) {
        if (!isShown(giveButton) && givingTo != null) {
            givingTo.setWishShown(true);
            givingTo = null;
        }
        if (wishing != null && !isShown(giveButton)) {
            giveButton = addButton("GIVE", wishing.getWish().getPicture(), () -> give(wishing),
                    () -> canGive(wishing), wishing);
            giveButton.setPlace(GIVE_PLACE);
            givingTo = wishing;
            wishing.setWishShown(false);
        }
    }

    /**
     * Of the creatures next to the player, the one the give button is for: one wishing for something the player
     * carries, else the first one wishing at all; null when none wishes.
     */
    private Creature wishingCreature(List<Creature> near) {
        Creature first = null;
        for (Creature creature : near) {
            if (creature.getWish() == null) {
                continue;
            }
            if (slotWith(creature.getWish()) >= 0) {
                return creature;
            }
            if (first == null) {
                first = creature;
            }
        }
        return first;
    }

    /** The one the hand is for: the wishing one if it can be stroked (the buttons stay together), else the first. */
    private static Creature strokableCreature(List<Creature> near, Creature wishing) {
        if (wishing != null && wishing.canBeStroked()) {
            return wishing;
        }
        for (Creature creature : near) {
            if (creature.canBeStroked()) {
                return creature;
            }
        }
        return null;
    }

    /**
     * Its button stays while it is still the one to give to: next to the player and wishing, and no other one next to
     * the player wishes for something the player carries when it doesn't (then its button fades and comes over that
     * one).
     */
    private boolean canGive(Creature creature) {
        return wishingCreature(creaturesNextToPlayer()) == creature;
    }

    private void give(Creature creature) {
        if (!canGive(creature)) {
            return; // a fading button catches the click but does nothing
        }
        int slot = slotWith(creature.getWish());
        if (slot < 0) {
            giveButton.shake(); // nothing to give: the face says what to find
            hud.showHint();
            return;
        }
        Item item = player.getInventory().take(slot);
        context.getGameEventService().post(new BackpackItemEjectedEvent(slot)); // the HUD shakes its slot
        creature.give(item);
    }

    private boolean canStroke(Creature creature) {
        return isNextToPlayer(creature) && creature.canBeStroked();
    }

    private void stroke(Creature creature) {
        if (!canStroke(creature)) {
            return;
        }
        creature.stroke();
        new FloatingPicture("heart", heart, HEART_SCALE).showOver(player, HEART_GAP);
    }

    private boolean isNextToPlayer(Creature creature) {
        return creaturesNextToPlayer().contains(creature);
    }

    /** The creatures used from the player's tile (next to it, on a side it is used from). */
    private List<Creature> creaturesNextToPlayer() {
        List<Creature> near = new ArrayList<>();
        Tile tile = player.getTile();
        if (tile == null) {
            return near;
        }
        for (Tile neighbour : tile.getSideNeighbours()) {
            if (!neighbour.getUsedFromNeighbours().contains(tile)) {
                continue;
            }
            for (TileOccupant occupant : context.getBoardService().getOccupants(neighbour)) {
                if (occupant instanceof Creature creature) {
                    near.add(creature);
                }
            }
        }
        return near;
    }

    /** The backpack slot holding an item of the wished kind, or -1. */
    private int slotWith(Item wish) {
        List<Item> items = player.getInventory().getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (wish.isSameKindAs(items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isShown(ActionButton button) {
        return button != null && button.isOnStage();
    }

    private ActionButton addButton(String name, TextureRegion picture, Runnable onClick, BooleanSupplier available,
            Creature over) {
        ActionButton button = new ActionButton(context, name, picture, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        }, available, over.getActor());
        context.getUiStage().addActor(button);
        return button;
    }
}
