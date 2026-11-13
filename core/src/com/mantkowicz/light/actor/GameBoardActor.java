package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.context.GameContext;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.board.TilePoint.CENTER;

public abstract class GameBoardActor extends GameActor implements Interactive, TileOccupant {
    /** What can be done with it in the radial menu besides reading its description (the menu adds that one). */
    private final List<Interaction> interactions = new ArrayList<>();
    private final BoardService boardService;
    private final String description;
    private Tile tile;

    protected GameBoardActor(GameActorType gameActorType, GameContext context, String description) {
        super(gameActorType);
        this.description = description;
        this.boardService = context.getBoardService();

        addListener(new OpenMenuOnHoldListener(this, context.getGameEventService()));
    }

    public String getDescription() {
        return description;
    }

    /**
     * Shows the actor's image, looked up by name in the resources (atlas region or preloaded texture).
     */
    protected void createAvatar(GameContext context, String avatarName) {
        createAvatar(context.getResourcesService().getTexture(avatarName));
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        enterTile(tile);

        Vector2 position = tile.calculatePositionForActorAt(this, CENTER);
        this.setPosition(position.x, position.y);
    }

    /**
     * Moves the actor onto the tile on the board (which triggers collisions) without changing where it is drawn.
     * Used while walking, when the actor crosses into a tile somewhere between tile centres.
     */
    public void enterTile(Tile tile) {
        if (this.tile != null) {
            boardService.unregisterOccupant(this.tile, this);
        }

        this.tile = tile;

        boardService.registerOccupant(tile, this);
    }

    @Override
    public boolean remove() {
        if (this.tile != null) {
            boardService.unregisterOccupant(this.tile, this);
        }
        return super.remove();
    }

    @Override
    public List<Interaction> getInteractions() {
        return interactions;
    }
}
