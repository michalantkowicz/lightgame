package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.ThumbnailType;

/**
 * A caught fish: it goes into the backpack (or lies on the ground). Nothing more yet.
 */
public class Fish implements Item {
    private final FishType type;
    private final TextureRegion picture;

    public Fish(FishType type, ResourcesService resources) {
        this.type = type;
        picture = resources.getAnimation(Sprite.FISH_CATCH, Action.CAUGHT).getKeyFrames()[type.getFrame()];
    }

    public FishType getType() {
        return type;
    }

    @Override
    public TextureRegion getPicture() {
        return picture;
    }

    /** Fish of the same colour are alike. */
    @Override
    public boolean isSameKindAs(Item other) {
        return other instanceof Fish fish && fish.type == type;
    }

    @Override
    public void use() {
    }

    @Override
    public void unuse() {
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }

    @Override
    public String toString() {
        return type.getName();
    }
}
