package com.mantkowicz.light.character;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.WorldScale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What a {@link Frog} sees around it: who it keeps away from and how near they are, and where it can go (water, or
 * land nobody blocks): away from them, a hop or a swim on, its way to the water or out of it.
 */
class FrogSurroundings {
    /** Who it keeps away from. */
    private static final Set<GameActorType> FEARED = EnumSet.of(GameActorType.PLAYER, GameActorType.DOG,
            GameActorType.COW, GameActorType.CHICKEN);

    private final Stage stage;

    FrogSurroundings(Stage stage) {
        this.stage = stage;
    }

    /** How far the nearest one it keeps away from is from this tile, in tiles (diagonally too). */
    int threatDistance(Tile tile) {
        int nearest = Integer.MAX_VALUE;
        for (Actor actor : stage.getActors()) {
            if (actor instanceof GameBoardActor other && FEARED.contains(other.getGameActorType())
                    && other.getTile() != null) {
                nearest = Math.min(nearest, distance(tile, other.getTile()));
            }
        }
        return nearest;
    }

    /** Nobody it keeps away from is {@link Frog#KEEP_AWAY} tiles away from this tile or nearer. */
    boolean isSafe(Tile tile) {
        return threatDistance(tile) > Frog.KEEP_AWAY;
    }

    /** Can it be on this tile: water, or land that nobody blocks (not a fence, a chest or a cow)? */
    static boolean canBeOn(Tile tile) {
        return tile.isWater() || tile.isAccessible();
    }

    /** Land that nobody blocks and nobody is near: where it sits. */
    boolean isSafeLand(Tile tile) {
        return !tile.isWater() && tile.isAccessible() && isSafe(tile);
    }

    /**
     * The tile next to {@code from} (diagonally too) where the nearest one is furthest, the water rather than the land
     * (nobody follows it there: it counts as a tile further); null when none is farther than {@code from}.
     */
    Tile awayFrom(Tile from) {
        int here = threatDistance(from);
        List<Tile> best = new ArrayList<>();
        int bestDistance = here;
        for (Tile tile : from.getNeighbours()) {
            if (!canBeOn(tile)) {
                continue;
            }
            int distance = threatDistance(tile) + (tile.isWater() ? 1 : 0);
            if (distance > bestDistance) {
                best.clear();
                bestDistance = distance;
            }
            if (distance == bestDistance && distance > here) {
                best.add(tile);
            }
        }
        return random(best);
    }

    /** A tile next to {@code from} (up, down, left or right) that is safe land; null with none. */
    Tile hopFrom(Tile from) {
        List<Tile> free = new ArrayList<>();
        for (Tile tile : from.getSideNeighbours()) {
            if (isSafeLand(tile)) {
                free.add(tile);
            }
        }
        return random(free);
    }

    /** Water next to {@code from} (up, down, left or right) where nobody is near; null with none. */
    Tile swimFrom(Tile from) {
        List<Tile> water = new ArrayList<>();
        for (Tile tile : from.getSideNeighbours()) {
            if (tile.isWater() && isSafe(tile)) {
                water.add(tile);
            }
        }
        return random(water);
    }

    private static Tile random(List<Tile> tiles) {
        return tiles.isEmpty() ? null : tiles.get(MathUtils.random(tiles.size() - 1));
    }

    /**
     * The shortest way (over tiles it can be on, diagonally too) from {@code start} to the nearest tile that is the
     * goal, without the start; null when there is none.
     */
    static Deque<Tile> wayTo(Tile start, Predicate<Tile> goal) {
        Map<Tile, Tile> cameFrom = new HashMap<>();
        Deque<Tile> open = new ArrayDeque<>();
        cameFrom.put(start, start);
        open.add(start);
        while (!open.isEmpty()) {
            Tile tile = open.poll();
            if (tile != start && goal.test(tile)) {
                Deque<Tile> way = new ArrayDeque<>();
                for (Tile at = tile; at != start; at = cameFrom.get(at)) {
                    way.addFirst(at);
                }
                return way;
            }
            for (Tile next : tile.getNeighbours()) {
                if (!cameFrom.containsKey(next) && canBeOn(next)) {
                    cameFrom.put(next, tile);
                    open.add(next);
                }
            }
        }
        return null;
    }

    /** How far apart two tiles are, in tiles, diagonally too (the bigger of the two distances). */
    static int distance(Tile a, Tile b) {
        float dx = Math.abs(a.getCenter().x - b.getCenter().x), dy = Math.abs(a.getCenter().y - b.getCenter().y);
        return Math.round(Math.max(dx, dy) / WorldScale.TILE_SIZE);
    }
}
