package com.mantkowicz.light.actor.implementation.animal;

import com.badlogic.gdx.graphics.Texture;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.game.GameContext;

import static com.mantkowicz.light.actor.GameActorType.DOG;

public class Dog extends GameBoardActor {
    private static final String AVATAR_RESOURCE_NAME = "dog.png";

    public Dog(GameContext context) {
        super(DOG, context);

        Texture avatarTexture = context.getResourcesService().getAssetManager().get(AVATAR_RESOURCE_NAME, Texture.class);
        createAvatar(avatarTexture);
    }

    @Override
    protected String getDescription() {
        return "This is the dog that I know from the very beginning and I like him very much";
    }
}
