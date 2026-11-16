package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.ResourcesService;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.forever;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.run;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import static com.mantkowicz.light.actor.GameActorType.ITEM_ON_GROUND;
import static com.mantkowicz.light.board.TilePoint.CENTER;
import static com.mantkowicz.light.board.WorldScale.TILE_SIZE;

/**
 * An item lying on a tile, bobbing gently up and down over its shadow as in old games, until someone picks it up. It
 * gets there by flying in an arc (thrown out of a chest or by the player); only once it has landed is it on its tile.
 * It doesn't block the tile and clicks go through it, so clicking it walks there.
 */
public class ItemOnGround extends GameBoardActor {
    /** The item's pixels per art pixel: a 16 px item is 96 px, a bit smaller than its 128 px tile. */
    private static final float SCALE = 6;
    /** Where the picture rests: its bottom this high over the tile's bottom; how high it bobs, and how fast. */
    private static final float REST_Y = (TILE_SIZE - 16 * SCALE) / 2f, BOB_HEIGHT = 10, BOB_SECONDS = 0.6f;
    /** How long a throw takes and how high the item flies over the straight line. */
    static final float FLIGHT_SECONDS = 0.6f;
    private static final float FLIGHT_HEIGHT = 110;
    private static final String SHADOW = "item_shadow/row_00";
    /** Pecked at, it pulses: this much bigger at most, and a grow and shrink take this long. */
    private static final float PULSE_SIZE = 1.2f, PULSE_SECONDS = 0.5f;
    /** Eaten, it shrinks to this size while it fades out, in this long (as cut grass does). */
    private static final float EATEN_SIZE = 0.2f, EATEN_SECONDS = 0.35f;

    private final Item item;
    private final Image picture;
    /** Who goes for it to eat it (a chicken), or null: nobody else goes for it meanwhile. */
    private Actor eater;
    /** The pulse while it is pecked at, or null. */
    private Action pulse;
    /** Eaten: it is going (shrinking, fading out), nobody can pick it up any more. */
    private boolean eaten;

    public ItemOnGround(GameContext context, Item item) {
        super(ITEM_ON_GROUND, context, "item");
        this.item = item;
        ResourcesService resources = context.getResourcesService();
        setSize(TILE_SIZE, TILE_SIZE);
        setTouchable(Touchable.disabled);

        TextureRegion shadowRegion = resources.getTexture(SHADOW); // 14x4, at the bottom of its 16x16 frame, 1 px in
        Image shadow = new Image(shadowRegion);
        shadow.setBounds((TILE_SIZE - 16 * SCALE) / 2f + SCALE, REST_Y, shadowRegion.getRegionWidth() * SCALE,
                shadowRegion.getRegionHeight() * SCALE);
        addActor(shadow);

        TextureRegion itemRegion = item.getPicture();
        picture = new Image(itemRegion);
        picture.setSize(itemRegion.getRegionWidth() * SCALE, itemRegion.getRegionHeight() * SCALE);
        picture.setPosition((TILE_SIZE - picture.getWidth()) / 2f, REST_Y);
        addActor(picture);
    }

    /**
     * Flies from a point (the stage point its middle starts at, e.g. a chest's middle) onto a tile, in an arc; then it
     * is on that tile, bobs, and runs {@code landed}.
     */
    public void flyOnto(Vector2 from, Tile target, Runnable landed) {
        setPosition(from.x - getWidth() / 2f, from.y - getHeight() / 2f);
        Vector2 end = target.calculatePositionForActorAt(this, CENTER);
        picture.addAction(sequence(Actions.moveBy(0, FLIGHT_HEIGHT, FLIGHT_SECONDS / 2, Interpolation.pow2Out),
                Actions.moveBy(0, -FLIGHT_HEIGHT, FLIGHT_SECONDS / 2, Interpolation.pow2In)));
        addAction(sequence(Actions.moveTo(end.x, end.y, FLIGHT_SECONDS), run(() -> {
            setTile(target);
            bob();
            landed.run();
        })));
    }

    private void bob() {
        picture.addAction(forever(sequence(Actions.moveBy(0, BOB_HEIGHT, BOB_SECONDS, Interpolation.sine),
                Actions.moveBy(0, -BOB_HEIGHT, BOB_SECONDS, Interpolation.sine))));
    }

    /**
     * Pecked at (a chicken eats it) it pulses, getting a fifth bigger and smaller again; then it bobs as before.
     */
    public void setPecked(boolean pecked) {
        if (pecked == isPecked()) {
            return;
        }
        if (pecked) {
            picture.setOrigin(picture.getWidth() / 2f, picture.getHeight() / 2f);
            pulse = forever(sequence(Actions.scaleTo(PULSE_SIZE, PULSE_SIZE, PULSE_SECONDS / 2, Interpolation.sine),
                    Actions.scaleTo(1, 1, PULSE_SECONDS / 2, Interpolation.sine)));
            picture.addAction(pulse);
        } else {
            picture.removeAction(pulse);
            pulse = null;
            picture.setScale(1);
        }
    }

    public boolean isPecked() {
        return pulse != null;
    }

    /** How big its picture is drawn now (1 its size; more while it pulses). */
    public float getPictureScale() {
        return picture.getScaleX();
    }

    /** Who goes for it to eat it (a chicken), or null. */
    public Actor getEater() {
        return eater;
    }

    public void setEater(Actor eater) {
        this.eater = eater;
    }

    /**
     * Eaten (by a chicken): it stops pulsing, shrinks while it fades out and is gone; meanwhile nobody can pick it up.
     */
    public void eatUp() {
        setPecked(false);
        eaten = true;
        picture.clearActions();
        picture.setOrigin(picture.getWidth() / 2f, 0);
        addAction(sequence(Actions.parallel(Actions.run(() -> picture.addAction(Actions.parallel(
                Actions.scaleTo(EATEN_SIZE, EATEN_SIZE, EATEN_SECONDS), Actions.fadeOut(EATEN_SECONDS)))),
                Actions.delay(EATEN_SECONDS)), Actions.removeActor()));
    }

    /** On its tile: it has landed and can be picked up (not once it is being eaten up). */
    public boolean isLanded() {
        return getTile() != null && !eaten;
    }

    public Item getItem() {
        return item;
    }
}
