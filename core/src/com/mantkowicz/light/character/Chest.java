package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.ThumbnailType;
import com.mantkowicz.light.resources.UiImage;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

/**
 * The Sprout Lands chest, holding a crop picked at random (a carrot, corn...). It blocks its tile, so the player uses it from a neighbouring tile: walking
 * next to it shows its button, a gold star (treasure inside). Opening it lifts the lid, which stays open, and what is
 * inside jumps out; empty, it has no button any more.
 */
public class Chest extends SpriteActor implements Collectible {
    private static final Action OPEN = Action.OPEN_FRONT;
    private static final String DESCRIPTION = "chest";
    private static final int INVENTORY_SIZE = 2;

    private final Inventory inventory;
    private final TextureRegion closed, treasure;
    private boolean isOpen;
    /** How high it is drawn, as of the end of its last opening or closing (it doesn't change while the lid moves). */
    private float topY;

    public Chest(GameContext context) {
        super(CHEST, context, Sprite.CHEST, OPEN, DESCRIPTION);
        setName("Nice Chest");
        setTouchable(Touchable.disabled);
        // the closed chest without the empty rows above it in its frame
        closed = new TextureRegion(context.getResourcesService().getAnimation(Sprite.CHEST, OPEN).getKeyFrame(0), 1, 7,
                16, 14);
        treasure = context.getResourcesService().getUiImage(UiImage.TREASURE);

        List<Item> content = new ArrayList<>();
        content.add(new Crop(CropType.random(), context.getResourcesService()));
        inventory = new Inventory(INVENTORY_SIZE, new ArrayList<Item>());
        inventory.setItemsAndCompleteWithNulls(content);

        playBackwards(OPEN);
        finishAnimation(); // starts closed
    }

    /**
     * Used from a neighbouring tile: the chest blocks its own.
     */
    @Override
    public boolean isDistant() {
        return true;
    }

    /**
     * The button's picture: a gold star, there is treasure inside. (Once open it has no button: it stays open and
     * empty.)
     */
    @Override
    public TextureRegion getActionIcon() {
        return treasure;
    }

    /** The closed chest, as the player sees it on the map (the button's picture before the star). */
    public TextureRegion getClosedIcon() {
        return closed;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isBusy() {
        return !isAnimationFinished();
    }

    /**
     * Its tile's top while it is closed; open, the top of its lid, which sticks out above the tile (the picture's top
     * row is empty). While the lid moves, where it was before.
     */
    @Override
    public float getTopY() {
        if (!isBusy()) {
            topY = isOpen ? getAvatar().getY() + getAvatar().getHeight() - WorldScale.PIXEL_SCALE : getHeight();
        }
        return topY;
    }

    @Override
    public Actor getActor() {
        return this;
    }

    /**
     * It is opened: the lid opens (and stays open).
     */
    @Override
    public void beforeCollect() {
        isOpen = true;
        playAnimation(OPEN);
    }

    /**
     * It is closed: the lid shuts.
     */
    @Override
    public void afterCollect() {
        isOpen = false;
        playBackwards(OPEN);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
