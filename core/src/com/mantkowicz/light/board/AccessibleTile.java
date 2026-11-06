package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

public abstract class AccessibleTile extends Tile {

    public AccessibleTile(ResourcesService resourcesService, String backgroundTextureName) {
        super(resourcesService, backgroundTextureName);
    }

    @Override
    public boolean isAccessible() {
        return true;
    }
}
