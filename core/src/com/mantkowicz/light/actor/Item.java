package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.Thumbnail;

public interface Item extends Thumbnail {
    /** How it looks lying on the ground and in the HUD's backpack. */
    TextureRegion getPicture();

    /** Is the other one the same kind of thing (e.g. both carrots), so either will do? */
    default boolean isSameKindAs(Item other) {
        return other != null && other.getClass() == getClass();
    }

    void use();

    void unuse();
}
