package com.mantkowicz.light.board.tile.implementation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.mantkowicz.light.board.tile.NoPathTile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.resources.ResourcesService;

public class WallTile extends NoPathTile {
    public WallTile(ResourcesService resourcesService) {
        super(resourcesService, "wall-tile.png");
    }

    @Override
    public void prepare(GameContext context) {
        super.prepare(context);

        BodyDef wallBodyDef = new BodyDef();
        Vector2 tilePosition = this.getLeftBottom();
        wallBodyDef.position.set(this.stageToLocalCoordinates(tilePosition));
        Body wallBody = context.getWorld().createBody(wallBodyDef);

        ChainShape hexShape = new ChainShape();
        hexShape.createLoop(this.getPolygonVertices());
        wallBody.createFixture(hexShape, 0.0f);

        hexShape.dispose();
    }
}
