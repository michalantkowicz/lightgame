package com.mantkowicz.light.actor;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.context.GameContext;

import static com.mantkowicz.light.actor.GameActorType.DOG;

public class Dog extends GameBoardActor {
    private static final String AVATAR_NAME = "dog.png";
    private static final String DESCRIPTION = "This is the dog that I know from the very beginning and I like him very much";

    public Dog(GameContext context) {
        super(DOG, context, DESCRIPTION);
        createAvatar(context, AVATAR_NAME);

        setTouchable(Touchable.enabled);
    }
}
