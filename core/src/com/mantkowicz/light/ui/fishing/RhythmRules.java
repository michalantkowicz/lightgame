package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.math.MathUtils;

/**
 * Everything that makes the fishing game ({@link RhythmRound}) easier or harder, in one place. (A plain class, not a
 * record: GWT's compiler fails on this one as a record.)
 */
public final class RhythmRules {
    /** The levels made for 3-, 5- and 8-year-olds. */
    public static final float AGE_3 = 0.15f, AGE_5 = 0.45f, AGE_8 = 0.8f;
    /** The same at every level: an 8 s run of 12 fish in 3 lanes on half-second beats, three misses lose. */
    public static final float SECONDS = 8, BEAT = 0.5f;
    public static final int FISHES = 12, LANES = 3, TO_LOSE = 3;

    private final float seconds;
    private final float beat;
    private final int fishes;
    private final int lanes;
    private final int toLose;
    private final float fallSeconds;
    private final float window;

    /**
     * @param seconds       how long the fish come: the last one reaches the line this long after the first beat
     * @param beat          the fish reach the line on beats this far apart (one beat or a few between two fish)
     * @param fishes        how many fish come (at most one a beat)
     * @param lanes         how many lanes, each with its net button
     * @param toLose        this many fish missed lose the game
     * @param fallSeconds   how long a fish takes from the top to the line (shorter: faster)
     * @param window        a tap this close to a fish reaching the line (before or after) catches it
     */
    public RhythmRules(float seconds, float beat, int fishes, int lanes, int toLose, float fallSeconds, float window) {
        this.seconds = seconds;
        this.beat = beat;
        this.fishes = fishes;
        this.lanes = lanes;
        this.toLose = toLose;
        this.fallSeconds = fallSeconds;
        this.window = window;
    }

    /** The rules for a level, 0 (easiest) .. 1 (hardest): the fish fall faster, the window is shorter. */
    public static RhythmRules forLevel(float level) {
        float l = MathUtils.clamp(level, 0, 1);
        return new RhythmRules(SECONDS, BEAT, FISHES, LANES, TO_LOSE, byAge(l, 2.2f, 1.6f, 1.2f),
                byAge(l, 0.4f, 0.28f, 0.14f));
    }

    /** A value at the age levels ({@link #AGE_3}, ...), straight lines between them, flat beyond. */
    static float byAge(float level, float age3, float age5, float age8) {
        if (level <= AGE_3) {
            return age3;
        }
        if (level <= AGE_5) {
            return MathUtils.lerp(age3, age5, (level - AGE_3) / (AGE_5 - AGE_3));
        }
        if (level <= AGE_8) {
            return MathUtils.lerp(age5, age8, (level - AGE_5) / (AGE_8 - AGE_5));
        }
        return age8;
    }

    public float seconds() {
        return seconds;
    }

    public float beat() {
        return beat;
    }

    public int fishes() {
        return fishes;
    }

    public int lanes() {
        return lanes;
    }

    public int toLose() {
        return toLose;
    }

    public float fallSeconds() {
        return fallSeconds;
    }

    public float window() {
        return window;
    }
}
