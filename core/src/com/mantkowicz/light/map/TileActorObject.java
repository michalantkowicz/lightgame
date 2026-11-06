package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.game.GameContext;

/**
 * A map object that spawns an actor on its tile: creates the actor, puts it on the tile and adds it to the game stage.
 * The actor doesn't block the tile.
 */
public abstract class TileActorObject<T extends GameBoardActor> extends TileObject {
    protected TileActorObject(TileObjectType objectType) {
        super(objectType);
    }

    protected abstract T createActor(GameContext context);

    /**
     * Called after the actor is placed on its tile and before it is added to the stage.
     */
    protected void prepareActor(T actor, GameContext context) {
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        T actor = createActor(context);
        actor.setTile(tile);
        prepareActor(actor, context);
        context.getGameStage().addActor(actor);
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
