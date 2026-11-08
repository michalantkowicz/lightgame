package com.mantkowicz.light.board;

/**
 * Sizes shared by maps and sprites. The pixel art uses 16 px tiles; the world draws a tile 128 units big, so every
 * art pixel becomes an 8x8 block.
 */
public final class WorldScale {
    public static final float TILE_SIZE = 128f;
    public static final float PIXEL_ART_TILE_SIZE = 16f;
    public static final float PIXEL_SCALE = TILE_SIZE / PIXEL_ART_TILE_SIZE;

    private WorldScale() {
    }
}
