package com.mantkowicz.light.map.implementation.tmx;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.object.TileObjectType;
import com.mantkowicz.light.board.object.factory.TileObjectFactory;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.board.tile.TileType;
import com.mantkowicz.light.board.tile.factory.TileFactory;
import com.mantkowicz.light.map.TileSet;
import com.mantkowicz.light.map.TileSet.TilesetProperties;
import com.mantkowicz.light.map.TiledMapLoader;
import com.mantkowicz.light.service.resources.ResourcesService;
import com.mantkowicz.light.util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.mantkowicz.light.board.object.TileObjectAttribute.OBJECT_CLASS;
import static com.mantkowicz.light.board.tile.TileAttribute.TEXTURE_NAME;
import static com.mantkowicz.light.board.tile.TileAttribute.TILE_CLASS;
import static com.mantkowicz.light.util.Tuple.of;

public class TmxTiledMapLoader implements TiledMapLoader<TmxTileMapLoaderProperties> {
    private final ResourcesService resourcesService;
    private final Map<Tuple<Integer, Integer>, TileObject> objectsMap = new HashMap<>();
    private final Map<Tuple<Integer, Integer>, Tile> tilesMap = new HashMap<>();

    public TmxTiledMapLoader(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Override
    public TileSet loadTiles(TmxTileMapLoaderProperties properties) {
        TiledMap tiledMap = new TmxMapLoader().load(properties.getTileMapFileName());

        int layerWidth = getTileLayer(tiledMap).getWidth();
        int layerHeight = getTileLayer(tiledMap).getHeight();

        initializeObjectsMap(tiledMap, layerHeight);

        //create tiles
        for (int x = 0; x < layerWidth; x++) {
            for (int y = 0; y < layerHeight; y++) {
                Tuple<Integer, Integer> cellPosition = of(x, y);

                Tile tile = createNewTile(tiledMap, cellPosition);
                if (tile != null) {
                    tilesMap.put(cellPosition, tile);
                }
            }
        }

        //collect neighbours
        for (int x = 0; x < layerWidth; x++) {
            for (int y = 0; y < layerHeight; y++) {
                Tuple<Integer, Integer> cellPosition = of(x, y);

                if (tilesMap.containsKey(cellPosition)) {
                    Tile tile = tilesMap.get(cellPosition);

                    Integer tileX = cellPosition.getX();
                    Integer tileY = cellPosition.getY();

                    addNeighbour(tile, of(tileX - 1, tileY - (tileX % 2)));
                    addNeighbour(tile, of(tileX + 1, tileY - (tileX % 2)));
                    addNeighbour(tile, of(tileX, tileY - 1));
                    addNeighbour(tile, of(tileX, tileY + 1));
                    addNeighbour(tile, of(tileX - 1, tileY + 1 - (tileX % 2)));
                    addNeighbour(tile, of(tileX + 1, tileY + 1 - (tileX % 2)));
                }
            }
        }

        tiledMap.dispose();

        TileSet tileSet = new TileSet();

        tileSet.setTiles(new ArrayList<>(tilesMap.values()));
        tileSet.setProperties(getTilesetProperties(tiledMap));

        return tileSet;
    }

    private TilesetProperties getTilesetProperties(TiledMap tiledMap) {
        TilesetProperties tilesetProperties = new TilesetProperties();
        float tileHeight = getTileLayer(tiledMap).getTileWidth();
        float tileWidth = getTileLayer(tiledMap).getTileWidth();
        tilesetProperties.setTileSize(of(tileWidth, tileHeight));
        return tilesetProperties;
    }

    private void initializeObjectsMap(TiledMap tiledMap, int layerHeight) {
        for (MapObject object : getObjectLayer(tiledMap).getObjects()) {
            if (object instanceof EllipseMapObject) {
                Ellipse ellipse = ((EllipseMapObject) object).getEllipse();
                TiledMapTileLayer tileLayer = getTileLayer(tiledMap);

                float tileHeight = tileLayer.getTileHeight();
                float tileWidth = cellWidth(tileHeight);

                Integer objectX = 0;
                Integer objectY = 0;

                //TODO to improve this algorithm
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    for (int y = 0; y < tileLayer.getHeight(); y++) {
                        float ellipseCenterX = ellipse.x + ellipse.width / 2f;
                        float ellipseCenterY = ellipse.y + ellipse.height / 2f;
                        float newDistance = new Vector2(ellipseCenterX, ellipseCenterY).dst2(getTileCenter(of(x, y), tileWidth, tileHeight));
                        float currentDistance = new Vector2(ellipseCenterX, ellipseCenterY).dst2(getTileCenter(of(objectX, objectY), tileWidth, tileHeight));
                        if (newDistance < currentDistance) {
                            objectX = x;
                            objectY = y;
                        }
                    }
                }

                MapProperties objectProperties = object.getProperties();
                TileObjectType objectType = TileObjectType.valueOf(objectProperties.get(OBJECT_CLASS.getValue(), String.class));
                objectsMap.put(of(objectX, objectY), TileObjectFactory.createTileObject(objectType, objectProperties));
            }
        }
    }

    private Vector2 getTileCenter(Tuple<Integer, Integer> coordinates, float tileWidth, float tileHeight) {
        return cellPosition(coordinates.getX(), coordinates.getY(), tileWidth, tileHeight).add(tileWidth / 2f, tileHeight / 2f);
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


    private void addNeighbour(Tile tile, Tuple<Integer, Integer> neighbourPosition) {
        if (tilesMap.containsKey(neighbourPosition)) {
            Tile neighbour = tilesMap.get(neighbourPosition);
            if (!tile.getNeighbours().contains(neighbour)) {
                tile.addNeighbour(neighbour);
            }
        }
    }

    private Tile createNewTile(TiledMap tiledMap, Tuple<Integer, Integer> cellPosition) {
        TiledMapTileLayer tileLayer = getTileLayer(tiledMap);
        TiledMapTileSet tileSet = getTileSet(tiledMap);

        Integer x = cellPosition.getX();
        Integer y = cellPosition.getY();

        float tileHeight = tileLayer.getTileHeight();
        float tileWidth = cellWidth(tileHeight);

        TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);

        if (cell != null) {
            int tileDefinitionId = cell.getTile().getId();
            Tile tile = createTile(tileSet, tileDefinitionId);
            Vector2 position = cellPosition(x, y, tileWidth, tileHeight);
            tile.setPosition(position.x, position.y);

            addTileObjects(tile, x, y);

            return tile;
        }
        return null;
    }

    private void addTileObjects(Tile tile, Integer x, Integer y) {
        for (Tuple objectPosition : objectsMap.keySet()) {
            if (x.equals(objectPosition.getX()) && y.equals(objectPosition.getY())) {
                tile.addTileObject(objectsMap.get(objectPosition));
            }
        }
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
