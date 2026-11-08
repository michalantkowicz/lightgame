package com.mantkowicz.light.resources;

/**
 * How the frames of one sprite sheet sit in the atlas (from sprites/animations.json). All frames of a sheet were cut to
 * one common box, so every animation of the sheet keeps the same anchor.
 */
public class SpriteSheet {
    private final int frameWidth;
    private final int frameHeight;
    private final int trimX;
    private final int trimY;

    public SpriteSheet(int frameWidth, int frameHeight, int trimX, int trimY) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.trimX = trimX;
        this.trimY = trimY;
    }

    /** Size of a frame in the original sheet (art pixels). */
    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    /** Where the packed (trimmed) frame starts inside the original frame, from its bottom-left corner (art pixels). */
    public int getTrimX() {
        return trimX;
    }

    public int getTrimY() {
        return trimY;
    }
}
