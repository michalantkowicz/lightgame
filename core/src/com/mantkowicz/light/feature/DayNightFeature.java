package com.mantkowicz.light.feature;

import box2dLight.RayHandler;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.light.GameLight;

import java.util.ArrayList;
import java.util.List;

/**
 * Day and night take turns on a map with daylight (one that sets its {@code ambientLight}; the house, lit by its
 * lamps, has no day): {@link #DAY_SECONDS} of day, then {@link #NIGHT_SECONDS} of night, again and again. In the last
 * {@link #CHANGE_SECONDS} of the day the light dims (smoothly, not in a straight line) to the night's, in the last ones
 * of the night it gets brighter again. Night isn't black: the map stays {@link #NIGHT_LIGHT} as bright as by day, so
 * the player still sees the way. The game starts in the morning (full daylight). The map's night-only lights (Tiled's
 * {@code nightOnly}) are off in full daylight and shine from when the light starts dimming until the day is back.
 * Switched off with
 * {@link com.mantkowicz.light.debug.FeatureFlag#DAY_AND_NIGHT}.
 */
public class DayNightFeature implements Feature {
    /** How long a day and a night last, each with its change at the end. */
    static final float DAY_SECONDS = 30, NIGHT_SECONDS = 30;
    /** The light changes in this many last seconds of the day (dims) and of the night (brightens). */
    static final float CHANGE_SECONDS = 5;
    /** How bright the night is: this part of the day's light. */
    static final float NIGHT_LIGHT = 0.1f;

    private final RayHandler rayHandler;
    private final float daylight;
    /** The map's lights that shine only at night. */
    private final List<GameLight> nightLights = new ArrayList<>();
    private float time;

    /**
     * Made once the map's lights are on the game stage.
     *
     * @param daylight the map's ambient light by day (0 dark .. 1 daylight), see {@code GameScreen.prepareLights}
     */
    public DayNightFeature(GameContext context, float daylight) {
        this.rayHandler = context.getRayHandler();
        this.daylight = daylight;
        for (Actor actor : context.getGameStage().getActors()) {
            if (actor instanceof GameLight light && light.isNightOnly()) {
                nightLights.add(light);
            }
        }
        run(0); // the night-only lights are off from the first frame
    }

    @Override
    public void run(float delta) {
        time = (time + delta) % (DAY_SECONDS + NIGHT_SECONDS);
        float brightness = brightness(time);
        rayHandler.setAmbientLight(0, 0, 0, daylight * brightness);
        for (GameLight light : nightLights) {
            light.setShining(brightness < 1);
        }
    }

    /** How bright it is (1 by day, {@link #NIGHT_LIGHT} by night) this many seconds into a day and its night. */
    static float brightness(float time) {
        float dusk = DAY_SECONDS - CHANGE_SECONDS, dawn = DAY_SECONDS + NIGHT_SECONDS - CHANGE_SECONDS;
        if (time < dusk) {
            return 1;
        }
        if (time < DAY_SECONDS) {
            return Interpolation.smooth.apply(1, NIGHT_LIGHT, (time - dusk) / CHANGE_SECONDS);
        }
        if (time < dawn) {
            return NIGHT_LIGHT;
        }
        return Interpolation.smooth.apply(NIGHT_LIGHT, 1, (time - dawn) / CHANGE_SECONDS);
    }
}
