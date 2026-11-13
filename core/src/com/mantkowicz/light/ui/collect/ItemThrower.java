package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Throws items out onto the board: each flies onto a random free tile next to where it comes from (a chest, the
 * player). Free means walkable, with nobody and nothing on it and no other item flying there.
 */
public class ItemThrower {
    private final GameContext context;
    private final BoardService boardService;
    /** Tiles items are flying onto right now. */
    private final Set<Tile> landing = new HashSet<>();

    public ItemThrower(GameContext context) {
        this.context = context;
        this.boardService = context.getBoardService();
    }

    /**
     * @param next  the tile whose neighbours it may land on
     * @param start where its flight starts (a stage point, e.g. the chest's middle)
     * @return false (nothing is thrown) when no tile next to {@code next} is free
     */
    public boolean throwOut(Item item, Tile next, Vector2 start) {
        List<Tile> free = new ArrayList<>();
        for (Tile tile : next.getNeighbours()) {
            if (isFree(tile)) {
                free.add(tile);
            }
        }
        if (free.isEmpty()) {
            return false;
        }
        Tile target = free.get(MathUtils.random(free.size() - 1));
        landing.add(target);
        ItemOnGround thrown = new ItemOnGround(context, item);
        context.getGameStage().addActor(thrown);
        thrown.flyOnto(start, target, () -> landing.remove(target));
        return true;
    }

    private boolean isFree(Tile tile) {
        return tile.isAccessible() && boardService.getOccupants(tile).isEmpty() && !landing.contains(tile);
    }
}
