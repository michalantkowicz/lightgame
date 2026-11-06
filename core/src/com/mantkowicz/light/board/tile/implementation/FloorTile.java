package com.mantkowicz.light.board.tile.implementation;

import com.badlogic.gdx.maps.MapProperties;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.AccessibleTile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.service.resources.ResourcesService;

public class FloorTile extends AccessibleTile {
    private static final String TEXTURE_NAME_ATTRIBUTE_NAME = "textureName";

    private boolean accessible = true;

    public FloorTile(ResourcesService resourcesService) {
        super(resourcesService, "floor-tile.png");
    }

    @Override
    public void prepare(GameContext context) {
        MapProperties attributes = getAttributes();

        if (attributes != null && attributes.containsKey(TEXTURE_NAME_ATTRIBUTE_NAME)) {
            setBackground(context.getResourcesService(), attributes.get(TEXTURE_NAME_ATTRIBUTE_NAME, String.class));
        }

        for (TileObject object : objects) {
            if (!object.isAccessible()) {
                this.accessible = false;
            }
        }

        super.prepare(context);
    }

    @Override
    public boolean isAccessible() {
        return this.accessible;
    }
}
