package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.map.TiledMapLoader;
import com.mantkowicz.light.map.TmxTileMapLoaderProperties;
import com.mantkowicz.light.map.TmxTiledMapLoader;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Board extends Actor {
    private final List<Tile> tiles;

    public Board(GameContext context, List<Tile> tiles) {
        this.tiles = tiles;
        setupPositionAndSize(tiles);
        addListener(new BoardClickListener(context, this));
    }

    private void setupPositionAndSize(List<Tile> tiles) {
        if (tiles.size() > 0) {
            float minX = tiles.get(0).getX();
            float maxX = tiles.get(0).getX();
            float minY = tiles.get(0).getY();
            float maxY = tiles.get(0).getY();

            for (Tile tile : tiles) {
                minX = min(minX, tile.getX());
                maxX = max(maxX, tile.getX());
                minY = min(minY, tile.getY());
                maxY = max(maxY, tile.getY());
            }

            setPosition(minX, minY);
            setSize(maxX - minX + tiles.get(0).getWidth(), maxY - minY + tiles.get(0).getHeight());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        for (Tile tile : tiles) {
            tile.draw(batch, parentAlpha);
        }
    }

    public static List<Tile> loadTiles(ResourcesService resourcesService, String mapName) {
        TmxTileMapLoaderProperties properties = new TmxTileMapLoaderProperties().setTileMapFileName(mapName);
        TiledMapLoader<TmxTileMapLoaderProperties> tmxTiledMapLoader = new TmxTiledMapLoader(resourcesService);
        return tmxTiledMapLoader.loadTiles(properties).getTiles();
    }

    public List<Tile> getTiles() {
        return tiles;
    }
}
