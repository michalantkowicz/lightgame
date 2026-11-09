package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;

import static com.mantkowicz.light.actor.GameActorType.COW;

/**
 * A cow, standing in its place. Holding it shows its description. Its colour is its sprite (COW_BROWN, COW_PINK…),
 * picked in the map.
 */
public class Cow extends SpriteActor {
    public Cow(GameContext context, Sprite sprite) {
        super(COW, context, sprite, Action.IDLE, "A " + colour(sprite) + " cow. It looks calm and a bit hungry.");
        setTouchable(Touchable.enabled);
        playAnimation(Action.IDLE);
    }

    /** COW_PINK -> "pink". */
    private static String colour(Sprite sprite) {
        return sprite.name().substring(sprite.name().indexOf('_') + 1).toLowerCase();
    }
}
