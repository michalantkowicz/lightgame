package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public class FloorTile extends AccessibleTile {
    private boolean accessible = true;

    public FloorTile(ResourcesService resourcesService) {
        super(resourcesService, "floor-tile.png");
    }

    /**
     * Without a background picture, for maps made with tools/assets: the map draws the floor.
     */
    public FloorTile() {
    }

    /**
     * A floor tile is blocked when any of its objects blocks.
     */
    @Override
    public void addTileObject(TileContent object) {
        super.addTileObject(object);
        if (!object.isAccessible()) {
            accessible = false;
        }
    }

    /**
     * For what blocks a tile and moves (a wandering cow): the tile it leaves becomes walkable, the one it steps onto
     * blocked. Use {@link BoardService#setAccessible}, which also tells the path finder.
     */
    void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    @Override
    public boolean isAccessible() {
        return accessible;
    }
}
