package com.mantkowicz.light.character;

import com.mantkowicz.light.board.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the chickens of a map go to sleep, from its doors (the map's {@code BedtimeFeature} makes it): on the farm, the
 * door to the coop they go through at night (Tiled's door property {@code bedtime}); in the coop, the doors they came in
 * by, the entrance they sleep away from and go out of in the morning.
 */
public final class Roost {
    private final Tile bedtimeDoor;
    private final List<Tile> doors;

    /**
     * @param bedtimeDoor the door the chickens go through to sleep, or null
     * @param doors       the doors chickens may go through on this map
     */
    public Roost(Tile bedtimeDoor, List<Tile> doors) {
        this.bedtimeDoor = bedtimeDoor;
        this.doors = Collections.unmodifiableList(new ArrayList<>(doors));
    }

    /** The door the chickens go through to sleep, or null. */
    Tile getBedtimeDoor() {
        return bedtimeDoor;
    }

    /** The doors chickens may go through on this map. */
    List<Tile> getDoors() {
        return doors;
    }
}
