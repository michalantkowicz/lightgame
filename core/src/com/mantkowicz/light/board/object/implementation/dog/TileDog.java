package com.mantkowicz.light.board.object.implementation.dog;

import com.mantkowicz.light.actor.implementation.animal.Dog;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.board.object.TileObjectType.DOG;

public class TileDog extends TileObject {
    public TileDog() {
        super(DOG);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        Dog dog = new Dog(context);
        dog.setTile(tile);

        context.getGameStage().addActor(dog);
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
