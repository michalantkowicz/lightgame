package com.mantkowicz.light.board;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.ResourcesService;

/**
 * A tile nobody walks through. It also blocks light: it adds a static body with the tile's outline to the physics world.
 */
public class WallTile extends NoPathTile {
    public WallTile(ResourcesService resourcesService) {
        super(resourcesService, "wall-tile.png");
    }

    @Override
    public void prepare(GameContext context) {
        super.prepare(context);

        // the body stays at the world origin and the shape uses the tile's stage coordinates (the world uses stage units)
        Body wallBody = context.getWorld().createBody(new BodyDef());

        ChainShape outline = new ChainShape();
        outline.createLoop(this.getPolygonVertices());
        wallBody.createFixture(outline, 0.0f);

        outline.dispose();
    }
}
