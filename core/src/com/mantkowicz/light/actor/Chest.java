package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.SpriteAnimationPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.ThumbnailType;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.CHEST;

/**
 * The Sprout Lands chest. It blocks its tile (see TileChest), so the player uses it from a neighbouring tile: walking
 * next to it shows the COLLECT button. The lid opens when the collect window opens and shuts when it closes.
 */
public class Chest extends GameBoardActor implements Collectible {
    private static final Sprite SPRITE = Sprite.CHEST;
    private static final Action OPEN = Action.OPEN_FRONT;
    private static final String DESCRIPTION = "chest";
    private static final int INVENTORY_SIZE = 2;

    private final Inventory inventory;
    private final Animation<TextureRegion> openAnimation;
    private final String openAnimationName;
    private final SpriteAnimationPlugin animationPlugin;

    public Chest(GameContext context) {
        super(CHEST, context, DESCRIPTION);
        inventory = new Inventory(INVENTORY_SIZE, new ArrayList<Item>());
        setName("Nice Chest");
        ResourcesService resources = context.getResourcesService();
        openAnimation = resources.getAnimation(SPRITE, OPEN);
        openAnimationName = ResourcesService.animationName(SPRITE, OPEN, null);
        createAvatar(openAnimation.getKeyFrame(0), resources.getSpriteSheet(SPRITE));

        setTouchable(Touchable.disabled);

        animationPlugin = new SpriteAnimationPlugin(this);
        animationPlugin.playBackwards(openAnimationName, openAnimation);
        animationPlugin.finish(); // starts closed
        addPlugin(animationPlugin);
    }

    /**
     * Used from a neighbouring tile: the chest blocks its own.
     */
    @Override
    public boolean isDistant() {
        return true;
    }

    /**
     * The collect window opens: the lid opens.
     */
    @Override
    public void beforeCollect() {
        animationPlugin.play(openAnimationName, openAnimation);
    }

    /**
     * The collect window closes: the lid shuts.
     */
    @Override
    public void afterCollect() {
        animationPlugin.playBackwards(openAnimationName, openAnimation);
    }

    /**
     * The animation being played, e.g. "chest/open_front" (opening) or "chest/open_front (backwards)" (closing).
     */
    public String getCurrentAnimation() {
        return animationPlugin.getCurrentAnimation();
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
