package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public class TileFactory {
    static public Tile createTile(ResourcesService resourcesService, TileType tileType) {
        switch (tileType) {
            case FLOOR :
                return new FloorTile(resourcesService);
            case WALL :
                return new WallTile(resourcesService);
            case BLOCKED :
                return new BlockedTile();
            default :
                throw new IllegalArgumentException(
                        "There is no tile implementation for this tile type: " + tileType.name());
        }
    }

    /**
     * A tile without a background picture, for maps made with tools/assets: the map's layers draw it
     * ({@link Tile#setGraphics}), so the old single-texture pictures (floor-tile.png...) are never loaded for them.
     */
    static public Tile createMapTile(TileType tileType) {
        switch (tileType) {
            case FLOOR :
                return new FloorTile();
            case WALL :
                return new WallTile();
            case BLOCKED :
                return new BlockedTile();
            default :
                throw new IllegalArgumentException(
                        "There is no tile implementation for this tile type: " + tileType.name());
        }
    }
}
