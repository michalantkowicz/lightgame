package com.mantkowicz.light.board.object.implementation.chest;

import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.implementation.collectible.Chest;
import com.mantkowicz.light.actor.implementation.item.Lighter;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.board.object.TileObjectType.CHEST;

public class TileChest extends TileObject {
    public TileChest() {
        super(CHEST);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        Chest chest = new Chest(context);
        chest.setTile(tile);

        List<Item> content = getContent(context);
        chest.getInventory().setItemsAndCompleteWithNulls(content);

        context.getGameStage().addActor(chest);
    }

    private List<Item> getContent(GameContext context) {
        Lighter lighter = new Lighter(context);

        List<Item> content = new ArrayList<>();
        content.add(lighter);

        return content;
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
