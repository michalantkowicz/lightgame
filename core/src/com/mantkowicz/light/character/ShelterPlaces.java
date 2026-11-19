package com.mantkowicz.light.character;

import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a farm animal goes on its trip to bed ({@link BedtimeTrip}): a place {@link #AWAY_FROM_THE_DOOR} to
 * {@link #NEAR_THE_DOOR} tiles from the doors of its kind's {@link Roost} (to sleep in the shelter, or to walk to
 * after coming out in the morning), and the nearest door.
 */
final class ShelterPlaces {
    /**
     * In the shelter it sleeps, and out of it in the morning it first walks to, a place at least this many tiles
     * (diagonally too) from the doors, and at most {@link #NEAR_THE_DOOR} from the nearest one.
     */
    static final int AWAY_FROM_THE_DOOR = 2, NEAR_THE_DOOR = 4;

    private ShelterPlaces() {
    }

    /** Walkable, and nobody and nothing on it. */
    static boolean isFree(BoardService board, Tile tile) {
        return tile != null && tile.isAccessible() && board.getOccupants(tile).isEmpty();
    }

    /**
     * A free tile it can get to from {@code from}, far enough from the doors but not too far, picked at random (its own
     * tile counts too); null when there is none.
     */
    static Tile randomPlace(BoardService board, Roost roost, Tile from) {
        List<Tile> places = new ArrayList<>();
        board.findSteps(from, tile -> {
            if (isAwayFromTheDoors(roost, tile) && isNearADoor(roost, tile) && (tile == from || isFree(board, tile))) {
                places.add(tile);
            }
            return false;
        }, tile -> isFree(board, tile), false);
        return places.isEmpty() ? null : places.get(MathUtils.random(places.size() - 1));
    }

    /** The door nearest to {@code from}, or null without doors. */
    static Tile nearestDoor(Roost roost, Tile from) {
        Tile nearest = null;
        for (Tile door : roost.getDoors()) {
            if (nearest == null || tilesApart(door, from) < tilesApart(nearest, from)) {
                nearest = door;
            }
        }
        return nearest;
    }

    /** At least {@link #AWAY_FROM_THE_DOOR} tiles from every door. */
    static boolean isAwayFromTheDoors(Roost roost, Tile tile) {
        for (Tile door : roost.getDoors()) {
            if (tilesApart(tile, door) < AWAY_FROM_THE_DOOR) {
                return false;
            }
        }
        return true;
    }

    /** Is a door at most {@link #NEAR_THE_DOOR} tiles away (with no door on the map: anywhere is near)? */
    private static boolean isNearADoor(Roost roost, Tile tile) {
        if (roost.getDoors().isEmpty()) {
            return true;
        }
        for (Tile door : roost.getDoors()) {
            if (tilesApart(tile, door) <= NEAR_THE_DOOR) {
                return true;
            }
        }
        return false;
    }

    /** How many tiles apart, diagonally too (a tile next to another, also on a corner, is 1 apart). */
    private static int tilesApart(Tile a, Tile b) {
        float size = Math.max(a.getWidth(), 1);
        return Math.round(Math.max(Math.abs(a.getCenter().x - b.getCenter().x),
                Math.abs(a.getCenter().y - b.getCenter().y)) / size);
    }
}
