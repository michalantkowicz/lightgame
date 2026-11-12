package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.ThumbnailType;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

/**
 * The Sprout Lands chest, holding a lighter. It blocks its tile, so the player uses it from a neighbouring tile: walking
 * next to it shows the COLLECT button. The lid opens when the collect window opens and shuts when it closes.
 */
public class Chest extends SpriteActor implements Collectible {
    private static final Action OPEN = Action.OPEN_FRONT;
    private static final String DESCRIPTION = "chest";
    private static final int INVENTORY_SIZE = 2;

    private final Inventory inventory;
    private final TextureRegion closed;

    public Chest(GameContext context) {
        super(CHEST, context, Sprite.CHEST, OPEN, DESCRIPTION);
        setName("Nice Chest");
        setTouchable(Touchable.disabled);
        // the closed chest without the empty rows above it in its frame
        closed = new TextureRegion(context.getResourcesService().getAnimation(Sprite.CHEST, OPEN).getKeyFrame(0), 1, 7, 16, 14);

        List<Item> content = new ArrayList<>();
        content.add(new Lighter(context));
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
     * The closed chest, as the player sees it on the map.
     */
    @Override
    public TextureRegion getActionIcon() {
        return closed;
    }

    /**
     * The collect window opens: the lid opens.
     */
    @Override
    public void beforeCollect() {
        playAnimation(OPEN);
    }

    /**
     * The collect window closes: the lid shuts.
     */
    @Override
    public void afterCollect() {
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
