package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.SpriteAnimationPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

import static com.mantkowicz.light.actor.GameActorType.COW;

/**
 * A brown cow standing on its tile, playing its idle animation. It blocks its tile (see TileCow) and can be held for
 * its description, like the dog.
 */
public class Cow extends GameBoardActor {
    private static final Sprite SPRITE = Sprite.COW_BROWN;
    private static final String DESCRIPTION = "A brown cow. It looks calm and a bit hungry.";

    private final SpriteAnimationPlugin animationPlugin;

    public Cow(GameContext context) {
        super(COW, context, DESCRIPTION);
        ResourcesService resources = context.getResourcesService();
        createAvatar(resources.getAnimation(SPRITE, Action.IDLE).getKeyFrame(0), resources.getSpriteSheet(SPRITE));
        setTouchable(Touchable.enabled);

        animationPlugin = new SpriteAnimationPlugin(this);
        animationPlugin.play(ResourcesService.animationName(SPRITE, Action.IDLE, null), resources.getAnimation(SPRITE, Action.IDLE));
        addPlugin(animationPlugin);
    }

    /**
     * The animation being played, e.g. "cow_brown/idle".
     */
    public String getCurrentAnimation() {
        return animationPlugin.getCurrentAnimation();
    }
}
