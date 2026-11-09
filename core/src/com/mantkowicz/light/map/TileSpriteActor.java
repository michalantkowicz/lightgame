package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.SpriteActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.Sprite;

/**
 * Any sprite actor placed in a Tiled map (a cow, a chest…): creates it once the game world exists, using its
 * {@link TileObjectType} line, e.g. {@code COW(Cow::new, Sprite.COW_BROWN, …)}, drawn with the sprite the map picked
 * (or the line's first one). Sprite actors block their tile.
 */
public class TileSpriteActor extends TileActorObject<SpriteActor> {
    private final Sprite sprite;

    public TileSpriteActor(TileObjectType objectType, Sprite sprite) {
        super(objectType);
        this.sprite = sprite;
    }

    @Override
    protected SpriteActor createActor(GameContext context) {
        return getObjectType().getSpriteActor().apply(context, sprite);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
