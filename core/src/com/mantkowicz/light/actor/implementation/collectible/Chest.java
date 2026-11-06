package com.mantkowicz.light.actor.implementation.collectible;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.resources.ThumbnailType;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

public class Chest extends GameBoardActor implements Collectible {
    private static final String AVATAR_RESOURCE_NAME = "chest.png";

    private final Inventory inventory;

    public Chest(GameContext context) {
        super(CHEST, context);

        inventory = new Inventory(2, new ArrayList<Item>());
        setName("Nice Chest");

        Texture avatarTexture = context.getResourcesService().getAssetManager().get(AVATAR_RESOURCE_NAME, Texture.class);
        createAvatar(avatarTexture);

        setTouchable(Touchable.disabled);
    }

    @Override
    public boolean isDistant() {
        return false;
    }

    @Override
    public void beforeCollect() {
        System.out.println("I'll be collect!");
        remove();
    }

    @Override
    public void afterCollect() {
        System.out.println("I'm being collected!");
        remove();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    protected String getDescription() {
        return "chest";
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
