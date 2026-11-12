package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.ThumbnailType;

/**
 * A carrot: it lies on the ground and goes into the backpack. Nothing more yet.
 */
public class Carrot implements Item {
    /** Its row of the farm items sheet (one item per row). */
    private static final String PICTURE = "farm_items/row_02";

    private final TextureRegion picture;

    public Carrot(ResourcesService resources) {
        picture = resources.getTexture(PICTURE);
    }

    @Override
    public TextureRegion getPicture() {
        return picture;
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
}
