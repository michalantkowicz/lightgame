package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;

import static com.mantkowicz.light.actor.GameActorType.COW;

/**
 * A brown cow, standing in its place. Holding it shows its description.
 */
public class Cow extends SpriteActor {
    public Cow(GameContext context) {
        super(COW, context, Sprite.COW_BROWN, Action.IDLE, "A brown cow. It looks calm and a bit hungry.");
        setTouchable(Touchable.enabled);
        playAnimation(Action.IDLE);
    }
}
