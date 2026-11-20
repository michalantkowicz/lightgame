package com.mantkowicz.light.character;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.resources.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Frog} going for a crop lying on the ground and eating it: it notices one within {@link #CROP_REACH} tiles
 * that nobody else goes for, goes to stand beside it (left or right: its tongue shoots sideways) where nobody is near,
 * and its tongue reaches for it a few times while it pulses; then it is eaten. Its states ({@link FrogState}) use this.
 */
class FrogMeal {
    /** How far away it notices a crop on the ground (in tiles, diagonally too). */
    static final int CROP_REACH = 4;

    private final Frog frog;
    private final Stage stage;
    private final FrogSurroundings around;
    /** The crop it goes for or eats, or null. */
    private ItemOnGround crop;
    /** Licks still to come before the crop is eaten. */
    private int licksLeft;

    FrogMeal(Frog frog, Stage stage, FrogSurroundings around) {
        this.frog = frog;
        this.stage = stage;
        this.around = around;
    }

    /** Notices the nearest crop it can get beside, and plans its way there (or is there already). */
    boolean spot() {
        for (ItemOnGround lying : cropsNear()) {
            List<Tile> besides = besides(lying.getTile());
            if (besides.contains(frog.getTile()) || frog.planWay(besides::contains)) {
                crop = lying;
                crop.setEater(frog);
                return true;
            }
        }
        return false;
    }

    /** The crops lying within its reach that nobody goes for, the nearest first. */
    private List<ItemOnGround> cropsNear() {
        Tile at = frog.getTile();
        List<ItemOnGround> crops = new ArrayList<>();
        for (Actor actor : stage.getActors()) {
            if (actor instanceof ItemOnGround lying && isFree(lying)
                    && FrogSurroundings.distance(at, lying.getTile()) <= CROP_REACH) {
                crops.add(lying);
            }
        }
        crops.sort((a, b) -> FrogSurroundings.distance(at, a.getTile()) - FrogSurroundings.distance(at, b.getTile()));
        return crops;
    }

    private static boolean isFree(ItemOnGround lying) {
        return lying.isLanded() && lying.getItem() instanceof Crop && lying.getEater() == null;
    }

    /** Where it eats a crop on this tile from: land left and right of it where nobody is near. */
    private List<Tile> besides(Tile cropTile) {
        List<Tile> tiles = new ArrayList<>();
        for (Tile tile : cropTile.getSideNeighbours()) {
            if (Math.abs(tile.getCenter().y - cropTile.getCenter().y) < 1 && around.isSafeLand(tile)) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    /** The crop still lies there for it (nobody picked it up). */
    boolean isThere() {
        return crop != null && crop.hasParent() && crop.isLanded() && crop.getEater() == frog;
    }

    boolean isBeside() {
        return isThere() && besides(crop.getTile()).contains(frog.getTile());
    }

    /** Turns to the crop and starts eating, its tongue reaching for it this many times; the crop pulses meanwhile. */
    void start(int licks) {
        frog.setMirrored(crop.getX() < frog.getX());
        crop.setPecked(true);
        licksLeft = licks;
        frog.playOnce(Action.TONGUE);
    }

    /**
     * Its tongue has reached for the crop once more.
     *
     * @return true when that was the last time (the crop can be eaten); else its tongue reaches for it again
     */
    boolean lick() {
        licksLeft--;
        if (licksLeft > 0) {
            frog.playOnce(Action.TONGUE);
        }
        return licksLeft <= 0;
    }

    /** Eaten: the crop shrinks and fades out. */
    void eat() {
        crop.eatUp();
        crop = null;
    }

    /** It goes for the crop no more (someone came near, or it is gone): it lies as it lay, for anyone to take. */
    void letGo() {
        if (crop != null && crop.getEater() == frog) {
            crop.setPecked(false);
            crop.setEater(null);
        }
        crop = null;
    }

    /** The crop it goes for or eats, or null. */
    ItemOnGround getCrop() {
        return crop;
    }
}
