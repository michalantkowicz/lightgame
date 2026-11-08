package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public abstract class NoPathTile extends Tile {

    public NoPathTile(ResourcesService resourcesService, String backgroundTextureName) {
        super(resourcesService, backgroundTextureName);
    }

    /**
     * Without a background picture: the map draws it (see {@link Tile#Tile()}).
     */
    protected NoPathTile() {
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
