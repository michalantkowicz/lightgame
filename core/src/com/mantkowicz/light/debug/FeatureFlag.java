package com.mantkowicz.light.debug;

/**
 * Whole game features that can be switched off (or on) in one place, e.g. to compare the game without one. A feature
 * checks its flag where it is created; switching a flag affects games started from then on.
 */
public enum FeatureFlag {
    /** Grass grows on the map and the rabbit cuts it with a scythe ({@code feature.GrassFeature}). */
    GROWING_GRASS(true),
    /** Outdoors, day and night take turns: the map's daylight dims to night and back ({@code feature.DayNightFeature}). */
    DAY_AND_NIGHT(true);

    private final boolean byDefault;
    private boolean on;

    FeatureFlag(boolean byDefault) {
        this.byDefault = byDefault;
        this.on = byDefault;
    }

    public boolean isOn() {
        return on;
    }

    public void switchOn() {
        on = true;
    }

    public void switchOff() {
        on = false;
    }

    /** Back to how the game ships (tests do this before each game). */
    public static void reset() {
        for (FeatureFlag flag : values()) {
            flag.on = flag.byDefault;
        }
    }
}
