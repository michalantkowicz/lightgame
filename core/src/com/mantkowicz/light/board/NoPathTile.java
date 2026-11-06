package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public abstract class NoPathTile extends Tile {

    public NoPathTile(ResourcesService resourcesService, String backgroundTextureName) {
        super(resourcesService, backgroundTextureName);
    }

    @Override
    public boolean isAccessible() {
        return false;
    }
}
