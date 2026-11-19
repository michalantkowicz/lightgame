package com.mantkowicz.light.light;

import com.badlogic.gdx.math.Interpolation;

/**
 * The time of day, one for the whole game (every map has the same): day, dusk, night, dawn, again and again, as long as
 * {@link DayNightSettings} says, from the beginning of the day. The night starts when the sun starts to set (dusk) and
 * the day when it starts to rise (dawn): each map's {@code DayNightFeature} follows it.
 */
public class DayClock {
    /** The parts of a day, in their order. */
    public enum Part {
        DAY, DUSK, NIGHT, DAWN;

        /** From dusk until dawn: the animals sleep. */
        public boolean isNight() {
            return this == DUSK || this == NIGHT;
        }
    }

    private final DayNightSettings settings;
    /** Seconds since the beginning of this day. */
    private float time;

    public DayClock(DayNightSettings settings) {
        this.settings = settings;
    }

    /** Time passes (once a frame, for the whole game). */
    public void update(float delta) {
        time = (time + delta) % settings.cycleSeconds();
    }

    public Part getPart() {
        float dusk = settings.daySeconds(), night = dusk + settings.duskSeconds();
        float dawn = night + settings.nightSeconds();
        if (time < dusk) {
            return Part.DAY;
        }
        if (time < night) {
            return Part.DUSK;
        }
        return time < dawn ? Part.NIGHT : Part.DAWN;
    }

    public boolean isNight() {
        return getPart().isNight();
    }

    /**
     * How bright it is, a part of the map's daylight: 1 by day, {@link DayNightSettings#nightLight()} by night, smoothly
     * between them at dusk and dawn.
     */
    public float getBrightness() {
        float dusk = settings.daySeconds(), night = dusk + settings.duskSeconds();
        float dawn = night + settings.nightSeconds();
        Part part = getPart();
        if (part == Part.DAY) {
            return 1;
        }
        if (part == Part.DUSK) {
            return Interpolation.smooth.apply(1, settings.nightLight(), (time - dusk) / settings.duskSeconds());
        }
        if (part == Part.NIGHT) {
            return settings.nightLight();
        }
        return Interpolation.smooth.apply(settings.nightLight(), 1, (time - dawn) / settings.dawnSeconds());
    }
}
