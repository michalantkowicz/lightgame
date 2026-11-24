package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.character.Chest;
import com.mantkowicz.light.character.Chicken;
import com.mantkowicz.light.character.ChickenType;
import com.mantkowicz.light.character.Cow;
import com.mantkowicz.light.character.FarmAnimal;
import com.mantkowicz.light.character.Field;
import com.mantkowicz.light.character.Frog;
import com.mantkowicz.light.character.SpriteActor;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.quest.QuestBook;

import java.util.List;
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
    /** A way to another map: whoever ends its walk on this tile comes out on the other map ({@link TileDoor}). */
    DOOR,
    /** A place to fish from ({@link TileFishingSpot}). */
    FISHING_SPOT,
    // sprite actors
    CHEST((context, properties) -> new Chest(context)),
    COW((context, properties) -> withQuests(new Cow(context, favourite(properties)), properties)),
    CHICKEN((context, properties) -> withQuests(new Chicken(context, ChickenType.CHICKEN), properties)),
    CHICKEN_BABY((context, properties) -> withQuests(new Chicken(context, ChickenType.CHICKEN_BABY), properties)),
    FIELD((context, properties) -> new Field(context,
            CropType.named(properties.get(TileObjectAttribute.CROP_TYPE.getValue(), String.class), CropType.CARROT),
            number(properties, TileObjectAttribute.WEED_CHANCE, Field.DEFAULT_WEED_CHANCE))),
    /** A frog: it blocks nobody's way (it hops away from everyone anyway). */
    FROG((context, properties) -> new Frog(context,
            Frog.colour(properties.get(TileObjectAttribute.COLOUR.getValue(), String.class))), false);

    private final BiFunction<GameContext, MapProperties, SpriteActor> spriteActor;
    /** Does the sprite actor block its tile (nobody walks onto it)? */
    private final boolean blocks;

    TileObjectType() {
        this(null);
    }

    TileObjectType(BiFunction<GameContext, MapProperties, SpriteActor> spriteActor) {
        this(spriteActor, true);
    }

    TileObjectType(BiFunction<GameContext, MapProperties, SpriteActor> spriteActor, boolean blocks) {
        this.spriteActor = spriteActor;
        this.blocks = blocks;
    }

    /** Does its sprite actor block its tile (most do; a frog doesn't)? */
    public boolean blocksItsTile() {
        return blocks;
    }

    /** A cow's favourite crop: Tiled's favouriteDish (with several, the first), a carrot without it. */
    static CropType favourite(MapProperties properties) {
        List<CropType> dishes = CropType.listOf(
                properties.get(TileObjectAttribute.FAVOURITE_DISH.getValue(), String.class));
        String named = properties.get(TileObjectAttribute.FAVOURITE_DISH.getValue(), String.class);
        return named == null || named.trim().isEmpty() ? Cow.DEFAULT_FAVOURITE : dishes.get(0);
    }

    /** The animal with its quests from Tiled (its quests property, names from quest.QuestBook). */
    static FarmAnimal withQuests(FarmAnimal animal, MapProperties properties) {
        animal.assignQuests(QuestBook.create(properties.get(TileObjectAttribute.QUESTS.getValue(), String.class)));
        return animal;
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
