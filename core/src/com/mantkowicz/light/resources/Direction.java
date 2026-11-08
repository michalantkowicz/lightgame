package com.mantkowicz.light.resources;

/**
 * The way a character faces. Rows of a sprite sheet named "action_direction" in tools/assets/pack.json (e.g.
 * "walk_left") are the action's animation for that direction.
 */
public enum Direction {
    DOWN("down"),
    UP("up"),
    LEFT("left"),
    RIGHT("right");

    private final String suffix;

    Direction(String suffix) {
        this.suffix = suffix;
    }

    /**
     * The end of the row name in pack.json, e.g. "left" in "walk_left".
     */
    public String getSuffix() {
        return suffix;
    }
}
