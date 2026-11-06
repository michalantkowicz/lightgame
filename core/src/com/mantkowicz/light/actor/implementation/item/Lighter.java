package com.mantkowicz.light.actor.implementation.item;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.resources.ThumbnailType;

import static com.mantkowicz.light.actor.GameActorType.LIGHTER;
import static com.mantkowicz.light.service.resources.ThumbnailType.DEFAULT;

public class Lighter extends GameBoardActor implements Item, Collectible {
    private static final String DESCRIPTION = "lighter";
    private static final String THUMBNAIL_NAME = "lighter";

    private final TextureRegion thumbnail;

    public Lighter(GameContext context) {
        super(LIGHTER, context, DESCRIPTION);
        thumbnail = context.getResourcesService().getTexture(THUMBNAIL_NAME);
    }

    @Override
    public void use() {
        System.out.println("TURN LIGHTER ON");
    }

    @Override
    public void unuse() {
        System.out.println("TURN LIGHTER OFF");
    }

    @Override
    public boolean isDistant() {
        return false;
    }

    @Override
    public void beforeCollect() {

    }

    @Override
    public void afterCollect() {

    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
