package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Carrot;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.ThumbnailType;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

/**
 * The Sprout Lands chest, holding a carrot. It blocks its tile, so the player uses it from a neighbouring tile: walking
 * next to it shows its button. Opening it lifts the lid, which stays open, and what is inside jumps out.
 */
public class Chest extends SpriteActor implements Collectible {
    private static final Action OPEN = Action.OPEN_FRONT;
    private static final String DESCRIPTION = "chest";
    private static final int INVENTORY_SIZE = 2;

    private final Inventory inventory;
    private final TextureRegion closed, open;
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
        // the open chest (the lid up) without the empty column and row around it
        Animation<TextureRegion> opening = context.getResourcesService().getAnimation(Sprite.CHEST, OPEN);
        open = new TextureRegion(opening.getKeyFrame(opening.getAnimationDuration(), false), 1, 1, 16, 20);

        List<Item> content = new ArrayList<>();
        content.add(new Carrot(context.getResourcesService()));
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
     * What the button does: the open chest while it is closed, the closed chest while it is open.
     */
    @Override
    public TextureRegion getActionIcon() {
        return isOpen ? closed : open;
    }

    /** The closed chest, as the player sees it on the map: the button's picture while the chest is open. */
    public TextureRegion getClosedIcon() {
        return closed;
    }

    /** It opens and closes as often as the player likes; what was inside comes out only once. */
    @Override
    public boolean canBeClosed() {
        return true;
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
