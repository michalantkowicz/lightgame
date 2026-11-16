package com.mantkowicz.light.character;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A chicken going for a crop lying on the ground (one the player didn't pick up) to eat it; its states
 * ({@link ChickenState#GOING_FOR_CROP} and on) use this.
 * <ul>
 * <li>It notices a crop up to {@link #REACH} tiles away (diagonally too) that nobody else goes for, if it can get next
 * to it in at most {@link #MOST_STEPS} steps, and goes for the nearest. When the way gets longer than that (its side
 * got taken), it gives up.</li>
 * <li>It goes slowly: a step every {@link #SECONDS_PER_STEP}, up, down, left or right, never onto the crop's tile or
 * anyone's. It stops beside the crop, left or right of it (pecking from above or below looks bad).</li>
 * <li>It never stands in the player's way: if its next step, or where it stands while it looks or pecks, would leave
 * the player no way to a crop on the ground (to pick it up before it is eaten; its own or any other), it gives its
 * crop up (as if it had eaten it: it doesn't go for it again) and goes back.</li>
 * <li>Next to the crop it pecks at it, {@link #SECONDS_TO_EAT}, and the crop pulses meanwhile; then it is eaten.</li>
 * <li>Stroked while it goes for the crop or pecks at it, it gives the crop up for good and, after the hearts, goes
 * back.</li>
 * <li>Then, or when the crop is gone (picked up), it goes back where it started, or as near as it can get without
 * standing in the player's way to a crop, and goes on with what it did before.</li>
 * </ul>
 */
class CropHunt {
    /** How far away (in tiles, diagonally too) it notices a crop on the ground. */
    static final int REACH = 6;
    /**
     * It walks at most this many steps to a crop: a longer way round (e.g. its side of the crop got taken and the
     * other side is behind a wall) and it gives up instead of wandering off for a minute.
     */
    static final int MOST_STEPS = 2 * REACH;
    /** It takes a step towards the crop this often (not hurrying: the player has time to come and pick it up). */
    static final float SECONDS_PER_STEP = 5;
    /** Pecking at the crop this long, it has eaten it. */
    static final float SECONDS_TO_EAT = 10;
    /** Pecking, it leans this much of a tile towards the crop (standing on its own tile its beak wouldn't reach it). */
    static final float LEAN = 0.3f;

    private final Chicken chicken;
    private final BoardService board;
    /** Crops it gave up (going on would have blocked the player): it doesn't go for them again. */
    private final Set<ItemOnGround> givenUp = new HashSet<>();
    /** What it goes for, or null. */
    private ItemOnGround crop;
    /** Where it started from; on its way back, where it goes (as near as it can get). */
    private Tile home, backTo;
    /** Stroked while it went for a crop: it goes back once the stroke is over. */
    private boolean goBackAfterTheStroke;

    CropHunt(Chicken chicken, BoardService board) {
        this.chicken = chicken;
        this.board = board;
    }

    /**
     * Is there a crop on the ground near enough that nobody else goes for and that it can get next to? Then it goes
     * for the nearest (by steps), starting from where it stands now.
     */
    boolean spotCrop() {
        Tile at = chicken.getTile();
        if (at == null || chicken.getStage() == null) {
            return false;
        }
        givenUp.removeIf(lying -> lying.getStage() == null);
        ItemOnGround nearest = null;
        int fewestSteps = Integer.MAX_VALUE;
        for (ItemOnGround lying : cropsNear(at)) {
            List<Tile> steps = stepsTo(lying);
            if (steps != null && steps.size() <= MOST_STEPS && steps.size() < fewestSteps) {
                nearest = lying;
                fewestSteps = steps.size();
            }
        }
        if (nearest == null) {
            return false;
        }
        crop = nearest;
        crop.setEater(chicken);
        home = at;
        return true;
    }

    /** The crops lying near enough that nobody else goes for (and that it didn't give up). */
    private List<ItemOnGround> cropsNear(Tile at) {
        List<ItemOnGround> near = new ArrayList<>();
        for (Actor actor : chicken.getStage().getActors()) {
            if (actor instanceof ItemOnGround lying && isFood(lying) && isNear(at, lying.getTile())) {
                near.add(lying);
            }
        }
        return near;
    }

    private boolean isFood(ItemOnGround lying) {
        return lying.isLanded() && lying.getItem() instanceof Crop && lying.getEater() == null
                && !givenUp.contains(lying);
    }

    private static boolean isNear(Tile from, Tile to) {
        float tile = Math.max(from.getWidth(), 1);
        return Math.abs(to.getCenter().x - from.getCenter().x) <= REACH * tile + 1
                && Math.abs(to.getCenter().y - from.getCenter().y) <= REACH * tile + 1;
    }

    /** It still lies where it lay (nobody picked it up). */
    boolean isCropThere() {
        return crop != null && crop.getStage() != null && crop.isLanded();
    }

    /** It stands beside the crop: left or right of it. */
    boolean isNextToCrop() {
        return isCropThere() && besides(crop.getTile()).contains(chicken.getTile());
    }

    /** Where it pecks a crop on this tile from: the tiles left and right of it (not above or below). */
    private static List<Tile> besides(Tile cropTile) {
        List<Tile> spots = new ArrayList<>();
        for (Tile side : cropTile.getSideNeighbours()) {
            if (Math.abs(side.getCenter().y - cropTile.getCenter().y) < 0.5f) {
                spots.add(side);
            }
        }
        return spots;
    }

    /**
     * A step towards the crop (the chicken walks it at its pace). False when there is no way, or the step would leave
     * the player no way to the crop: then it gives the crop up for good.
     */
    boolean stepTowardsCrop() {
        List<Tile> steps = isCropThere() ? stepsTo(crop) : null;
        if (steps == null || steps.isEmpty() || steps.size() > MOST_STEPS || blocksThePlayer(steps.get(0))) {
            if (crop != null) {
                givenUp.add(crop);
            }
            return false;
        }
        chicken.stepOnto(steps.get(0), chicken.getType().getSecondsPerTile(), true);
        return true;
    }

    /** The steps to a tile next to the crop (side by side), over free tiles; null without a way. */
    private List<Tile> stepsTo(ItemOnGround lying) {
        Tile cropTile = lying.getTile();
        List<Tile> spots = besides(cropTile);
        return board.findSteps(chicken.getTile(), spots::contains,
                tile -> tile != cropTile && isFree(tile), false);
    }

    private boolean isFree(Tile tile) {
        return tile.isAccessible() && board.getOccupants(tile).isEmpty();
    }

    /**
     * Standing where it stands, does it shut the player out from a crop on the ground (e.g. beside it in a corridor, the
     * player coming from behind; or in the only gap to another crop)? Then it gives its crop up for good.
     */
    boolean blocksThePlayerHere() {
        if (!isCropThere() || !blocksThePlayer(chicken.getTile())) {
            return false;
        }
        givenUp.add(crop);
        return true;
    }

    /**
     * If it stood on {@code standing} (and nowhere else), would the player no longer get to a crop lying on the ground
     * that it could get to without the chicken in the way? Any crop, not only the one it goes for: a child running to
     * save a carrot shouldn't find a chicken waiting in the only way there.
     */
    private boolean blocksThePlayer(Tile standing) {
        Player player = findPlayer();
        if (player == null || player.getTile() == null) {
            return false;
        }
        Tile now = chicken.getTile();
        Set<Tile> withoutIt = reach(player.getTile(), tile -> tile.isAccessible() || tile == now);
        Set<Tile> withIt = reach(player.getTile(), tile -> tile != standing && (tile.isAccessible() || tile == now));
        for (Actor actor : chicken.getStage().getActors()) {
            if (actor instanceof ItemOnGround lying && lying.isLanded() && lying.getItem() instanceof Crop
                    && withoutIt.contains(lying.getTile()) && !withIt.contains(lying.getTile())) {
                return true;
            }
        }
        return false;
    }

    /** The tiles the player can get to from {@code from} over {@code passable} tiles (diagonally too, as it walks). */
    private Set<Tile> reach(Tile from, Predicate<Tile> passable) {
        Set<Tile> reached = new HashSet<>();
        board.findSteps(from, tile -> {
            reached.add(tile);
            return false;
        }, passable, true);
        return reached;
    }

    private Player findPlayer() {
        for (Actor actor : chicken.getStage().getActors()) {
            if (actor instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    /**
     * It turns to the crop beside it (left of it, it looks right, its sheet's way; right of it, left) and leans
     * towards it ({@link #LEAN} of a tile), so its beak reaches the crop.
     */
    void faceCrop() {
        float dx = crop.getTile().getCenter().x - chicken.getTile().getCenter().x;
        chicken.setMirrored(dx < -1);
        chicken.lean(Math.signum(dx) * LEAN * chicken.getTile().getWidth());
    }

    /** The crop pulses while it is pecked at, and lies still again once it isn't. */
    void setPecking(boolean pecking) {
        if (crop != null) {
            crop.setPecked(pecking);
        }
    }

    /** Eaten: the crop shrinks and fades out, and is gone. */
    void eatCrop() {
        if (isCropThere()) {
            crop.eatUp();
        }
        crop = null;
    }

    /**
     * It is done with the crop (eaten, gone, given up) and goes back: where it started, or, taken or out of reach,
     * the free tile nearest to it that it can get to.
     */
    void startGoingBack() {
        letGo();
        goBackAfterTheStroke = false;
        backTo = nearestReachableTo(home == null ? chicken.getTile() : home);
    }

    /** Done with the crop: it lies as before, for anyone to take. */
    private void letGo() {
        if (crop != null) {
            crop.setPecked(false);
            if (crop.getEater() == chicken) {
                crop.setEater(null);
            }
            crop = null;
        }
    }

    /**
     * Stroked while it goes for a crop or pecks at it (nothing happens otherwise): it gives that crop up for good, and
     * after the stroke it goes back where it started ({@link #isGoingBackAfterTheStroke()}).
     */
    void giveUpForAStroke() {
        if (crop != null) {
            givenUp.add(crop);
            letGo();
            goBackAfterTheStroke = true;
        }
    }

    /** It was stroked while it went for a crop: after the stroke it goes back where it started. */
    boolean isGoingBackAfterTheStroke() {
        return goBackAfterTheStroke;
    }

    /**
     * A step on its way back (walked at its pace); false once it is there or can't get any nearer. Where it goes is
     * chosen again before each step: a crop dropped meanwhile may make its home a bad place to stand.
     */
    boolean stepBack() {
        if (home != null) {
            backTo = nearestReachableTo(home);
        }
        if (backTo == null || backTo == chicken.getTile()) {
            return false;
        }
        Tile target = backTo;
        List<Tile> steps = board.findSteps(chicken.getTile(), tile -> tile == target, this::isFree, false);
        if (steps == null || steps.isEmpty()) {
            backTo = nearestReachableTo(home); // something stepped in the way: as near as it gets now
            return false;
        }
        chicken.stepOnto(steps.get(0), chicken.getType().getSecondsPerTile(), true);
        return true;
    }

    /**
     * Of the tiles it can get to (and its own), the nearest to {@code place} (the fewest steps when as near) where,
     * standing, it doesn't cut the player off from a crop on the ground (its home may be the only way to one); with no
     * such tile, the nearest.
     */
    private Tile nearestReachableTo(Tile place) {
        List<Tile> reachable = new ArrayList<>();
        board.findSteps(chicken.getTile(), tile -> {
            reachable.add(tile); // visited nearest first, its own tile too
            return false;
        }, this::isFree, false);
        Tile nearest = null;
        Tile nearestOutOfTheWay = null;
        for (Tile tile : reachable) {
            if (nearest == null || distance(tile, place) < distance(nearest, place) - 0.5f) {
                nearest = tile;
            }
            if ((nearestOutOfTheWay == null || distance(tile, place) < distance(nearestOutOfTheWay, place) - 0.5f)
                    && !blocksThePlayer(tile)) {
                nearestOutOfTheWay = tile;
            }
        }
        return nearestOutOfTheWay != null ? nearestOutOfTheWay : nearest;
    }

    /** How far apart two tiles are, in steps up, down and sideways (as the chicken walks). */
    private static float distance(Tile a, Tile b) {
        float tile = Math.max(a.getWidth(), 1);
        return (Math.abs(a.getCenter().x - b.getCenter().x) + Math.abs(a.getCenter().y - b.getCenter().y)) / tile;
    }

    /** The crop it goes for or pecks at, or null. */
    ItemOnGround getCrop() {
        return crop;
    }

    /** Where it started from, or null. */
    Tile getHome() {
        return home;
    }
}
