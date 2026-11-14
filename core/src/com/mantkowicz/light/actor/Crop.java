package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.ThumbnailType;

/**
 * A crop (a carrot, corn, a tomato...): it lies on the ground and goes into the backpack. Nothing more yet.
 */
public class Crop implements Item {
    private final CropType type;
    private final TextureRegion picture;

    public Crop(CropType type, ResourcesService resources) {
        this.type = type;
        picture = resources.getTexture(type.getPicture());
    }

    public CropType getType() {
        return type;
    }

    @Override
    public TextureRegion getPicture() {
        return picture;
    }

    /** Crops of the same type are alike: any carrot will do for a cow wishing for one. */
    @Override
    public boolean isSameKindAs(Item other) {
        return other instanceof Crop crop && crop.type == type;
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

    /** Its type, e.g. "CARROT" (for tests and descriptions). */
    @Override
    public String toString() {
        return type.name();
    }
}
