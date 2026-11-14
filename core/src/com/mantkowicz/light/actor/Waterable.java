package com.mantkowicz.light.actor;

/**
 * Something that grows when it is watered (a field): while it needs water, its action button waters it instead of
 * opening it. The player's can never runs dry.
 */
public interface Waterable {
    /** It needs water now (a field with a crop still growing). */
    boolean needsWater();

    /** It is watered: water falls onto it and it grows a stage. */
    void water();
}
