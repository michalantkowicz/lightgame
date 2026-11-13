package com.mantkowicz.light.map;

import com.mantkowicz.light.character.Dog;
import com.mantkowicz.light.context.GameContext;

import static com.mantkowicz.light.map.TileObjectType.DOG;

public class TileDog extends TileActorObject<Dog> {
    public TileDog() {
        super(DOG);
    }

    @Override
    protected Dog createActor(GameContext context) {
        return new Dog(context);
    }
}
