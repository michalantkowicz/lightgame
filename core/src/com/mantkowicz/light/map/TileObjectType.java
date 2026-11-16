package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.character.Chest;
import com.mantkowicz.light.character.Chicken;
import com.mantkowicz.light.character.ChickenType;
import com.mantkowicz.light.character.Cow;
import com.mantkowicz.light.character.Field;
import com.mantkowicz.light.character.SpriteActor;
import com.mantkowicz.light.context.GameContext;

import java.util.function.BiFunction;

/**
 * What a Tiled object is (its objectClass property). Sprite actors are listed with how to create them: adding one is a
 * line here, e.g. {@code SHEEP((context, properties) -> new Sheep(context))}, and nothing else outside its class. The
 * object's Tiled properties are there for settings, e.g. a cow's favouriteDish.
 */
public enum TileObjectType {
    SPRITE,
    LIGHT,
    PLAYER,
    DOG,
    /** Grass on a tile, grown as far as its phase property says ({@link TileGrass}). */
    GRASS,
    // sprite actors
    CHEST((context, properties) -> new Chest(context)),
    COW((context, properties) -> new Cow(context,
            CropType.listOf(properties.get(TileObjectAttribute.FAVOURITE_DISH.getValue(), String.class)))),
    CHICKEN((context, properties) -> new Chicken(context, ChickenType.CHICKEN)),
    CHICKEN_BABY((context, properties) -> new Chicken(context, ChickenType.CHICKEN_BABY)),
    FIELD((context, properties) -> new Field(context,
            CropType.named(properties.get(TileObjectAttribute.CROP_TYPE.getValue(), String.class), CropType.CARROT),
            number(properties, TileObjectAttribute.WEED_CHANCE, Field.DEFAULT_WEED_CHANCE)));

    private final BiFunction<GameContext, MapProperties, SpriteActor> spriteActor;

    TileObjectType() {
        this(null);
    }

    TileObjectType(BiFunction<GameContext, MapProperties, SpriteActor> spriteActor) {
        this.spriteActor = spriteActor;
    }

    /**
     * A number property: Tiled's float or int property, or a string property holding a number; {@code fallback}
     * without it.
     */
    static float number(MapProperties properties, TileObjectAttribute attribute, float fallback) {
        Object value = properties.get(attribute.getValue());
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof String text && !text.trim().isEmpty()) {
            return Float.parseFloat(text.trim());
        }
        return fallback;
    }

    /**
     * How to create the sprite actor from the object's Tiled properties, or null when this isn't one.
     */
    public BiFunction<GameContext, MapProperties, SpriteActor> getSpriteActor() {
        return spriteActor;
    }
}
