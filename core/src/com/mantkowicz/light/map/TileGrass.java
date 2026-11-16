package com.mantkowicz.light.map;

import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;

/**
 * Grass put on a tile in Tiled (objectClass GRASS), grown as far as its phase property says: 1 a tuft, 2 two tufts, 3
 * a bush, 4 a big bush (1 without it). It is only a mark: the grass itself comes with the growing grass
 * ({@code feature.GrassFeature}, which reads these at the start), so with that switched off there is none. It blocks
 * nothing.
 */
public class TileGrass extends TileObject {
    /** The phases Tiled may give, from a tuft to a big bush. */
    public static final int FIRST_PHASE = 1, LAST_PHASE = 4;

    private final int phase;

    /** @param phase 1 (a tuft) to 4 (a big bush); kept within that */
    public TileGrass(int phase) {
        super(TileObjectType.GRASS);
        this.phase = Math.max(FIRST_PHASE, Math.min(LAST_PHASE, phase));
    }

    /** How far it has grown, as Tiled says it: 1 a tuft ... 4 a big bush. */
    public int getPhase() {
        return phase;
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        // nothing: the growing grass plants it
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
