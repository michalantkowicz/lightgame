package com.mantkowicz.light.character;

/**
 * A farm animal that sleeps in a shelter (a chicken in the coop, the cow in the barn), going there at night through
 * the door Tiled marks with {@code bedtime} for its kind ({@link BedtimeTrip}).
 */
public interface Sheltered {
    /** Where the animals of its kind sleep on the map it is on ({@code BedtimeFeature} tells it every frame). */
    void setRoost(Roost roost);

    /** Did it come into its shelter for the night (it goes back out in the morning)? For tests. */
    boolean sleepsAway();
}
