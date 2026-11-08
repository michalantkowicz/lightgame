package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.Chest;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Lighter;
import com.mantkowicz.light.game.GameContext;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.map.TileObjectType.CHEST;

/**
 * A chest placed in a Tiled map (an ellipse with objectClass CHEST), holding a lighter. It blocks its tile: the player
 * opens it from a neighbouring one.
 */
public class TileChest extends TileActorObject<Chest> {
    public TileChest() {
        super(CHEST);
    }

    @Override
    protected Chest createActor(GameContext context) {
        return new Chest(context);
    }

    @Override
    protected void prepareActor(Chest chest, GameContext context) {
        chest.getInventory().setItemsAndCompleteWithNulls(getContent(context));
    }

    private List<Item> getContent(GameContext context) {
        Lighter lighter = new Lighter(context);

        List<Item> content = new ArrayList<>();
        content.add(lighter);
        return content;
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
