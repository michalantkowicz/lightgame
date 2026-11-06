package com.mantkowicz.light.board.object.implementation.chest;

import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.implementation.collectible.Chest;
import com.mantkowicz.light.actor.implementation.item.Lighter;
import com.mantkowicz.light.board.object.TileActorObject;
import com.mantkowicz.light.game.GameContext;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.board.object.TileObjectType.CHEST;

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
}
