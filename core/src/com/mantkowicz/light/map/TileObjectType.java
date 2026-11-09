package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.Chest;
import com.mantkowicz.light.actor.Cow;
import com.mantkowicz.light.actor.SpriteActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.Sprite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * What a Tiled object is (its objectClass property). Sprite actors are listed with how to create them and the sprites
 * they can be drawn with, the first one being the default: adding one is a line here, e.g.
 * {@code SHEEP(Sheep::new, Sprite.SHEEP)}, and nothing else outside its class. In Tiled, the object's optional
 * {@code sprite} property picks another one of its sprites, e.g. {@code COW_PINK}.
 * <p>
 * tools/assets/build_assets.py reads this enum: it offers the constants and their sprites as dropdowns in Tiled and
 * checks the maps against them, so keep each line in the form {@code NAME(Class::new, Sprite.A, Sprite.B)}.
 */
public enum TileObjectType {
    SPRITE, LIGHT, PLAYER, DOG,
    // sprite actors
    CHEST(Chest::new, Sprite.CHEST),
    COW(Cow::new, Sprite.COW_BROWN, Sprite.COW_GREEN, Sprite.COW_LIGHT, Sprite.COW_PINK, Sprite.COW_PURPLE);

    private final BiFunction<GameContext, Sprite, SpriteActor> spriteActor;
    private final List<Sprite> sprites;

    TileObjectType() {
        this(null);
    }

    TileObjectType(BiFunction<GameContext, Sprite, SpriteActor> spriteActor, Sprite... sprites) {
        this.spriteActor = spriteActor;
        this.sprites = Collections.unmodifiableList(Arrays.asList(sprites));
    }

    /**
     * How to create the sprite actor drawn with the given sprite, or null when this isn't one.
     */
    public BiFunction<GameContext, Sprite, SpriteActor> getSpriteActor() {
        return spriteActor;
    }

    /**
     * The sprites this actor can be drawn with, the default first; empty when it isn't a sprite actor.
     */
    public List<Sprite> getSprites() {
        return sprites;
    }
}
