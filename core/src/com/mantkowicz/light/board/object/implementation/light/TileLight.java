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
import static com.mantkowicz.light.board.object.TileObjectType.CHEST;

public class TileLight extends TileObject {
    private static final float BORDER_OFFSET = 5f;
    private GameLight light;

    private TileLightAttributes lightAttributes;

    public TileLight(MapProperties objectProperties) {
        super(CHEST);
        lightAttributes = new TileLightAttributes(objectProperties);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        light = LightFactory.createLight(context.getRayHandler(), lightAttributes.getType());
        light.setColor(lightAttributes.getColor());
        light.setTile(tile);
        context.getGameStage().addActor(light);

        if (lightAttributes.isLightBorder()) {
            moveLight(tile, lightAttributes.getBorderPosition().getRotation());
        }
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

    private class TileLightAttributes {
        private LightType type;
        private Color color;
        private BorderLightPosition borderPosition;

        public TileLightAttributes(MapProperties properties) {
            String typeLiteral = properties.get(LIGHT_TYPE.getValue(), "TORCH", String.class);
            String colorLiteral = properties.get(LIGHT_COLOR_HEX.getValue(), "555555", String.class);
            String borderPositionLiteral = properties.get(BORDER_LIGHT_POSITION.getValue(), null, String.class);

            type = LightType.valueOf(typeLiteral);
            color = Color.valueOf(colorLiteral);
            borderPosition = (borderPositionLiteral != null) ? BorderLightPosition.valueOf(borderPositionLiteral) : null;
        }

        boolean isLightBorder() {
            return borderPosition != null;
        }

        public LightType getType() {
            return type;
        }

        public Color getColor() {
            return color;
        }

        public BorderLightPosition getBorderPosition() {
            return borderPosition;
        }
    }
}
