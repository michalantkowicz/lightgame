package com.mantkowicz.light.feature;

/**
 * A feature about the player (the camera on it, what it collects, the creatures next to it, its scythe): it runs only on
 * the map the player is in. A feature about the map itself (grass growing, doors) is a plain {@link Feature}: it runs on
 * every map, also while the player is elsewhere.
 */
public interface PlayerFeature extends Feature {
    /**
     * The player went to another map, so this one stops running until the player is back: take away what it shows once
     * on the shared HUD (a tool) and forget what it was in the middle of. Nothing by default: action buttons over
     * things on this map go by themselves, and what is set every frame (the hint) the player's new map sets.
     */
    default void playerLeft() {
        // nothing to take away
    }
}
