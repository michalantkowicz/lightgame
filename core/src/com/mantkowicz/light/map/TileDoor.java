package com.mantkowicz.light.map;

import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A door to another map, put on a walkable tile in Tiled (objectClass DOOR): whoever ends its walk on this tile (the
 * rabbit, an animal that may go through) comes out on the other map, on the door with the same name. Its properties:
 * {@code door} (the name, the same on both maps), {@code targetMap} (e.g. "maps/coop.tmx") and, optionally,
 * {@code passes}: who may go through, e.g. "PLAYER,CHICKEN" (everyone without it), and {@code bedtime}: who goes
 * through it to sleep, e.g. "CHICKEN" on the farm's door to the coop (nobody without it). It blocks nothing and draws
 * nothing: the map draws the door.
 */
public class TileDoor extends TileObject {
    private final String name;
    private final String targetMap;
    /** Who may go through; empty: everyone. */
    private final List<GameActorType> passes;
    /** Who goes through it to sleep; empty: nobody. */
    private final List<GameActorType> bedtime;

    /**
     * @param passes  who may go through, e.g. "PLAYER,CHICKEN"; everyone when null or empty
     * @param bedtime who goes through it to sleep, e.g. "CHICKEN"; nobody when null or empty
     */
    public TileDoor(String name, String targetMap, String passes, String bedtime) {
        super(TileObjectType.DOOR);
        this.name = name;
        this.targetMap = targetMap;
        this.passes = parse(passes);
        this.bedtime = parse(bedtime);
    }

    private static List<GameActorType> parse(String passes) {
        List<GameActorType> types = new ArrayList<>();
        if (passes != null) {
            for (String part : passes.split(",")) {
                if (!part.trim().isEmpty()) {
                    types.add(GameActorType.valueOf(part.trim()));
                }
            }
        }
        return Collections.unmodifiableList(types);
    }

    /** The door's name: on the other map one comes out of the door with the same name. */
    public String getName() {
        return name;
    }

    /** The map it leads to, e.g. "maps/coop.tmx". */
    public String getTargetMap() {
        return targetMap;
    }

    /** May this kind (the player, a cow, a chicken) go through? */
    public boolean lets(GameActorType type) {
        return passes.isEmpty() || passes.contains(type);
    }

    /** Does this kind go through it to sleep (and may go through)? */
    public boolean isBedtimeFor(GameActorType type) {
        return bedtime.contains(type) && lets(type);
    }

    @Override
    public void prepare(Tile tile, GameContext context) {
        // nothing: the doors feature finds the door on its tile
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
