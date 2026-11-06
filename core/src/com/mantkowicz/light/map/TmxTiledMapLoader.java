package com.mantkowicz.light.map;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileFactory;
import com.mantkowicz.light.board.TileType;
import com.mantkowicz.light.map.TileSet.TilesetProperties;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mantkowicz.light.board.TileAttribute.TEXTURE_NAME;
import static com.mantkowicz.light.board.TileAttribute.TILE_CLASS;
import static com.mantkowicz.light.map.TileObjectAttribute.OBJECT_CLASS;
import static com.mantkowicz.light.util.Tuple.of;

public class TmxTiledMapLoader implements TiledMapLoader<TmxTileMapLoaderProperties> {
    private final ResourcesService resourcesService;

    public TmxTiledMapLoader(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Override
    public TileSet loadTiles(TmxTileMapLoaderProperties properties) {
        TiledMap tiledMap = new TmxMapLoader().load(properties.getTileMapFileName());
        TiledMapTileLayer tileLayer = getTileLayer(tiledMap);
        TiledMapTileSet tiledMapTileSet = getTileSet(tiledMap);

        float tileHeight = tileLayer.getTileHeight();
        float tileWidth = cellWidth(tileHeight);

        Map<GridPoint2, TileObject> objects = createObjects(tiledMap, tileLayer, tileWidth, tileHeight);

        // create tiles, in cell order
        Map<GridPoint2, Tile> tilesByCell = new HashMap<>();
        List<Tile> tiles = new ArrayList<>();
        for (int x = 0; x < tileLayer.getWidth(); x++) {
            for (int y = 0; y < tileLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell != null) {
                    GridPoint2 cellPosition = new GridPoint2(x, y);
                    Tile tile = createTile(tiledMapTileSet, cell.getTile().getId());
                    Vector2 position = cellPosition(x, y, tileWidth, tileHeight);
                    tile.setPosition(position.x, position.y);
                    TileObject object = objects.get(cellPosition);
                    if (object != null) {
                        tile.addTileObject(object);
                    }
                    tilesByCell.put(cellPosition, tile);
                    tiles.add(tile);
                }
            }
        }

        // collect neighbours
        for (Map.Entry<GridPoint2, Tile> entry : tilesByCell.entrySet()) {
            Tile tile = entry.getValue();
            for (GridPoint2 neighbourCell : neighbourCells(entry.getKey().x, entry.getKey().y)) {
                Tile neighbour = tilesByCell.get(neighbourCell);
                if (neighbour != null && !tile.getNeighbours().contains(neighbour)) {
                    tile.addNeighbour(neighbour);
                }
            }
        }

        TilesetProperties tilesetProperties = getTilesetProperties(tileLayer);
        tiledMap.dispose();

        TileSet tileSet = new TileSet();
        tileSet.setTiles(tiles);
        tileSet.setProperties(tilesetProperties);
        return tileSet;
    }

    private TilesetProperties getTilesetProperties(TiledMapTileLayer tileLayer) {
        TilesetProperties tilesetProperties = new TilesetProperties();
        float tileHeight = tileLayer.getTileWidth();
        float tileWidth = tileLayer.getTileWidth();
        tilesetProperties.setTileSize(of(tileWidth, tileHeight));
        return tilesetProperties;
    }

    /**
     * Each object goes to the cell whose centre is nearest to the object's centre. A later object on the same cell replaces an earlier one.
     */
    private Map<GridPoint2, TileObject> createObjects(TiledMap tiledMap, TiledMapTileLayer tileLayer, float tileWidth, float tileHeight) {
        Map<GridPoint2, TileObject> objects = new HashMap<>();
        for (MapObject object : getObjectLayer(tiledMap).getObjects()) {
            if (object instanceof EllipseMapObject) {
                Ellipse ellipse = ((EllipseMapObject) object).getEllipse();
                Vector2 objectCenter = new Vector2(ellipse.x + ellipse.width / 2f, ellipse.y + ellipse.height / 2f);

                int objectX = 0;
                int objectY = 0;
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    for (int y = 0; y < tileLayer.getHeight(); y++) {
                        float newDistance = objectCenter.dst2(cellCenter(x, y, tileWidth, tileHeight));
                        float currentDistance = objectCenter.dst2(cellCenter(objectX, objectY, tileWidth, tileHeight));
                        if (newDistance < currentDistance) {
                            objectX = x;
                            objectY = y;
                        }
                    }
                }

                MapProperties objectProperties = object.getProperties();
                TileObjectType objectType = TileObjectType.valueOf(objectProperties.get(OBJECT_CLASS.getValue(), String.class));
                objects.put(new GridPoint2(objectX, objectY), TileObjectFactory.createTileObject(objectType, objectProperties));
            }
        }
        return objects;
    }

    /**
     * Width of a hex cell (flat top) of the given height.
     */
    private static float cellWidth(float tileHeight) {
        return Math.round((Math.sqrt(3) * tileHeight) / 2f);
    }

    /**
     * Bottom-left position of a cell. Odd columns are shifted down by half a cell (staggered odd columns).
     */
    private static Vector2 cellPosition(int x, int y, float tileWidth, float tileHeight) {
        return new Vector2(x * tileWidth, (y - (x % 2) / 2f) * tileHeight);
    }

    private static Vector2 cellCenter(int x, int y, float tileWidth, float tileHeight) {
        return cellPosition(x, y, tileWidth, tileHeight).add(tileWidth / 2f, tileHeight / 2f);
    }

    /**
     * The six cells around a cell in a staggered-odd hex grid (cells may lie outside the map).
     */
    private static GridPoint2[] neighbourCells(int x, int y) {
        int shift = x % 2;
        return new GridPoint2[]{
                new GridPoint2(x - 1, y - shift),
                new GridPoint2(x + 1, y - shift),
                new GridPoint2(x, y - 1),
                new GridPoint2(x, y + 1),
                new GridPoint2(x - 1, y + 1 - shift),
                new GridPoint2(x + 1, y + 1 - shift)
        };
    }

    private TiledMapTileLayer getTileLayer(TiledMap tiledMap) {
        return tiledMap
                .getLayers()
                .getByType(TiledMapTileLayer.class)
                .first();
    }

    private MapLayer getObjectLayer(TiledMap tiledMap) {
        return tiledMap
                .getLayers()
                .get("objects");
    }

    private TiledMapTileSet getTileSet(TiledMap tiledMap) {
        return tiledMap
                .getTileSets()
                .getTileSet(0);
    }

    private Tile createTile(TiledMapTileSet tileSet, int tileDefinitionId) {
        MapProperties propertiesMap = tileSet.getTile(tileDefinitionId).getProperties();
        TileType type = TileType.valueOf(propertiesMap.get(TILE_CLASS.getValue(), String.class));
        Tile tile = TileFactory.createTile(resourcesService, type);
        tile.setAttributes(propertiesMap);
        String textureName = propertiesMap.get(TEXTURE_NAME.getValue(), String.class);
        if (textureName != null) {
            tile.setBackground(resourcesService, textureName);
        }
        return tile;
    }
}
