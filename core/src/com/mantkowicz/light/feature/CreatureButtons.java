package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.ItemFlights;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** What the buttons over creatures need ({@link CreatureFeature}, {@link QuestFeature}): who is near, what is carried. */
final class CreatureButtons {
    private CreatureButtons() {
    }

    /** The creatures used from the player's tile (next to it, on a side it is used from). */
    static List<Creature> nextTo(Player player, BoardService board) {
        List<Creature> near = new ArrayList<>();
        Tile tile = player.getTile();
        if (tile == null) {
            return near;
        }
        for (Tile neighbour : tile.getSideNeighbours()) {
            if (!neighbour.getUsedFromNeighbours().contains(tile)) {
                continue;
            }
            for (TileOccupant occupant : board.getOccupants(neighbour)) {
                if (occupant instanceof Creature creature) {
                    near.add(creature);
                }
            }
        }
        return near;
    }

    /** The backpack slot holding an item of this kind, or -1. */
    static int slotWith(Player player, Item kind) {
        List<Item> items = player.getInventory().getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (kind.isSameKindAs(items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    static boolean isShown(ActionButton button) {
        return button != null && button.isOnStage();
    }

    /** A round button over the creature, on the UI stage: it fades out once it isn't {@code available}. */
    static ActionButton add(GameContext context, String name, TextureRegion picture, Runnable onClick,
            BooleanSupplier available, Creature over) {
        ActionButton button = new ActionButton(context, name, picture, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        }, available, over.getActor());
        context.getUiStage().addActor(button);
        return button;
    }

    /** A picture of the item flies from its slot ({@link ItemFlights#QUEST_SLOT}: the quest's) to the creature. */
    static void flyTo(GameContext context, Hud hud, Item item, int fromSlot, Creature creature) {
        Vector2 screen = context.getGameStage().stageToScreenCoordinates(new Vector2(creature.getCenter()));
        hud.getFlights().toScreen(nameOf(item), item.getPicture(), fromSlot, screen);
    }

    /** What an item is called in the flights, e.g. "carrot", "milk". */
    static String nameOf(Item item) {
        return item.toString().toLowerCase();
    }
}
