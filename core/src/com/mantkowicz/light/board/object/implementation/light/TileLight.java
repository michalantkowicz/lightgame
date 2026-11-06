package com.mantkowicz.light.board.object.implementation.light;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.lights.GameLight;
import com.mantkowicz.light.lights.LightType;
import com.mantkowicz.light.lights.factory.LightFactory;

import static com.mantkowicz.light.board.object.TileObjectAttribute.BORDER_LIGHT_POSITION;
import static com.mantkowicz.light.board.object.TileObjectAttribute.LIGHT_COLOR_HEX;
import static com.mantkowicz.light.board.object.TileObjectAttribute.LIGHT_TYPE;
import static com.mantkowicz.light.board.object.TileObjectType.LIGHT;

public class TileLight extends TileObject {
    private static final float BORDER_OFFSET = 5f;
    private static final String DEFAULT_TYPE = "TORCH";
    private static final String DEFAULT_COLOR_HEX = "555555";

    private final LightType type;
    private final Color color;
    /** {@code null} when the light stands in the middle of the tile */
    private final BorderLightPosition borderPosition;
    private GameLight light;

    public TileLight(MapProperties objectProperties) {
        super(LIGHT);
        String typeLiteral = objectProperties.get(LIGHT_TYPE.getValue(), DEFAULT_TYPE, String.class);
        String colorLiteral = objectProperties.get(LIGHT_COLOR_HEX.getValue(), DEFAULT_COLOR_HEX, String.class);
        String borderPositionLiteral = objectProperties.get(BORDER_LIGHT_POSITION.getValue(), null, String.class);

        type = LightType.valueOf(typeLiteral);
        color = Color.valueOf(colorLiteral);
        borderPosition = (borderPositionLiteral != null) ? BorderLightPosition.valueOf(borderPositionLiteral) : null;
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        light = LightFactory.createLight(context.getRayHandler(), type);
        light.setColor(color);
        light.setTile(tile);
        context.getGameStage().addActor(light);

        if (isLightBorder()) {
            moveLight(tile, borderPosition.getRotation());
        }
    }

    private boolean isLightBorder() {
        return borderPosition != null;
    }

    /**
     * Light must be added to the stage because it's position must be known
     */
    void moveLight(Tile tile, int rotation) {
        Vector2 lightPosition = new Vector2(light.getX(), light.getY());
        Vector2 newLightPosition = lightPosition.cpy().add(0, tile.getHeight() / 2f - BORDER_OFFSET);
        Vector2 offset = newLightPosition.cpy().sub(lightPosition).rotateDeg(rotation);
        light.setPosition(light.getX() + offset.x, light.getY() + offset.y);
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
