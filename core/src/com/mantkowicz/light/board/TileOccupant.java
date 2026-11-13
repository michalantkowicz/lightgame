package com.mantkowicz.light.board;

/**
 * Something standing on a tile (a character, a chest, an item lying there): what {@link BoardService} keeps track of,
 * without knowing what it is.
 */
public interface TileOccupant {
    /** Used from a side neighbour (a chest): clicking its tile means "walk next to it". */
    default boolean isUsedFromNeighbour() {
        return false;
    }
}
