package com.mantkowicz.light.board;

/**
 * Told by {@link BoardService} whenever something steps onto a tile (e.g. to find what can be collected there).
 */
public interface OccupantListener {
    /** The occupant has just been put on the tile; the tile's other occupants are already there. */
    void entered(TileOccupant occupant, Tile tile);
}
