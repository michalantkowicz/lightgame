package com.mantkowicz.light.resources;

/**
 * Small pictures for things in the UI: the radial menu's buttons and items in the collect window.
 */
public enum ThumbnailType {
    /** The radial menu's "describe" button, and anything without a picture of its own. */
    DEFAULT(UiImage.QUESTION_BUTTON),
    /** The lighter: the pack has no lighter, the sun says "light". */
    LIGHT(UiImage.SUN);

    private final UiImage image;

    ThumbnailType(UiImage image) {
        this.image = image;
    }

    public UiImage getImage() {
        return image;
    }
}
