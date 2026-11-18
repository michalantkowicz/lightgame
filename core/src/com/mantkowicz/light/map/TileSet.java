package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.debug.FeatureFlag;
import com.mantkowicz.light.util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class TileSet {
    private List<Tile> tiles;
    private TilesetProperties properties;

    public static class TilesetProperties {
        private Tuple<Float, Float> tileSize;
        /** The map's 'ambientLight' property (0 dark .. 1 daylight), or {@code null} when the map doesn't set it. */
        private Float ambientLight;
        /**
         * The map's 'featuresOff' property: the features switched off on this map, e.g. GROWING_GRASS inside the coop
         * (names of {@link FeatureFlag}s, comma-separated); none without it.
         */
        private final List<FeatureFlag> featuresOff = new ArrayList<>();

        public void setTileSize(Tuple<Float, Float> tileSize) {
            this.tileSize = tileSize;
        }

        public Float getAmbientLight() {
            return ambientLight;
        }

        public void setAmbientLight(Float ambientLight) {
            this.ambientLight = ambientLight;
        }

        /** Is this feature on for this map: switched on for the game and not switched off by the map? */
        public boolean isOn(FeatureFlag feature) {
            return feature.isOn() && !featuresOff.contains(feature);
        }

        public void switchOff(FeatureFlag feature) {
            featuresOff.add(feature);
        }
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
    }

    public TilesetProperties getProperties() {
        return properties;
    }

    public void setProperties(TilesetProperties properties) {
        this.properties = properties;
    }
}
