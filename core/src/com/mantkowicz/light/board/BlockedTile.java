package com.mantkowicz.light.board;

/**
 * A tile nobody walks through, but light passes over it (water, fences...). Unlike {@link WallTile} it adds no body to
 * the physics world. It only exists on maps made with tools/assets, so it has no picture of its own: the map draws it.
 */
public class BlockedTile extends NoPathTile {
}
