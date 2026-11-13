package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.List;

/**
 * Loading a map (a Tiled .tmx file in the assets) into the board's tiles.
 */
public final class Maps {
    private Maps() {
    }

    public static List<Tile> loadTiles(ResourcesService resourcesService, String mapName) {
        return loadTileSet(resourcesService, mapName).getTiles();
    }

    /**
     * The map's tiles together with its properties (tile size, ambient light).
     */
    public static TileSet loadTileSet(ResourcesService resourcesService, String mapName) {
        TmxTileMapLoaderProperties properties = new TmxTileMapLoaderProperties().setTileMapFileName(mapName);
        TiledMapLoader<TmxTileMapLoaderProperties> tmxTiledMapLoader = new TmxTiledMapLoader(resourcesService);
        return tmxTiledMapLoader.loadTiles(properties);
    }
}
