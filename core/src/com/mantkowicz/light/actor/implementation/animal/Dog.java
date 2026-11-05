package com.mantkowicz.light.actor.implementation.animal;

import com.badlogic.gdx.graphics.Texture;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.configuration.api.PlayerConfiguration;

import static com.mantkowicz.light.actor.GameActorType.DOG;

public class Dog extends GameBoardActor {
    private static final String AVATAR_RESOURCE_NAME = "dog.png";

    public Dog(PlayerConfiguration configuration) {
        super(DOG, configuration);

        Texture avatarTexture = configuration.getResourcesService().getAssetManager().get(AVATAR_RESOURCE_NAME, Texture.class);
        createAvatar(avatarTexture);
    }

    @Override
    protected String getDescription() {
        return "This is the dog that I know from the very beginning and I like him very much";
    }
}
