package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.Cow;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.map.TileObjectType.COW;

/**
 * A cow placed in a Tiled map (an ellipse with objectClass COW). Nobody walks through a cow.
 */
public class TileCow extends TileActorObject<Cow> {
    public TileCow() {
        super(COW);
    }

    @Override
    protected Cow createActor(GameContext context) {
        return new Cow(context);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
