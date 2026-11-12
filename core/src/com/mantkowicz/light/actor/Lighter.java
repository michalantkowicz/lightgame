package com.mantkowicz.light.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.ThumbnailType;

import static com.mantkowicz.light.actor.GameActorType.LIGHTER;

public class Lighter extends GameBoardActor implements Item, Collectible {
    private static final String LOG_TAG = "Lighter";
    private static final String DESCRIPTION = "lighter";
    private static final String THUMBNAIL_NAME = "lighter";

    private final TextureRegion thumbnail;

    public Lighter(GameContext context) {
        super(LIGHTER, context, DESCRIPTION);
        thumbnail = context.getResourcesService().getTexture(THUMBNAIL_NAME);
    }

    @Override
    public void use() {
        Gdx.app.log(LOG_TAG, "TURN LIGHTER ON");
    }

    @Override
    public void unuse() {
        Gdx.app.log(LOG_TAG, "TURN LIGHTER OFF");
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
        return ThumbnailType.LIGHT;
    }
}
