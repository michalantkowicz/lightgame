package com.mantkowicz.light.actor;

import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.OccupantListener;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.event.GameEventService;

/**
 * When something steps onto a tile, finds what can be collected there: collectibles on that tile, and the ones used
 * from a neighbour (a chest) when this tile is one it is used from ({@link Tile#getUsedFromNeighbours()}). Each one
 * found is a {@link CollectEvent}.
 */
public class CollectibleFinder implements OccupantListener {
    private final BoardService boardService;
    private final GameEventService gameEventService;

    public CollectibleFinder(BoardService boardService, GameEventService gameEventService) {
        this.boardService = boardService;
        this.gameEventService = gameEventService;
    }

    @Override
    public void entered(TileOccupant occupant, Tile tile) {
        for (TileOccupant other : boardService.getOccupants(tile)) {
            if (!other.equals(occupant) && other instanceof Collectible collectible) {
                gameEventService.post(new CollectEvent(collectible));
            }
        }
        for (Tile neighbour : tile.getSideNeighbours()) {
            if (neighbour.getUsedFromNeighbours().contains(tile)) {
                findUsedFromNeighbour(occupant, neighbour);
            }
        }
    }

    private void findUsedFromNeighbour(TileOccupant occupant, Tile neighbour) {
        for (TileOccupant other : boardService.getOccupants(neighbour)) {
            if (!other.equals(occupant) && other instanceof Collectible collectible && collectible.isDistant()) {
                gameEventService.post(new CollectEvent(collectible));
            }
        }
    }
}
