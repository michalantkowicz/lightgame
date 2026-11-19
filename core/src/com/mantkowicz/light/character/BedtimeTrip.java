package com.mantkowicz.light.character;

import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;

import java.util.List;

/**
 * A farm animal going to its shelter to sleep (a chicken to the coop, the cow to the barn), and back out in the
 * morning; its states (e.g. {@link ChickenState#GOING_TO_BED}, {@link ChickenState#LEAVING_THE_COOP},
 * {@link CowState#GOING_TO_BED}, {@link CowState#LEAVING_THE_BARN}) use this, with the {@link Roost} of the map it is on
 * for its kind.
 * <ul>
 * <li>At night, on a map with a door to sleep behind (the farm's door to the coop, or to the barn), it walks to the
 * door and goes through. Behind it it walks to a free tile {@link ShelterPlaces#AWAY_FROM_THE_DOOR} to {@link ShelterPlaces#NEAR_THE_DOOR} tiles
 * from the doors (the entrance), picked at random, and sleeps there. On a map without such a door (the coop's own
 * chicken, a map without a shelter) it sleeps where it is.</li>
 * <li>In the morning, if it came in for the night, it walks to the door and goes back out; out there it walks to a
 * free tile {@link ShelterPlaces#AWAY_FROM_THE_DOOR} to {@link ShelterPlaces#NEAR_THE_DOOR} tiles from the doors, picked at random, and goes on
 * with its day.</li>
 * <li>With no way there (someone in the way) it waits a moment ({@link #PATIENCE}) and tries again; still no way (or
 * the door doesn't take it: no room on the other side), it gives up: it sleeps where it is, or stays in.</li>
 * </ul>
 * Where it is with its trip goes with it through the door ({@link State}, in the animal's shared part).
 */
class BedtimeTrip {
    /** With no way on, it waits this long before it gives up. */
    static final float PATIENCE = 3;

    /** How it is getting on, see {@link #walk()}. */
    enum Way {
        ON_THE_WAY, THERE, GAVE_UP
    }

    private final FarmAnimal animal;
    private final BoardService board;
    /** Its step onto the next tile takes this long; after it, does it keep looking the way it went? */
    private final float secondsPerStep;
    private final boolean keepFacing;
    /** The map's, once its BedtimeFeature has told it (null until then). */
    private Roost roost;
    /**
     * Where it is with its trip, carried through a door with the rest of the animal ({@code FarmAnimal.Shared}): on
     * the other map it goes on from there.
     */
    static final class State {
        /** It came through the door for the night: it sleeps away from the entrance and goes back out in the morning. */
        boolean sleepsAway;
        /** It came out of its shelter this morning: it walks away from the door first. */
        boolean comingOut;
        /** It couldn't get to bed tonight: it sleeps where it is. */
        boolean gaveUp;
    }

    /** Where it goes: a door or a place to sleep; null when it goes nowhere. */
    private Tile target;
    /** No way on: it waits (until it is time to give up). */
    private boolean waiting;

    /**
     * @param secondsPerStep its step onto the next tile takes this long
     * @param keepFacing     after a step it keeps looking the way it went (a chicken), or turns back (the cow)
     */
    BedtimeTrip(FarmAnimal animal, BoardService board, float secondsPerStep, boolean keepFacing) {
        this.animal = animal;
        this.board = board;
        this.secondsPerStep = secondsPerStep;
        this.keepFacing = keepFacing;
    }

    /** Its trip so far, as the animal carries it (also through a door). */
    private State state() {
        return animal.shared.bedtime;
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
        if (state().gaveUp || roost == null) {
            return true;
        }
        if (state().sleepsAway) {
            return ShelterPlaces.isAwayFromTheDoors(roost, animal.getTile());
        }
        return roost.getBedtimeDoor() == null;
    }

    /**
     * By day: did it come in for the night (it goes back out), or has it just come out (it walks away from the door)?
     */
    boolean isAway() {
        return state().sleepsAway || state().comingOut;
    }

    /** Did it come in for the night (it goes back out in the morning)? */
    boolean sleepsAway() {
        return state().sleepsAway;
    }

    /** The night is over: tonight's giving up is forgotten. */
    void morning() {
        state().gaveUp = false;
    }

    /** At night: off to the door to sleep behind, or to a place away from the entrance. False: nowhere to go. */
    boolean startToBed() {
        state().comingOut = false;
        if (state().sleepsAway) {
            target = randomPlaceAwayFromTheDoors();
        } else {
            target = roost == null ? null : roost.getBedtimeDoor();
        }
        waiting = false;
        if (target == null) {
            state().gaveUp = true;
        }
        return target != null;
    }

    /**
     * In the morning: in the coop, off to the door it came in by (the nearest door); just out of it, off to a free
     * place away from the door. False: nowhere to go (it stays in, or stays where it is).
     */
    boolean startOut() {
        target = state().sleepsAway ? nearestDoor() : randomPlaceAwayFromTheDoors();
        waiting = false;
        if (target == null) {
            state().sleepsAway = false; // nowhere to go: it lives here now
            state().comingOut = false;
        }
        return target != null;
    }

    /**
     * The next step on its way, when it isn't walking: {@link Way#THERE} at its place (to sleep, or away from the door
     * in the morning); at a door it waits to be taken through. {@link Way#GAVE_UP} after waiting {@link #PATIENCE} for a way (or for the door).
     */
    Way walk() {
        if (animal.isWalking()) {
            return Way.ON_THE_WAY;
        }
        Tile at = animal.getTile();
        if (at == target && !isDoor(target)) {
            state().comingOut = false;
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
            animal.stayExactly(PATIENCE);
        }
        if (!animal.isTimeUp()) {
            return Way.ON_THE_WAY;
        }
        state().gaveUp = animal.isNight();
        state().sleepsAway = false; // didn't get through: it is where it lives now
        state().comingOut = false;
        return Way.GAVE_UP;
    }

    /**
     * One step towards the target, at its pace; false without a way. A step onto the door to sleep behind means it goes
     * in for the night; onto a door in the morning, back out.
     */
    private boolean step() {
        Tile goal = target;
        List<Tile> steps = board.findSteps(animal.getTile(), tile -> tile == goal, this::isFree, false);
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        Tile next = steps.get(0);
        if (next == goal && isDoor(goal)) { // through it: in for the night, or out in the morning
            state().sleepsAway = animal.isNight();
            state().comingOut = !state().sleepsAway;
        }
        animal.stepOnto(next, secondsPerStep, keepFacing);
        return true;
    }

    private boolean isDoor(Tile tile) {
        return roost != null && (tile == roost.getBedtimeDoor() || roost.getDoors().contains(tile));
    }

    private boolean isFree(Tile tile) {
        return ShelterPlaces.isFree(board, tile);
    }

    private Tile randomPlaceAwayFromTheDoors() {
        return ShelterPlaces.randomPlace(board, roost, animal.getTile());
    }

    private Tile nearestDoor() {
        return roost == null ? null : ShelterPlaces.nearestDoor(roost, animal.getTile());
    }

    /** Where it goes, or null (for tests). */
    Tile getTarget() {
        return target;
    }
}
