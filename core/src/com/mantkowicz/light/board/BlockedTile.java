package com.mantkowicz.light.board;

import com.mantkowicz.light.resources.ResourcesService;

/**
 * A tile nobody walks through, but light passes over it (water, fences...). Unlike {@link WallTile} it adds no body to
 * the physics world.
 */
public class BlockedTile extends NoPathTile {
    public BlockedTile(ResourcesService resourcesService) {
        super(resourcesService, "floor-tile.png");
    }
}
