package com.mantkowicz.light.board;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

/**
 * One map layer's picture on a tile: the map tile and how Tiled placed it (mirrored with X/Y, rotated with Z), plus the
 * layer's opacity and offset. {@link Tile#draw} draws it like libGDX's own map renderer would.
 */
public final class TileGraphic {
    private final TiledMapTile tile;
    private final boolean flipHorizontally;
    private final boolean flipVertically;
    private final int rotation;
    private final float opacity;
    private final float offsetX;
    private final float offsetY;

    /**
     * @param rotation {@link Cell#ROTATE_0} … {@link Cell#ROTATE_270} (counter-clockwise)
     * @param offsetX  the layer's offset in world units (right is positive)
     * @param offsetY  the layer's offset in world units (up is positive)
     */
    public TileGraphic(TiledMapTile tile, boolean flipHorizontally, boolean flipVertically, int rotation,
                       float opacity, float offsetX, float offsetY) {
        this.tile = tile;
        this.flipHorizontally = flipHorizontally;
        this.flipVertically = flipVertically;
        this.rotation = rotation;
        this.opacity = opacity;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * The tile as it is: not mirrored, not rotated, opaque, no offset.
     */
    public static TileGraphic of(TiledMapTile tile) {
        return new TileGraphic(tile, false, false, Cell.ROTATE_0, 1f, 0f, 0f);
    }

    public TiledMapTile getTile() {
        return tile;
    }

    public boolean isFlipHorizontally() {
        return flipHorizontally;
    }

    public boolean isFlipVertically() {
        return flipVertically;
    }

    public int getRotation() {
        return rotation;
    }

    public float getOpacity() {
        return opacity;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
