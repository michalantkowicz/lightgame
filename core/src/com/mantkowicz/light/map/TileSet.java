package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.util.Tuple;

import java.util.List;

public class TileSet {
    private List<Tile> tiles;
    private TilesetProperties properties;

    public static class TilesetProperties {
        private Tuple<Float, Float> tileSize;

        public void setTileSize(Tuple<Float, Float> tileSize) {
            this.tileSize = tileSize;
        }
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
    }

    public void setProperties(TilesetProperties properties) {
        this.properties = properties;
    }
}
