package com.mantkowicz.light.light;

/**
 * How day and night go ({@link DayClock}): the one place to tune them. A day is full daylight, then dusk (the light
 * dims), the night, then dawn (the light comes back), and again; the game starts at the beginning of the day.
 * <p>
 * The game uses {@link #GAME}; change the numbers there. Tests may use other settings for games started from then on
 * ({@link #use}).
 */
public final class DayNightSettings {
    /** As the game ships: 3 minutes of day, 8 s of dusk, 40 s of night, 8 s of dawn; the night a quarter as bright. */
    public static final DayNightSettings GAME = new DayNightSettings(180, 8, 40, 8, 0.25f);

    private static DayNightSettings current = GAME;

    private final float daySeconds;
    private final float duskSeconds;
    private final float nightSeconds;
    private final float dawnSeconds;
    private final float nightLight;

    /**
     * @param daySeconds   how long the full daylight lasts
     * @param duskSeconds  how long the light takes to dim to the night's (night has started: the animals go to sleep)
     * @param nightSeconds how long the full night lasts
     * @param dawnSeconds  how long the light takes to come back (day has started: the animals wake up)
     * @param nightLight   how bright the night is, a part of the map's daylight (0 black .. 1 as bright as the day)
     */
    public DayNightSettings(float daySeconds, float duskSeconds, float nightSeconds, float dawnSeconds,
            float nightLight) {
        this.daySeconds = daySeconds;
        this.duskSeconds = duskSeconds;
        this.nightSeconds = nightSeconds;
        this.dawnSeconds = dawnSeconds;
        this.nightLight = nightLight;
    }

    /** The settings for games started from now on. */
    public static DayNightSettings current() {
        return current;
    }

    /** Other settings for games started from now on (tests); {@link #GAME} brings the game's back. */
    public static void use(DayNightSettings settings) {
        current = settings;
    }

    public float daySeconds() {
        return daySeconds;
    }

    public float duskSeconds() {
        return duskSeconds;
    }

    public float nightSeconds() {
        return nightSeconds;
    }

    public float dawnSeconds() {
        return dawnSeconds;
    }

    public float nightLight() {
        return nightLight;
    }

    /** A whole day and its night. */
    public float cycleSeconds() {
        return daySeconds + duskSeconds + nightSeconds + dawnSeconds;
    }
}
