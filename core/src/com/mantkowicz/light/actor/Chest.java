package com.mantkowicz.light.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.ThumbnailType;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

public class Chest extends GameBoardActor implements Collectible {
    private static final String LOG_TAG = "Chest";
    private static final String AVATAR_NAME = "chest.png";
    private static final String DESCRIPTION = "chest";
    private static final int INVENTORY_SIZE = 2;

    private final Inventory inventory;

    public Chest(GameContext context) {
        super(CHEST, context, DESCRIPTION);
        inventory = new Inventory(INVENTORY_SIZE, new ArrayList<Item>());
        setName("Nice Chest");
        createAvatar(context, AVATAR_NAME);

        setTouchable(Touchable.disabled);
    }

    @Override
    public boolean isDistant() {
        return false;
    }

    @Override
    public void beforeCollect() {
        Gdx.app.log(LOG_TAG, "I'll be collect!");
        remove();
    }

    @Override
    public void afterCollect() {
        Gdx.app.log(LOG_TAG, "I'm being collected!");
        remove();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
