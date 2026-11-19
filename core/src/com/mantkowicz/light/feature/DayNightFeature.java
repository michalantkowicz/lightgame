package com.mantkowicz.light.feature;

import box2dLight.RayHandler;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.light.DayClock;
import com.mantkowicz.light.light.DayStartedEvent;
import com.mantkowicz.light.light.NightStartedEvent;

/**
 * Day and night on a map, following the game's {@link DayClock}: a map with daylight (Tiled's {@code ambientLight})
 * gets the part of it the clock says (dim at night); one without (the house, lit by its lamps) stays as it is. When
 * the night starts (the sun starts to set) it posts a {@link NightStartedEvent}, when the day starts (the sun starts to
 * rise) a {@link DayStartedEvent}, on this map's events, every map for itself.
 */
public class DayNightFeature implements Feature {
    private final GameEventService events;
    private final RayHandler lights;
    private final DayClock clock;
    /** The map's daylight (0 dark .. 1), or null: no daylight. */
    private final Float daylight;
    private boolean night;

    /** @param daylight the map's ambient light by day, or null for a map without daylight */
    public DayNightFeature(GameContext context, DayClock clock, Float daylight) {
        events = context.getGameEventService();
        lights = context.getRayHandler();
        this.clock = clock;
        this.daylight = daylight;
        night = clock.isNight();
    }

    @Override
    public void run(float delta) {
        if (daylight != null) {
            lights.setAmbientLight(0, 0, 0, daylight * clock.getBrightness());
        }
        if (clock.isNight() != night) {
            night = clock.isNight();
            events.post(night ? new NightStartedEvent() : new DayStartedEvent());
        }
    }
}
