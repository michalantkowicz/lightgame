package com.mantkowicz.light.board.object.implementation.dog;

import com.mantkowicz.light.actor.implementation.animal.Dog;
import com.mantkowicz.light.board.object.TileActorObject;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.board.object.TileObjectType.DOG;

public class TileDog extends TileActorObject<Dog> {
    public TileDog() {
        super(DOG);
    }

    @Override
    protected Dog createActor(GameContext context) {
        return new Dog(context);
    }
}
