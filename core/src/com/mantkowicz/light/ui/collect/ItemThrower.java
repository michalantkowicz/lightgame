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
 * Throws items out onto the board: each flies onto a free tile next to where it comes from (a chest, the player), a
 * random one or the one closest to a given point (the player, so what jumps out of a chest lands where it is easy to
 * pick up). Free means walkable, with nobody and nothing on it and no other item flying there.
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
     * Throws it onto a random free tile next to {@code next}.
     *
     * @param next  the tile whose neighbours it may land on
     * @param start where its flight starts (a stage point, e.g. the player's middle)
     * @return false (nothing is thrown) when no tile next to {@code next} is free
     */
    public boolean throwOut(Item item, Tile next, Vector2 start) {
        return throwOut(item, next, start, null);
    }

    /**
     * Throws it onto the free tile next to {@code next} that is closest to {@code near} (a random one of the closest
     * when several are as close).
     *
     * @param next  the tile whose neighbours it may land on
     * @param start where its flight starts (a stage point, e.g. the chest's middle)
     * @param near  a stage point (e.g. the player's middle) it lands as close to as it can; null for a random free tile
     * @return false (nothing is thrown) when no tile next to {@code next} is free
     */
    public boolean throwOut(Item item, Tile next, Vector2 start, Vector2 near) {
        List<Tile> free = new ArrayList<>();
        float closest = Float.MAX_VALUE;
        for (Tile tile : next.getNeighbours()) {
            if (!isFree(tile)) {
                continue;
            }
            float distance = near == null ? 0 : Math.round(tile.getCenter().dst(near)); // whole pixels: ties are ties
            if (distance < closest) {
                free.clear();
                closest = distance;
            }
            if (distance == closest) {
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

    /** Is any tile next to {@code next} free, so something thrown from there has somewhere to land? */
    public boolean hasRoomNextTo(Tile next) {
        for (Tile tile : next.getNeighbours()) {
            if (isFree(tile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFree(Tile tile) {
        return tile.isAccessible() && boardService.getOccupants(tile).isEmpty() && !landing.contains(tile);
    }
}
