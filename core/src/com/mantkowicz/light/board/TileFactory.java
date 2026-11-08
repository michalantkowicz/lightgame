package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public class TileFactory {
    static public Tile createTile(ResourcesService resourcesService, TileType tileType) {
        switch (tileType) {
            case FLOOR:
                return new FloorTile(resourcesService);
            case WALL:
                return new WallTile(resourcesService);
            case BLOCKED:
                return new BlockedTile(resourcesService);
            default:
                throw new IllegalArgumentException("There is no tile implementation for this tile type: " + tileType.name());
        }
    }
}
