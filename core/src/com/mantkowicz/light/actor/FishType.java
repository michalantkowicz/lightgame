package com.mantkowicz.light.actor;

/**
 * The fish the rabbit can catch, by colour: the first five pictures of the Ocean Pack's fish sheet.
 */
public enum FishType {
    PINK, ORANGE, YELLOW, GREEN, BLUE;

    /** Its picture's place on the sheet ({@code Sprite.FISH_CATCH}, the "caught" row). */
    public int getFrame() {
        return ordinal();
    }

    /** What it is called, e.g. "orange fish". */
    public String getName() {
        return name().toLowerCase() + " fish";
    }
}
