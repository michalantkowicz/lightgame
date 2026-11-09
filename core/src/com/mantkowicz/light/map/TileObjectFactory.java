package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.resources.Sprite;

import static com.mantkowicz.light.map.TileObjectAttribute.LIGHT_TYPE;
import static com.mantkowicz.light.map.TileObjectAttribute.SPRITE;
import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_NAME;

public class TileObjectFactory {
    public static TileObject createTileObject(TileObjectType objectType, MapProperties objectProperties) {
        if (objectType.getSpriteActor() != null) {
            return new TileSpriteActor(objectType, sprite(objectType, objectProperties));
        }
        if (objectProperties.containsKey(SPRITE.getValue())) {
            throw new IllegalArgumentException(objectType + " has no sprites to pick from, remove its " + SPRITE.getValue()
                    + " property");
        }
        switch (objectType) {
            case SPRITE:
                assertMandatoryProperties(objectProperties, TEXTURE_NAME);
                return new TileSprite(objectProperties);
            case LIGHT:
                assertMandatoryProperties(objectProperties, LIGHT_TYPE);
                return new TileLight(objectProperties);
            case PLAYER:
                return new TilePlayer();
            case DOG:
                return new TileDog();
            default:
                throw new IllegalArgumentException("There is no tile implementation for this tile object type: " + objectType.name());
        }
    }

    /**
     * The sprite named by the object's optional sprite property, e.g. COW_PINK, or the type's default (its first one).
     */
    private static Sprite sprite(TileObjectType objectType, MapProperties objectProperties) {
        String name = objectProperties.get(SPRITE.getValue(), String.class);
        if (name == null || name.isEmpty()) {
            return objectType.getSprites().get(0);
        }
        for (Sprite sprite : objectType.getSprites()) {
            if (sprite.name().equals(name)) {
                return sprite;
            }
        }
        throw new IllegalArgumentException(objectType + " can't be drawn with " + SPRITE.getValue() + " '" + name
                + "', only with one of " + objectType.getSprites());
    }

    private static void assertMandatoryProperties(MapProperties properites, TileObjectAttribute... attributes) {
        for (TileObjectAttribute attribute : attributes) {
            if (!doesContainNotNullProperty(properites, attribute.getValue())) {
                throw new IllegalStateException("Tile object does not contain mandatory property " + attribute.getValue());
            }
        }
    }

    private static boolean doesContainNotNullProperty(MapProperties properties, String propertyName) {
        return properties.containsKey(propertyName)
                && properties.get(propertyName, String.class) != null
                && !properties.get(propertyName, String.class).isEmpty();
    }
}
