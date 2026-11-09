package com.mantkowicz.light.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.XmlReader;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileFactory;
import com.mantkowicz.light.board.TileGraphic;
import com.mantkowicz.light.board.TileType;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.map.TileSet.TilesetProperties;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mantkowicz.light.board.TileAttribute.TEXTURE_NAME;
import static com.mantkowicz.light.board.TileAttribute.TILE_CLASS;
import static com.mantkowicz.light.map.TileObjectAttribute.OBJECT_CLASS;
import static com.mantkowicz.light.util.Tuple.of;

/**
 * Loads a Tiled map into board tiles. Two kinds of maps:
 * <ul>
 * <li>packed maps (made with tools/assets, they have an 'atlas' property): any number of tile layers drawn on top of
 * each other, 16 px tiles scaled to {@link WorldScale#TILE_SIZE}. A layer named {@value #COLLISION_LAYER} (never drawn)
 * marks cells nobody can walk on, a layer named {@value #WALLS_LAYER} marks walls that also block light;</li>
 * <li>the original map.tmx: one layer whose tiles say their class and texture in tile properties.</li>
 * </ul>
 * Both have an 'objects' layer with the player, chests, lights and furniture.
 */
public class TmxTiledMapLoader implements TiledMapLoader<TmxTileMapLoaderProperties> {
    static final String COLLISION_LAYER = "collision";
    static final String WALLS_LAYER = "walls";
    private static final String AMBIENT_LIGHT = "ambientLight";
    private static final String ATLAS = "atlas";

    private final ResourcesService resourcesService;

    public TmxTiledMapLoader(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Override
    public TileSet loadTiles(TmxTileMapLoaderProperties properties) {
        String fileName = properties.getTileMapFileName();
        if (isPackedMap(fileName)) {
            return loadPackedMap(loadWithAtlas(fileName), fileName);
        }
        TiledMap tiledMap = new TmxMapLoader().load(fileName);
        TiledMapTileLayer tileLayer = getTileLayer(tiledMap);
        TiledMapTileSet tiledMapTileSet = getTileSet(tiledMap);

        float tileHeight = tileLayer.getTileHeight();
        float tileWidth = tileLayer.getTileWidth();

        Map<GridPoint2, TileObject> objects = createObjects(tiledMap, tileLayer, tileWidth, tileHeight, fileName);

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

        connectNeighbours(tilesByCell);

        TilesetProperties tilesetProperties = getTilesetProperties(tileLayer);
        tiledMap.dispose();

        TileSet tileSet = new TileSet();
        tileSet.setTiles(tiles);
        tileSet.setProperties(tilesetProperties);
        return tileSet;
    }

    /**
     * Whether the map itself (not one of its objects or tiles) has the 'atlas' property that tools/assets writes.
     */
    private static boolean isPackedMap(String fileName) {
        XmlReader.Element properties = new XmlReader().parse(Gdx.files.internal(fileName)).getChildByName("properties");
        if (properties != null) {
            for (XmlReader.Element property : properties.getChildrenByName("property")) {
                if (ATLAS.equals(property.getAttribute("name", null))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Through the asset manager, so the map and its atlas live as long as the game's other resources (the tiles keep
     * drawing the atlas' regions) and are disposed with them. The atlas loader is registered for this file name only,
     * so other maps loaded through the asset manager keep their own loader.
     */
    private TiledMap loadWithAtlas(String fileName) {
        AssetManager assetManager = resourcesService.getAssetManager();
        if (!assetManager.isLoaded(fileName, TiledMap.class)) {
            assetManager.setLoader(TiledMap.class, fileName, new AtlasTmxMapLoader(new InternalFileHandleResolver()));
            assetManager.load(fileName, TiledMap.class);
            assetManager.finishLoadingAsset(fileName);
        }
        return assetManager.get(fileName, TiledMap.class);
    }

    private TileSet loadPackedMap(TiledMap tiledMap, String fileName) {
        List<TiledMapTileLayer> drawn = new ArrayList<>();
        TiledMapTileLayer collision = null;
        TiledMapTileLayer walls = null;
        for (TiledMapTileLayer layer : tiledMap.getLayers().getByType(TiledMapTileLayer.class)) {
            if (COLLISION_LAYER.equals(layer.getName())) {
                collision = layer;
            } else {
                if (WALLS_LAYER.equals(layer.getName())) {
                    walls = layer;
                }
                if (layer.isVisible()) {
                    drawn.add(layer);
                }
            }
        }
        if (drawn.isEmpty()) {
            throw new IllegalArgumentException("The map has no visible tile layer");
        }
        TiledMapTileLayer first = drawn.get(0);
        float tileWidth = first.getTileWidth();
        float tileHeight = first.getTileHeight();
        if (tileWidth != WorldScale.PIXEL_ART_TILE_SIZE || tileHeight != WorldScale.PIXEL_ART_TILE_SIZE) {
            throw new IllegalArgumentException(fileName + " has " + (int) tileWidth + "x" + (int) tileHeight
                    + " px tiles; the game's scale (WorldScale) is made for " + (int) WorldScale.PIXEL_ART_TILE_SIZE + " px");
        }
        float scale = WorldScale.TILE_SIZE / tileWidth;
        Map<GridPoint2, TileObject> objects = createObjects(tiledMap, first, tileWidth, tileHeight, fileName);

        Map<GridPoint2, Tile> tilesByCell = new HashMap<>();
        List<Tile> tiles = new ArrayList<>();
        for (int x = 0; x < first.getWidth(); x++) {
            for (int y = 0; y < first.getHeight(); y++) {
                List<TileGraphic> cellLayers = new ArrayList<>();
                for (TiledMapTileLayer layer : drawn) {
                    TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                    if (cell != null && cell.getTile() != null) {
                        // Tiled measures offsets downwards, the game upwards
                        cellLayers.add(new TileGraphic(cell.getTile(), cell.getFlipHorizontally(), cell.getFlipVertically(),
                                cell.getRotation(), layer.getOpacity(), layer.getRenderOffsetX() * scale,
                                -layer.getRenderOffsetY() * scale));
                    }
                }
                boolean wall = hasCell(walls, x, y);
                boolean blocked = hasCell(collision, x, y);
                if (cellLayers.isEmpty() && !wall && !blocked) {
                    continue;
                }
                Tile tile = TileFactory.createMapTile(wall ? TileType.WALL : blocked ? TileType.BLOCKED : TileType.FLOOR);
                tile.setAttributes(new MapProperties());
                tile.setGraphics(cellLayers, WorldScale.TILE_SIZE);
                tile.setPosition(x * WorldScale.TILE_SIZE, y * WorldScale.TILE_SIZE);
                GridPoint2 cellPosition = new GridPoint2(x, y);
                TileObject object = objects.get(cellPosition);
                if (object != null) {
                    tile.addTileObject(object);
                }
                tilesByCell.put(cellPosition, tile);
                tiles.add(tile);
            }
        }
        connectNeighbours(tilesByCell);

        TilesetProperties tilesetProperties = new TilesetProperties();
        tilesetProperties.setTileSize(of(WorldScale.TILE_SIZE, WorldScale.TILE_SIZE));
        MapProperties mapProperties = tiledMap.getProperties();
        if (mapProperties.containsKey(AMBIENT_LIGHT)) {
            String value = String.valueOf(mapProperties.get(AMBIENT_LIGHT));
            try {
                tilesetProperties.setAmbientLight(Float.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fileName + ": map property " + AMBIENT_LIGHT + " must be a number from 0 "
                        + "(dark) to 1 (daylight), not '" + value + "'", e);
            }
        }

        TileSet tileSet = new TileSet();
        tileSet.setTiles(tiles);
        tileSet.setProperties(tilesetProperties);
        return tileSet;
    }

    private static boolean hasCell(TiledMapTileLayer layer, int x, int y) {
        return layer != null && layer.getCell(x, y) != null;
    }

    private static void connectNeighbours(Map<GridPoint2, Tile> tilesByCell) {
        for (Map.Entry<GridPoint2, Tile> entry : tilesByCell.entrySet()) {
            Tile tile = entry.getValue();
            for (GridPoint2 neighbourCell : neighbourCells(entry.getKey().x, entry.getKey().y)) {
                Tile neighbour = tilesByCell.get(neighbourCell);
                if (neighbour != null && !tile.getNeighbours().contains(neighbour)) {
                    tile.addNeighbour(neighbour);
                }
            }
        }
    }

    private TilesetProperties getTilesetProperties(TiledMapTileLayer tileLayer) {
        TilesetProperties tilesetProperties = new TilesetProperties();
        float tileHeight = tileLayer.getTileHeight();
        float tileWidth = tileLayer.getTileWidth();
        tilesetProperties.setTileSize(of(tileWidth, tileHeight));
        return tilesetProperties;
    }

    /**
     * Each object goes to the cell whose centre is nearest to the object's centre. A later object on the same cell replaces an earlier one.
     * A mistake in an object's properties stops the game with the map, the cell and what's wrong.
     */
    private Map<GridPoint2, TileObject> createObjects(TiledMap tiledMap, TiledMapTileLayer tileLayer, float tileWidth, float tileHeight,
                                                      String fileName) {
        Map<GridPoint2, TileObject> objects = new HashMap<>();
        MapLayer objectLayer = getObjectLayer(tiledMap);
        if (objectLayer == null) {
            return objects;
        }
        for (MapObject object : objectLayer.getObjects()) {
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
                try {
                    TileObjectType objectType = objectType(objectProperties.get(OBJECT_CLASS.getValue(), String.class));
                    objects.put(new GridPoint2(objectX, objectY), TileObjectFactory.createTileObject(objectType, objectProperties));
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(fileName + ": the object on cell (" + objectX + "," + objectY + "): "
                            + e.getMessage(), e);
                }
            }
        }
        return objects;
    }

    private static TileObjectType objectType(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(OBJECT_CLASS.getValue() + " is missing");
        }
        for (TileObjectType type : TileObjectType.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException(OBJECT_CLASS.getValue() + " '" + name + "' isn't one of "
                + Arrays.toString(TileObjectType.values()));
    }

    /**
     * Bottom-left position of a cell in the square grid.
     */
    private static Vector2 cellPosition(int x, int y, float tileWidth, float tileHeight) {
        return new Vector2(x * tileWidth, y * tileHeight);
    }

    private static Vector2 cellCenter(int x, int y, float tileWidth, float tileHeight) {
        return cellPosition(x, y, tileWidth, tileHeight).add(tileWidth / 2f, tileHeight / 2f);
    }

    /**
     * The eight cells around a cell: four sides first, then four diagonals (cells may lie outside the map).
     */
    private static GridPoint2[] neighbourCells(int x, int y) {
        return new GridPoint2[]{
                new GridPoint2(x, y + 1),
                new GridPoint2(x + 1, y),
                new GridPoint2(x, y - 1),
                new GridPoint2(x - 1, y),
                new GridPoint2(x + 1, y + 1),
                new GridPoint2(x + 1, y - 1),
                new GridPoint2(x - 1, y - 1),
                new GridPoint2(x - 1, y + 1)
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
