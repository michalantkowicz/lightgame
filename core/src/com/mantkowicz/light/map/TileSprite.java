package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_NAME;
import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_OFFSET;
import static com.mantkowicz.light.map.TileObjectType.SPRITE;

public class TileSprite extends TileObject {
    private final String textureName;
    private final Vector2 offset;

    public TileSprite(MapProperties objectProperties) {
        super(SPRITE);
        textureName = objectProperties.get(TEXTURE_NAME.getValue(), String.class);
        String offsetValue = objectProperties.get(TEXTURE_OFFSET.getValue(), "0,0", String.class);
        String offsetValueY = offsetValue.split(",")[1];
        String offsetValueX = offsetValue.split(",")[0];
        this.offset = new Vector2(Float.parseFloat(offsetValueX), Float.parseFloat(offsetValueY));
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        Image image = new Image(context.getResourcesService().getTexture(textureName));
        image.setPosition(74 - image.getWidth() / 2f + offset.x, 64 - image.getHeight() / 2f + offset.y);
        // TODO bug B2: the image is never drawn. It used to be added to the tile, but Tile is no longer an Actor.
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
