package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.Chest;
import com.mantkowicz.light.actor.Cow;
import com.mantkowicz.light.actor.SpriteActor;
import com.mantkowicz.light.game.GameContext;

import java.util.function.Function;

/**
 * What a Tiled object is (its objectClass property). Sprite actors are listed with how to create them: adding one is a
 * line here, e.g. {@code SHEEP(Sheep::new)}, and nothing else outside its class.
 */
public enum TileObjectType {
    SPRITE,
    LIGHT,
    PLAYER,
    DOG,
    // sprite actors
    CHEST(Chest::new),
    COW(Cow::new);

    private final Function<GameContext, SpriteActor> spriteActor;

    TileObjectType() {
        this(null);
    }

    TileObjectType(Function<GameContext, SpriteActor> spriteActor) {
        this.spriteActor = spriteActor;
    }

    /**
     * How to create the sprite actor, or null when this isn't one.
     */
    public Function<GameContext, SpriteActor> getSpriteActor() {
        return spriteActor;
    }
}
