package com.mantkowicz.light.feature;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Grass;
import com.mantkowicz.light.actor.PictureBubble;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.board.TileOccupant;
import com.mantkowicz.light.character.Field;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.map.TileGrass;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.util.Dice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grass grows on the map, and the rabbit cuts it with its scythe ({@link com.mantkowicz.light.debug.FeatureFlag#GROWING_GRASS}).
 * <ul>
 * <li>At the start there is the grass the map puts on its tiles (Tiled's GRASS objects, {@link TileGrass}), as far
 * grown as they say.</li>
 * <li>Every {@link #SECONDS_BETWEEN_SPROUTS} each tile the rabbit can get to (also one an animal stands on; never a
 * chest's or a field's, which nobody stands on) without grass may sprout some: {@link #CHANCE}, or
 * {@link #CHANCE_NEXT_TO_GRASS} next to (diagonally too) a tile with grass, so it spreads.</li>
 * <li>Grass grows a phase every {@link #SECONDS_PER_PHASE}, as crops do: a tuft and two tufts stay behind whoever stands
 * there, a bush and a big bush are drawn over them ({@link #TALL_ALPHA}), over everything on the map but the wish
 * bubbles (wishes stay readable); only the HUD and the action buttons are in front. It changes nothing else: it blocks nothing, taps go
 * through it.</li>
 * <li>Standing still on grass the rabbit has the scythe on the HUD's tool button (the big round button right of the
 * face, the hint elsewhere): tapped, it swings it once (0.8 s) and the grass on its tile and on every tile around it
 * (diagonally too) fades out ({@link #FADE_SECONDS}). Walking off meanwhile, the grass stays.</li>
 * </ul>
 * Its dice are its own (a copy of the game's at the start, which it doesn't draw from), so the animals' choices stay
 * as they are with or without grass.
 */
public class GrassFeature implements Feature {
    // the settings
    /** How often each free tile may sprout grass. */
    public static final float SECONDS_BETWEEN_SPROUTS = 30;
    /** The chance a tile sprouts grass each time; next to grass it is likelier (it spreads). */
    public static final float CHANCE = 0.001f, CHANCE_NEXT_TO_GRASS = 0.01f;
    /** It grows a phase this often: as the crops on a field. */
    public static final float SECONDS_PER_PHASE = Field.SECONDS_PER_PHASE;
    /** Tall grass is drawn over whoever stands there this see-through (1 not at all). */
    public static final float TALL_ALPHA = 0.9f;
    /** Cut grass fades out in this long. */
    public static final float FADE_SECONDS = 0.35f;
    /** The scythe's name on the tool button. */
    public static final String SCYTHE = "scythe";

    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final RandomXS128 random;
    /** Short grass: right over the board, under everyone. Tall grass: over everyone. */
    private final Group below = new Group(), above = new Group();
    private final Map<Tile, Grass> grass = new LinkedHashMap<>();
    private List<Tile> reachable;
    private float sinceSprouts;
    /** Is the scythe on the tool button now? */
    private boolean scytheShown;
    /** Where the rabbit stands swinging its scythe, until the grass there and around is cut; null otherwise. */
    private Tile cuttingAt;

    public GrassFeature(GameContext context, Player player, Hud hud) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        this.random = Dice.copyOfTheGames();
        below.setTouchable(Touchable.disabled);
        above.setTouchable(Touchable.disabled);
        Stage stage = context.getGameStage();
        stage.getRoot().addActorAt(1, below); // the board is the first: right over it
        stage.addActor(above);
        plantWhatTheMapSays();
    }

    /** The grass put on the map in Tiled (objectClass GRASS), as far grown as its phase says. */
    private void plantWhatTheMapSays() {
        for (Tile tile : context.getBoardService().getTiles()) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileGrass mark) {
                    sprout(tile).growTo(mark.getPhase() - TileGrass.FIRST_PHASE);
                }
            }
        }
    }

    @Override
    public void run(float delta) {
        keepTallGrassInFront();
        sinceSprouts += delta;
        if (sinceSprouts >= SECONDS_BETWEEN_SPROUTS) {
            sinceSprouts -= SECONDS_BETWEEN_SPROUTS;
            sprout();
        }
        for (Grass tuft : grass.values()) {
            if (tuft.isTall() && tuft.getParent() == below) {
                above.addActor(tuft);
            }
        }
        cut();
        showScythe();
    }

    /** Tall grass over whoever was added to the map since, but under the wish bubbles (they stay in front). */
    private void keepTallGrassInFront() {
        above.toFront();
        Stage stage = context.getGameStage();
        List<Actor> bubbles = new ArrayList<>();
        for (Actor actor : stage.getActors()) {
            if (actor instanceof PictureBubble) {
                bubbles.add(actor);
            }
        }
        for (Actor bubble : bubbles) {
            bubble.toFront();
        }
    }

    /** Each tile the rabbit can get to and without grass may sprout some; next to grass, likelier. */
    private void sprout() {
        List<Tile> sprouting = new ArrayList<>();
        for (Tile tile : reachableTiles()) {
            if (grass.containsKey(tile)) {
                continue;
            }
            float chance = hasGrassNextTo(tile) ? CHANCE_NEXT_TO_GRASS : CHANCE;
            if (random.nextFloat() < chance) {
                sprouting.add(tile); // all decided on the grass there was before
            }
        }
        for (Tile tile : sprouting) {
            sprout(tile);
        }
    }

    /** Grass sprouts on this tile (a tuft). */
    public Grass sprout(Tile tile) {
        Grass tuft = new Grass(context.getResourcesService(), tile, SECONDS_PER_PHASE, TALL_ALPHA);
        grass.put(tile, tuft);
        below.addActor(tuft);
        return tuft;
    }

    private boolean hasGrassNextTo(Tile tile) {
        for (Tile neighbour : tile.getNeighbours()) {
            if (grass.containsKey(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The tiles the rabbit can get to from where it started: free ones, and ones only animals (or the rabbit) stand
     * on, as they move on. A chest's or a field's tile is never stood on. Worked out once: the map doesn't change.
     */
    private List<Tile> reachableTiles() {
        if (reachable != null) {
            return reachable;
        }
        Tile start = player.getTile();
        if (start == null) {
            return List.of(); // no player on the map yet (it comes through a door later): try again next time
        }
        reachable = new ArrayList<>();
        Set<Tile> seen = new HashSet<>();
        Deque<Tile> next = new ArrayDeque<>();
        next.add(start);
        seen.add(start);
        while (!next.isEmpty()) {
            Tile tile = next.poll();
            reachable.add(tile);
            for (Tile neighbour : tile.getNeighbours()) {
                if (seen.add(neighbour) && canBeStoodOn(neighbour)) {
                    next.add(neighbour);
                }
            }
        }
        return reachable;
    }

    /** Free, or taken only by something that moves on (an animal); not by a chest or a field. */
    private boolean canBeStoodOn(Tile tile) {
        BoardService board = context.getBoardService();
        List<TileOccupant> occupants = board.getOccupants(tile);
        if (tile.isAccessible()) {
            return true;
        }
        if (occupants.isEmpty()) {
            return false; // water, a wall
        }
        for (TileOccupant occupant : occupants) {
            if (occupant instanceof Collectible) {
                return false;
            }
        }
        return true;
    }

    // cutting

    /**
     * The scythe has swung: the grass on the rabbit's tile and on every tile around it (diagonally too) fades out; if
     * the rabbit walked off meanwhile, it all stays.
     */
    private void cut() {
        if (player.getTile() == null) {
            cuttingAt = null; // the rabbit went to another map: no swing here any more
        }
        if (cuttingAt == null || player.isScything()) {
            return;
        }
        Tile at = cuttingAt;
        cuttingAt = null;
        if (Status.MOVEMENT.equals(player.getStatus()) || player.getTile() != at) {
            return;
        }
        for (Grass tuft : grassAround(at)) {
            grass.remove(tuft.getTile());
            tuft.cutDown(FADE_SECONDS);
        }
    }

    /**
     * Standing still on grass (or swinging at it) the rabbit has the scythe on the HUD's tool button; elsewhere (also
     * just next to grass) the button is the hint again.
     */
    private void showScythe() {
        boolean scythe = cuttingAt != null || canCut();
        if (scythe && !scytheShown) {
            hud.getToolButton().setTool(SCYTHE,
                    context.getResourcesService().getAnimation(Sprite.SCYTHE_ICON, Action.ICON)
                            .getKeyFrame(0),
                    this::cutHere);
        } else if (!scythe && scytheShown) {
            hud.getToolButton().resetTool();
        }
        scytheShown = scythe;
    }

    /** The tool button with the scythe tapped: the rabbit swings it at the grass where it stands and around. */
    private void cutHere() {
        if (canCut()) {
            cuttingAt = player.getTile();
            player.scythe();
        }
    }

    /** The rabbit stands still on grass and isn't swinging already. */
    private boolean canCut() {
        Tile tile = player.getTile();
        return cuttingAt == null && tile != null && Status.IDLE.equals(player.getStatus())
                && grass.containsKey(tile);
    }

    /** The grass on this tile and on every tile around it, diagonally too: what one swing of the scythe cuts. */
    private List<Grass> grassAround(Tile tile) {
        List<Grass> around = new ArrayList<>();
        Grass here = grass.get(tile);
        if (here != null) {
            around.add(here);
        }
        for (Tile neighbour : tile.getNeighbours()) {
            Grass tuft = grass.get(neighbour);
            if (tuft != null) {
                around.add(tuft);
            }
        }
        return around;
    }

    /** The grass on the map now, by tile (for tests). */
    public Map<Tile, Grass> getGrass() {
        return grass;
    }

    /** Is this grass drawn over whoever stands on its tile? */
    public boolean isDrawnOverEveryone(Grass tuft) {
        return tuft.getParent() == above;
    }
}
