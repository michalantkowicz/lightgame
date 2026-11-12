package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.Thumbnail;

public interface Item extends Thumbnail {
    /** How it looks lying on the ground and in the HUD's backpack. */
    TextureRegion getPicture();

    void use();

    void unuse();
}
