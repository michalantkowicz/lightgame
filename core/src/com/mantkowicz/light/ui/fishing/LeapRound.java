package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules of the fishing game "catch the leap", without pictures ({@link LeapGame} draws it). The hooked fish leaps
 * out of the water again and again; some leaps reach the green zone at the top, some stop below it. A tap while the
 * fish is in the green zone catches it once; a tap at any other moment, or a leap into the green zone with no tap, is
 * a miss (between leaps a tap does nothing). Three catches win, three misses lose. At most {@link #LEAPS} leaps, at least {@link #GREEN_LEAPS} of them
 * into the green zone, so it always ends (doing nothing loses). One leap gives one result at most.
 * <p>
 * How hard it is comes from one number, the level, 0 (easiest) .. 1 (hardest): a slower or faster leap, a taller or
 * shorter green zone, leaps that stop far below the green zone or just under it, more or fewer quick low leaps,
 * longer or shorter pauses. The leap's
 * speed and the green zone are tuned for 3-, 5- and 8-year-olds ({@link #AGE_3}, {@link #AGE_5}, {@link #AGE_8}).
 */
public class LeapRound {
    /** Three catches win, three misses lose. */
    public static final int TO_WIN = 3, TO_LOSE = 3;
    public static final int LEAPS = 15, GREEN_LEAPS = 6;
    /** A quick low leap takes this part of a leap's time. */
    static final float FAST = 0.55f;

    /** The result of a tap or of a leap. */
    public enum Result {
        CAUGHT, MISSED, NONE
    }

    private final float leapSeconds, greenFrom, greenTop, pauseSeconds, shortMin, shortMax, fastShare;
    private final List<Boolean> reachesGreen = new ArrayList<>();
    private int leap = -1;
    /**
     * The current leap: its top (0 water .. 1 the window's top), how long it takes, time since it began; between
     * leaps: the pause.
     */
    private float top, duration, time, pause;
    /** Is the fish in the air; has its leap given its result (a catch or a miss)? */
    private boolean leaping, decided;
    private int caught, missed;

    /** @param level how hard, 0 .. 1 */
    public LeapRound(float level) {
        float l = MathUtils.clamp(level, 0, 1);
        // through three points, the levels for 3-, 5- and 8-year-olds (a straight line can't fit all three, found by
        // play-testers' simulated children); the green zone is a thin band at the top (the owner halved it)
        leapSeconds = byAge(l, 2.2f, 1.4f, 1.1f);
        greenFrom = byAge(l, 0.79f, 0.875f, 0.91f);
        // a green leap's top is at least this high: easy, the fish stays in the green zone about 1 s (a 3-year-old
        // reacts in about 0.75 s); hard, about half a second
        greenTop = Math.min(0.97f, MathUtils.lerp(0.92f, greenFrom + 0.08f, l));
        // some low leaps are quick (low ones are otherwise always slower than high ones): more of them when harder
        fastShare = byAge(l, 0.2f, 0.35f, 0.5f);
        pauseSeconds = MathUtils.lerp(1.0f, 0.5f, l);
        shortMin = MathUtils.lerp(0.3f, 0.5f, l);
        shortMax = greenFrom - MathUtils.lerp(0.14f, 0.04f, l);
        int greens = GREEN_LEAPS + MathUtils.random(LEAPS - GREEN_LEAPS - 3);
        for (int i = 0; i < LEAPS; i++) {
            reachesGreen.add(i < greens);
        }
        for (int i = LEAPS - 1; i > 0; i--) { // shuffled with the game's dice
            int j = MathUtils.random(i);
            Collections.swap(reachesGreen, i, j);
        }
        pause = 0.6f; // a moment before the first leap
    }

    /** The levels made for 3-, 5- and 8-year-olds. */
    public static final float AGE_3 = 0.15f, AGE_5 = 0.45f, AGE_8 = 0.8f;

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

    /** Time passes: the fish leaps, lands, the next one comes. A green leap that lands untouched is a miss. */
    public Result update(float delta) {
        if (isOver()) {
            return Result.NONE;
        }
        if (!leaping) {
            pause -= delta;
            if (pause <= 0) {
                if (leap + 1 >= LEAPS) {
                    missed = TO_LOSE; // out of leaps: it swims off
                    return Result.MISSED;
                }
                startNext();
            }
            return Result.NONE;
        }
        time += delta;
        if (time < duration) {
            return Result.NONE;
        }
        endLeap();
        if (!decided && reachesGreen.get(leap)) {
            decided = true;
            missed++;
            return Result.MISSED; // it was in the green zone and nobody caught it
        }
        return Result.NONE;
    }

    private void startNext() {
        leap++;
        leaping = true;
        time = 0;
        decided = false;
        boolean green = reachesGreen.get(leap);
        top = green ? MathUtils.random(greenTop, 1f) : MathUtils.random(shortMin, shortMax);
        duration = !green && MathUtils.random() < fastShare ? leapSeconds * FAST : leapSeconds;
    }

    private void endLeap() {
        leaping = false;
        pause = pauseSeconds * MathUtils.random(0.8f, 1.6f);
    }

    /**
     * The player taps: a catch while the fish is in the green zone, a miss while it is anywhere else in the air (one
     * result per leap); between leaps a tap does nothing.
     */
    public Result tap() {
        if (isOver() || !leaping || decided) {
            return Result.NONE;
        }
        decided = true;
        if (height() >= greenFrom) {
            caught++;
            endLeap(); // caught: out of the water
            return Result.CAUGHT;
        }
        missed++;
        return Result.MISSED;
    }

    /** Is a fish in the air now? */
    public boolean isLeaping() {
        return leaping;
    }

    /** How high the fish is: 0 at the water .. 1 the window's top (0 between leaps). */
    public float height() {
        return isLeaping() ? top * MathUtils.sin(MathUtils.PI * time / duration) : 0;
    }

    /** How far through its leap the fish is, 0 .. 1 (for its picture: it turns in the air). */
    public float progress() {
        return isLeaping() ? time / duration : 0;
    }

    /** Can a tap catch the fish right now (in the air, in the green zone, its leap not decided yet)? */
    public boolean isCatchable() {
        return leaping && !decided && height() >= greenFrom;
    }

    /** Where the green zone starts: 0 water .. 1 top. */
    public float getGreenFrom() {
        return greenFrom;
    }

    public int getCaught() {
        return caught;
    }

    public int getMissed() {
        return missed;
    }

    /** How many leaps have begun. */
    public int getLeaps() {
        return leap + 1;
    }

    /** Does the current leap reach the green zone? */
    public boolean reachesGreen() {
        return leaping && reachesGreen.get(leap);
    }

    public boolean isOver() {
        return caught >= TO_WIN || missed >= TO_LOSE;
    }

    public boolean isWon() {
        return caught >= TO_WIN;
    }
}
