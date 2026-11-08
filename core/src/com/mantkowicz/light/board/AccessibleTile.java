package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public abstract class AccessibleTile extends Tile {

    public AccessibleTile(ResourcesService resourcesService, String backgroundTextureName) {
        super(resourcesService, backgroundTextureName);
    }

    /**
     * Without a background picture: the map draws it (see {@link Tile#Tile()}).
     */
    protected AccessibleTile() {
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
