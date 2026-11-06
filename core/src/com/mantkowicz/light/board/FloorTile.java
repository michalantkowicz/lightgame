package com.mantkowicz.light.board;

import com.mantkowicz.light.map.TileObject;
import com.mantkowicz.light.resources.ResourcesService;

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
