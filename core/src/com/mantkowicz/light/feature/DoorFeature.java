package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.map.TileDoor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The doors of a map (Tiled's DOOR objects, {@link TileDoor}). Whoever ends its walk on a door's tile and may go
 * through it (one that {@link GameBoardActor#goesThroughDoors()}, the rabbit and the farm animals, of a kind the door
 * lets through) goes to the other map ({@link Travel}); just
 * walking over the tile on the way somewhere else takes nobody through. Whoever has just come out of a door stands on
 * it without going back: it goes through again only after it has walked off and back on.
 */
public class DoorFeature implements Feature {
    private final BoardService boardService;
    private final Travel travel;
    private final Map<Tile, TileDoor> doors = new LinkedHashMap<>();
    /** Who came out of a door and still stands on it. */
    private final List<GameBoardActor> cameOut = new ArrayList<>();

    public DoorFeature(GameContext context, Travel travel) {
        this.boardService = context.getBoardService();
        this.travel = travel;
        for (Tile tile : boardService.getTiles()) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileDoor door) {
                    doors.put(tile, door);
                }
            }
        }
    }

    @Override
    public void run(float delta) {
        cameOut.removeIf(actor -> actor.getTile() == null || !doors.containsKey(actor.getTile()));
        for (Map.Entry<Tile, TileDoor> entry : doors.entrySet()) {
            for (TileOccupant occupant : boardService.getOccupants(entry.getKey())) {
                if (occupant instanceof GameBoardActor actor && goesThrough(actor, entry.getValue())) {
                    travel.go(actor, entry.getValue());
                }
            }
        }
    }

    /** Stopped on the door (didn't just come out of it), and the door lets its kind through. */
    private boolean goesThrough(GameBoardActor actor, TileDoor door) {
        return actor.goesThroughDoors() && !cameOut.contains(actor)
                && Status.IDLE.equals(actor.getStatus()) && door.lets(actor.getGameActorType());
    }

    /** It has just come out of a door onto this map: it doesn't go back until it has walked off the door. */
    public void cameOut(GameBoardActor actor) {
        cameOut.add(actor);
    }

    public boolean isDoor(Tile tile) {
        return doors.containsKey(tile);
    }

    /** The tile of the door with this name, or null when the map has none. */
    public Tile doorTile(String name) {
        for (Map.Entry<Tile, TileDoor> entry : doors.entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
