package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.character.Chest;
import com.mantkowicz.light.character.Cow;
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
    // sprite actors
    CHEST((context, properties) -> new Chest(context)),
    COW((context, properties) -> new Cow(context,
            CropType.listOf(properties.get(TileObjectAttribute.FAVOURITE_DISH.getValue(), String.class))));

    private final BiFunction<GameContext, MapProperties, SpriteActor> spriteActor;

    TileObjectType() {
        this(null);
    }

    TileObjectType(BiFunction<GameContext, MapProperties, SpriteActor> spriteActor) {
        this.spriteActor = spriteActor;
    }

    /**
     * How to create the sprite actor from the object's Tiled properties, or null when this isn't one.
     */
    public BiFunction<GameContext, MapProperties, SpriteActor> getSpriteActor() {
        return spriteActor;
    }
}
