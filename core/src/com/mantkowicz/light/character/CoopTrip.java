package com.mantkowicz.light.character;

import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * A chicken going to the coop to sleep, and back out in the morning; its states ({@link ChickenState#GOING_TO_BED},
 * {@link ChickenState#LEAVING_THE_COOP}) use this, with the {@link Roost} of the map it is on.
 * <ul>
 * <li>At night, on a map with a door to sleep behind (the farm's door to the coop), it walks to the door and goes
 * through. Behind it (the coop) it walks to a free tile at least {@link #AWAY_FROM_THE_DOOR} tiles from the doors (the
 * entrance), picked at random, and sleeps there. On a map without such a door (the coop's own chicken, a map without
 * a coop) it sleeps where it is.</li>
 * <li>In the morning, if it came in for the night, it walks to the door and goes back out; out there it walks to a
 * free tile at least {@link #AWAY_FROM_THE_DOOR} tiles from the doors, picked at random, and goes on with its day.</li>
 * <li>With no way there (someone in the way) it waits a moment ({@link #PATIENCE}) and tries again; still no way (or
 * the door doesn't take it: no room on the other side), it gives up: it sleeps where it is, or stays in.</li>
 * </ul>
 * What it is doing goes with it through the door ({@link #carryFrom}).
 */
class CoopTrip {
    /** In the coop it sleeps at least this many tiles (diagonally too) from the entrance. */
    static final int AWAY_FROM_THE_DOOR = 2;
    /** With no way on, it waits this long before it gives up. */
    static final float PATIENCE = 3;

    /** How it is getting on, see {@link #walk()}. */
    enum Way {
        ON_THE_WAY, THERE, GAVE_UP
    }

    private final Chicken chicken;
    private final BoardService board;
    /** The map's, once its BedtimeFeature has told it (null until then). */
    private Roost roost;
    /** It came through the door for the night: it sleeps away from the entrance and goes back out in the morning. */
    private boolean sleepsAway;
    /** It came out of the coop this morning: it walks away from the door first. */
    private boolean comingOut;
    /** It couldn't get to bed tonight: it sleeps where it is. */
    private boolean gaveUp;
    /** Where it goes: a door or a place to sleep; null when it goes nowhere. */
    private Tile target;
    /** No way on: it waits (until it is time to give up). */
    private boolean waiting;

    CoopTrip(Chicken chicken, BoardService board) {
        this.chicken = chicken;
        this.board = board;
    }

    /** The same chicken coming out on another map: it goes on with its trip there. */
    void carryFrom(CoopTrip before) {
        sleepsAway = before.sleepsAway;
        comingOut = before.comingOut;
        gaveUp = before.gaveUp;
    }

    void setRoost(Roost roost) {
        this.roost = roost;
    }

    /** Does it know its map's roost yet (the first frame on a map it doesn't)? */
    boolean knowsTheWay() {
        return roost != null;
    }

    /** Is it where it sleeps the night (or it can't get there)? */
    boolean isInBed() {
        if (gaveUp || roost == null) {
            return true;
        }
        if (sleepsAway) {
            return isAwayFromTheDoors(chicken.getTile());
        }
        return roost.getBedtimeDoor() == null;
    }

    /**
     * By day: did it come in for the night (it goes back out), or has it just come out (it walks away from the door)?
     */
    boolean isAway() {
        return sleepsAway || comingOut;
    }

    /** Did it come in for the night (it goes back out in the morning)? */
    boolean sleepsAway() {
        return sleepsAway;
    }

    /** The night is over: tonight's giving up is forgotten. */
    void morning() {
        gaveUp = false;
    }

    /** At night: off to the door to sleep behind, or to a place away from the entrance. False: nowhere to go. */
    boolean startToBed() {
        comingOut = false;
        if (sleepsAway) {
            target = randomPlaceAwayFromTheDoors();
        } else {
            target = roost == null ? null : roost.getBedtimeDoor();
        }
        waiting = false;
        if (target == null) {
            gaveUp = true;
        }
        return target != null;
    }

    /**
     * In the morning: in the coop, off to the door it came in by (the nearest door); just out of it, off to a free
     * place away from the door. False: nowhere to go (it stays in, or stays where it is).
     */
    boolean startOut() {
        target = sleepsAway ? nearestDoor() : randomPlaceAwayFromTheDoors();
        waiting = false;
        if (target == null) {
            sleepsAway = false; // nowhere to go: it lives here now
            comingOut = false;
        }
        return target != null;
    }

    /**
     * The next step on its way, when it isn't walking: {@link Way#THERE} at its place (to sleep, or away from the door
     * in the morning); at a door it waits to be taken through. {@link Way#GAVE_UP} after waiting {@link #PATIENCE} for a way (or for the door).
     */
    Way walk() {
        if (chicken.isWalking()) {
            return Way.ON_THE_WAY;
        }
        Tile at = chicken.getTile();
        if (at == target && !isDoor(target)) {
            comingOut = false;
            return Way.THERE;
        }
        if (!isDoor(target) && !isFree(target)) {
            target = randomPlaceAwayFromTheDoors(); // someone took its place: another one
        }
        if (at != target && target != null && step()) {
            waiting = false;
            return Way.ON_THE_WAY;
        }
        return waitOrGiveUp();
    }

    private Way waitOrGiveUp() {
        if (!waiting) {
            waiting = true;
            chicken.stayExactly(PATIENCE);
        }
        if (!chicken.isTimeUp()) {
            return Way.ON_THE_WAY;
        }
        gaveUp = chicken.isNight();
        sleepsAway = false; // didn't get through: it is where it lives now
        comingOut = false;
        return Way.GAVE_UP;
    }

    /**
     * One step towards the target, at its pace; false without a way. A step onto the door to sleep behind means it goes
     * in for the night; onto a door in the morning, back out.
     */
    private boolean step() {
        Tile goal = target;
        List<Tile> steps = board.findSteps(chicken.getTile(), tile -> tile == goal, this::isFree, false);
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        Tile next = steps.get(0);
        if (next == goal && isDoor(goal)) { // through it: in for the night, or out in the morning
            sleepsAway = chicken.isNight();
            comingOut = !sleepsAway;
        }
        chicken.stepOnto(next, chicken.getType().getSecondsPerTile(), true);
        return true;
    }

    private boolean isDoor(Tile tile) {
        return roost != null && (tile == roost.getBedtimeDoor() || roost.getDoors().contains(tile));
    }

    private boolean isFree(Tile tile) {
        return tile != null && tile.isAccessible() && board.getOccupants(tile).isEmpty();
    }

    /** A free tile it can get to, far enough from the doors, picked at random; null when there is none. */
    private Tile randomPlaceAwayFromTheDoors() {
        List<Tile> places = new ArrayList<>();
        board.findSteps(chicken.getTile(), tile -> {
            if (isAwayFromTheDoors(tile) && (tile == chicken.getTile() || isFree(tile))) {
                places.add(tile);
            }
            return false;
        }, this::isFree, false);
        return places.isEmpty() ? null : places.get(MathUtils.random(places.size() - 1));
    }

    private Tile nearestDoor() {
        if (roost == null) {
            return null;
        }
        Tile nearest = null;
        for (Tile door : roost.getDoors()) {
            if (nearest == null || tilesApart(door, chicken.getTile()) < tilesApart(nearest, chicken.getTile())) {
                nearest = door;
            }
        }
        return nearest;
    }

    private boolean isAwayFromTheDoors(Tile tile) {
        for (Tile door : roost.getDoors()) {
            if (tilesApart(tile, door) < AWAY_FROM_THE_DOOR) {
                return false;
            }
        }
        return true;
    }

    /** How many tiles apart, diagonally too (a tile next to another, also on a corner, is 1 apart). */
    private static int tilesApart(Tile a, Tile b) {
        float size = Math.max(a.getWidth(), 1);
        return Math.round(Math.max(Math.abs(a.getCenter().x - b.getCenter().x),
                Math.abs(a.getCenter().y - b.getCenter().y)) / size);
    }

    /** Where it goes, or null (for tests). */
    Tile getTarget() {
        return target;
    }
}
