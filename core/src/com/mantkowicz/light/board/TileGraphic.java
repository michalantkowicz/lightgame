package com.mantkowicz.light.board;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

/**
 * One map layer's picture on a tile: the map tile and how Tiled placed it (mirrored with X/Y, rotated with Z), plus the
 * layer's opacity and offset. {@link Tile#draw} draws it like libGDX's own map renderer would.
 *
 * @param rotation {@link Cell#ROTATE_0} … {@link Cell#ROTATE_270} (counter-clockwise)
 * @param offsetX  the layer's offset in world units (right is positive)
 * @param offsetY  the layer's offset in world units (up is positive)
 */
public record TileGraphic(TiledMapTile tile, boolean flipHorizontally, boolean flipVertically, int rotation,
        float opacity, float offsetX, float offsetY) {

    /**
     * The tile as it is: not mirrored, not rotated, opaque, no offset.
     */
    public static TileGraphic of(TiledMapTile tile) {
        return new TileGraphic(tile, false, false, Cell.ROTATE_0, 1f, 0f, 0f);
    }
}
