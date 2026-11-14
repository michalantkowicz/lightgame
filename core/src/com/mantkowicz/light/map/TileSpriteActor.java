package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.character.SpriteActor;
import com.mantkowicz.light.context.GameContext;

import java.util.function.BiFunction;

/**
 * Any sprite actor placed in a Tiled map (a cow, a chest…): creates it once the game world exists, using its
 * {@link TileObjectType} line and the object's Tiled properties. Sprite actors block their tile.
 */
public class TileSpriteActor extends TileActorObject<SpriteActor> {
    private final BiFunction<GameContext, MapProperties, SpriteActor> factory;
    private final MapProperties properties;

    public TileSpriteActor(TileObjectType objectType, BiFunction<GameContext, MapProperties, SpriteActor> factory,
            MapProperties properties) {
        super(objectType);
        this.factory = factory;
        this.properties = properties;
    }

    @Override
    protected SpriteActor createActor(GameContext context) {
        return factory.apply(context, properties);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
