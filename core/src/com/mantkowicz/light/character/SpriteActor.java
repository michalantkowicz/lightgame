package com.mantkowicz.light.character;

import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.plugin.SpriteAnimationPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

/**
 * A board actor drawn from one Sprout Lands sheet: a picture one tile big (it may stick out) that plays the sheet's
 * animations. A subclass only picks its sprite, description and behaviours (plugins), e.g.
 * <pre>{@code
 * public Cow(GameContext context) {
 *     super(COW, context, Sprite.COW_BROWN, Action.IDLE, "A brown cow.");
 *     playAnimation(Action.IDLE);
 * }
 * }</pre>
 * and is placed in Tiled with the objectClass of its {@link com.mantkowicz.light.map.TileObjectType} line, e.g.
 * {@code COW(Cow::new)}. It blocks its tile.
 */
public abstract class SpriteActor extends GameBoardActor {
    private final Sprite sprite;
    private final ResourcesService resources;
    private final SpriteAnimationPlugin animationPlugin;

    /**
     * @param firstFrame the row (without a direction) whose first frame is shown until an animation is played
     */
    protected SpriteActor(GameActorType type, GameContext context, Sprite sprite, Action firstFrame,
            String description) {
        super(type, context, description);
        this.sprite = sprite;
        this.resources = context.getResourcesService();
        createAvatar(resources.getAnimation(sprite, firstFrame).getKeyFrame(0), resources.getSpriteSheet(sprite));

        animationPlugin = new SpriteAnimationPlugin(this);
        addPlugin(animationPlugin);
    }

    /**
     * Plays one of the sprite's rows without a direction (named after the action, e.g. "idle") from its start.
     */
    protected void playAnimation(Action action) {
        animationPlugin.play(ResourcesService.animationName(sprite, action, null),
                resources.getAnimation(sprite, action));
    }

    /**
     * Plays the row backwards, from its last frame to its first, and stops there.
     */
    protected void playBackwards(Action action) {
        animationPlugin.playBackwards(ResourcesService.animationName(sprite, action, null),
                resources.getAnimation(sprite, action));
    }

    /**
     * Jumps to the end of the current animation (e.g. a chest that starts closed shows its closed frame at once).
     */
    protected void finishAnimation() {
        animationPlugin.finish();
    }

    public Sprite getSprite() {
        return sprite;
    }

    /**
     * The animation being played, e.g. "cow_brown/idle" or "chest/open_front (backwards)".
     */
    public String getCurrentAnimation() {
        return animationPlugin.getCurrentAnimation();
    }
}
