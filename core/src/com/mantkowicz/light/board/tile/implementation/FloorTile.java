package com.mantkowicz.light.board.tile.implementation;

import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.tile.AccessibleTile;
import com.mantkowicz.light.service.resources.ResourcesService;

public class FloorTile extends AccessibleTile {
    private boolean accessible = true;

    public FloorTile(ResourcesService resourcesService) {
        super(resourcesService, "floor-tile.png");
    }

    /**
     * A floor tile is blocked when any of its objects blocks.
     */
    @Override
    public void addTileObject(TileObject object) {
        super.addTileObject(object);
        if (!object.isAccessible()) {
            accessible = false;
        }
    }

    @Override
    public boolean isAccessible() {
        return accessible;
    }
}
