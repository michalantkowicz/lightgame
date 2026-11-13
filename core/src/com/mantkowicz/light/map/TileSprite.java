package com.mantkowicz.light.map;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;

import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_NAME;
import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_OFFSET;
import static com.mantkowicz.light.map.TileObjectType.SPRITE;

/**
 * A piece of furniture (bed, tv, ...): a picture centred on its tile, moved by the optional offset. It blocks the tile.
 */
public class TileSprite extends TileObject {
    private final String textureName;
    private final Vector2 offset;
    private TextureRegion region;

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
        region = context.getResourcesService().getTexture(textureName);
    }

    @Override
    public void draw(Tile tile, Batch batch) {
        float x = tile.getX() + tile.getWidth() / 2f - region.getRegionWidth() / 2f + offset.x;
        float y = tile.getY() + tile.getHeight() / 2f - region.getRegionHeight() / 2f + offset.y;
        batch.draw(region, x, y);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
