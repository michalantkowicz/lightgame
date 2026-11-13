package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.actor.PlayerMoveEvent;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileTouchedEvent;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.Status.IDLE;
import static com.mantkowicz.light.actor.Status.MOVEMENT;

/**
 * Walks the player to the touched tile: along the A* path, but in straight lines wherever the line stays on walkable tiles,
 * at a constant speed ({@code secondsPerTile} seconds per tile) and without stopping on the way. A touch during a walk re-routes it
 * from where the player is.
 * <p>
 * The board still sees the walk tile by tile: when the player's centre crosses into a tile, a {@link PlayerMoveEvent} says it
 * left its tile, and in the next frame it enters the new one (collisions, collecting).
 */
public class BoardMovementPlugin implements Plugin {
    /** How far (in tiles) a straight line keeps away from blocked or missing tiles. */
    private static final float CLEARANCE = 0.25f;

    private final Player player;
    private final GameEventService gameEventService;
    private final BoardService boardService;
    private final float secondsPerTile;
    private Tile tileToEnter;

    public BoardMovementPlugin(Player player, GameContext context, float secondsPerTile) {
        this.player = player;
        this.gameEventService = context.getGameEventService();
        this.boardService = context.getBoardService();
        this.secondsPerTile = secondsPerTile;
    }

    @Override
    public void run(float delta) {
        TileTouchedEvent gameEvent = gameEventService.poll(TileTouchedEvent.class);
        if (gameEvent != null) {
            walkTo(gameEvent.getTile());
        }
        followTiles();
    }

    /**
     * The clicked tile, or for something used from a neighbouring tile (a chest) the free side neighbour (not a diagonal
     * one) with the shortest walk (where the player stands if it's already there).
     */
    private Tile walkTarget(Tile clicked, Tile current) {
        if (clicked.isAccessible() || !boardService.isUsedFromNeighbour(clicked)) {
            return clicked;
        }
        if (clicked.getSideNeighbours().contains(current)) {
            return current;
        }
        Tile best = clicked;
        int shortest = Integer.MAX_VALUE;
        for (Tile neighbour : clicked.getSideNeighbours()) {
            int length = neighbour.isAccessible() ? boardService.getPath(current, neighbour).getPathNodes().size() : 0;
            if (length > 0 && length < shortest) {
                best = neighbour;
                shortest = length;
            }
        }
        return best;
    }

    private void walkTo(Tile clicked) {
        Tile current = tileUnderPlayer();
        Tile target = walkTarget(clicked, current);
        List<Tile> nodes = new ArrayList<>(boardService.getPath(current, target).getPathNodes());
        if (nodes.isEmpty() && (target != current || IDLE.equals(player.getStatus()))) {
            return; // unreachable, or already standing there
        }
        float tileSize = current.getHeight();
        float clearance = CLEARANCE * tileSize;
        Vector2 start = playerCenter();
        // off-centre (re-routing mid-walk) the way to the first tile may not be straight: go through the current centre first
        if (nodes.isEmpty() || !boardService.isWalkableLine(start, nodes.get(0).getCenter(), clearance)) {
            nodes.add(0, current);
        }

        List<Vector2> positions = new ArrayList<>();
        positions.add(toPosition(start));
        float length = 0;
        Vector2 previous = start;
        for (Vector2 corner : corners(start, nodes, clearance)) {
            length += previous.dst(corner);
            positions.add(toPosition(corner));
            previous = corner;
        }

        player.clearActions();
        player.setStatus(MOVEMENT);
        player.addAction(Actions.sequence(
                new FollowPathAction(positions, secondsPerTile * length / tileSize),
                Actions.run(this::arrive)));
    }

    /**
     * Path smoothing: keeps only the corners of the path. From each point it jumps to the furthest later tile centre
     * that can be reached in a straight walkable line.
     */
    private List<Vector2> corners(Vector2 start, List<Tile> nodes, float clearance) {
        List<Vector2> corners = new ArrayList<>();
        Vector2 from = start;
        int index = 0;
        while (index < nodes.size()) {
            int furthest = index;
            for (int candidate = nodes.size() - 1; candidate > index; candidate--) {
                if (boardService.isWalkableLine(from, nodes.get(candidate).getCenter(), clearance)) {
                    furthest = candidate;
                    break;
                }
            }
            from = nodes.get(furthest).getCenter();
            corners.add(from);
            index = furthest + 1;
        }
        return corners;
    }

    /** Tells the board which tile the player is on, one frame behind the drawing (see the class comment). */
    private void followTiles() {
        enterPendingTile();
        Tile under = boardService.getTileAt(playerCenter());
        if (under != null && under != player.getTile()) {
            gameEventService.post(new PlayerMoveEvent(player));
            tileToEnter = under;
        }
    }

    private void arrive() {
        enterPendingTile();
        player.setStatus(IDLE);
    }

    private void enterPendingTile() {
        if (tileToEnter != null) {
            player.enterTile(tileToEnter);
            tileToEnter = null;
        }
    }

    /** The tile under the player's centre (the board may not have been told about it yet). */
    private Tile tileUnderPlayer() {
        Tile under = boardService.getTileAt(playerCenter());
        return under != null ? under : player.getTile();
    }

    private Vector2 playerCenter() {
        return new Vector2(player.getX(Align.center), player.getY(Align.center));
    }

    private Vector2 toPosition(Vector2 center) {
        return new Vector2(center.x - player.getWidth() / 2f, center.y - player.getHeight() / 2f);
    }
}
