package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;

import static com.mantkowicz.light.map.TileObjectAttribute.LIGHT_TYPE;
import static com.mantkowicz.light.map.TileObjectAttribute.TEXTURE_NAME;

public class TileObjectFactory {
    public static TileObject createTileObject(TileObjectType objectType, MapProperties objectProperties) {
        if (objectType.getSpriteActor() != null) {
            return new TileSpriteActor(objectType, objectType.getSpriteActor());
        }
        switch (objectType) {
            case SPRITE :
                assertMandatoryProperties(objectProperties, TEXTURE_NAME);
                return new TileSprite(objectProperties);
            case LIGHT :
                assertMandatoryProperties(objectProperties, LIGHT_TYPE);
                return new TileLight(objectProperties);
            case PLAYER :
                return new TilePlayer();
            case DOG :
                return new TileDog();
            default :
                throw new IllegalArgumentException(
                        "There is no tile implementation for this tile object type: " + objectType.name());
        }
    }

    private static void assertMandatoryProperties(MapProperties properites, TileObjectAttribute... attributes) {
        for (TileObjectAttribute attribute : attributes) {
            if (!doesContainNotNullProperty(properites, attribute.getValue())) {
                throw new IllegalStateException(
                        "Tile object does not contain mandatory property " + attribute.getValue());
            }
        }
    }

    private static boolean doesContainNotNullProperty(MapProperties properties, String propertyName) {
        return properties.containsKey(propertyName)
                && properties.get(propertyName, String.class) != null
                && !properties.get(propertyName, String.class).isEmpty();
    }
}
