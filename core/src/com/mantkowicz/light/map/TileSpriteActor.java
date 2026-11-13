package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.SpriteActor;
import com.mantkowicz.light.context.GameContext;

import java.util.function.Function;

/**
 * Any sprite actor placed in a Tiled map (a cow, a chest…): creates it once the game world exists, using its
 * {@link TileObjectType} line, e.g. {@code COW(Cow::new)}. Sprite actors block their tile.
 */
public class TileSpriteActor extends TileActorObject<SpriteActor> {
    private final Function<GameContext, SpriteActor> factory;

    public TileSpriteActor(TileObjectType objectType, Function<GameContext, SpriteActor> factory) {
        super(objectType);
        this.factory = factory;
    }

    @Override
    protected SpriteActor createActor(GameContext context) {
        return factory.apply(context);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
