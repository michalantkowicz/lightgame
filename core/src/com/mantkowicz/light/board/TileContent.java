package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.g2d.Batch;

/**
 * Something the map put on a tile (furniture, a character's start, a light...), as far as the board is concerned:
 * whether it blocks the tile, and what it draws on it. What it is and how it comes to life is the map's business.
 */
public interface TileContent {
    /** False when nobody can walk through the tile because of it. */
    boolean isAccessible();

    /**
     * Draws what it shows on its tile, after all tile backgrounds. Most contents spawn actors instead and draw nothing
     * here.
     */
    default void draw(Tile tile, Batch batch) {
    }
}
