package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
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

    private final Item item;
    private final Image picture;

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

    /** On its tile: it has landed and can be picked up. */
    public boolean isLanded() {
        return getTile() != null;
    }

    public Item getItem() {
        return item;
    }
}
