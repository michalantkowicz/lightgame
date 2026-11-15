package com.mantkowicz.light.actor;

/** Something a weed can grow on (a field): the player pulls the ripe weed out, and nothing is gained. */
public interface Weedable {
    /** A ripe weed grows on it: its button pulls it out. */
    boolean hasWeed();

    /** The weed is pulled out and just disappears. */
    void pullWeed();
}
