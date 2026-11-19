package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules of the fishing game, without pictures ({@link RhythmGame} draws it): fish come down lanes and reach a line
 * at the bottom, each on a beat ({@link RhythmRules#beat()}, one beat or a few apart, in a random order of lanes). A
 * tap on a lane's net while a fish of that lane is at the line catches it; a fish that passes the line uncaught is
 * missed. A tap too early, while a fish of that lane is coming but not at the line yet, scares it off: it is missed
 * too (the owner's choice: so tapping all the time doesn't win). A tap with no fish of that lane in sight does nothing.
 * Enough misses
 * ({@link RhythmRules#toLose()}) lose at once; otherwise the game is won when the last fish is caught or missed.
 * <p>
 * Time starts when the game opens; the first beat comes then, so the first fish are already falling.
 */
public class RhythmRound {
    /** One fish coming down a lane. */
    static final class Fish {
        private final int lane;
        private final float arrival;
        private State state = State.COMING;

        Fish(int lane, float arrival) {
            this.lane = lane;
            this.arrival = arrival;
        }

        int getLane() {
            return lane;
        }

        /** When it reaches the line (seconds since the game opened). */
        float getArrival() {
            return arrival;
        }

        State getState() {
            return state;
        }
    }

    /** What has become of a fish. */
    enum State {
        COMING, CAUGHT, MISSED
    }

    private final RhythmRules rules;
    private final List<Fish> fish = new ArrayList<>();
    private float time;
    private int caught, missed;

    public RhythmRound(RhythmRules rules) {
        this.rules = rules;
        List<Integer> beats = new ArrayList<>();
        int slots = Math.round(rules.seconds() / rules.beat());
        for (int beat = 1; beat <= slots; beat++) {
            beats.add(beat);
        }
        for (int i = beats.size() - 1; i > 0; i--) { // shuffled with the game's dice
            Collections.swap(beats, i, MathUtils.random(i));
        }
        List<Integer> chosen = new ArrayList<>(beats.subList(0, Math.min(rules.fishes(), slots)));
        Collections.sort(chosen);
        for (int beat : chosen) {
            fish.add(new Fish(nextLane(), rules.fallSeconds() + beat * rules.beat()));
        }
    }

    /** A random lane, but never three fish in a row down the same one. */
    private int nextLane() {
        int size = fish.size();
        int lane = MathUtils.random(rules.lanes() - 1);
        if (size >= 2 && fish.get(size - 1).lane == lane && fish.get(size - 2).lane == lane) {
            lane = (lane + 1 + MathUtils.random(rules.lanes() - 2)) % rules.lanes();
        }
        return lane;
    }

    /** Time passes: the fish fall. Returns the fish that have just passed the line uncaught (missed). */
    List<Fish> update(float delta) {
        List<Fish> gone = new ArrayList<>();
        if (isOver()) {
            return gone;
        }
        time += delta;
        for (Fish f : fish) {
            if (f.state == State.COMING && time > f.arrival + rules.window() && missed < rules.toLose()) {
                f.state = State.MISSED;
                missed++;
                gone.add(f);
            }
        }
        return gone;
    }

    /**
     * The player taps a lane's net: catches the fish of that lane at the line, or (none there) scares off the next one
     * coming down that lane, which is missed; returned, look at its state. With no fish of that lane in sight, or once
     * the game is over, nothing happens (null).
     */
    Fish tap(int lane) {
        if (isOver()) {
            return null;
        }
        Fish atTheLine = atTheLine(lane);
        if (atTheLine != null) {
            atTheLine.state = State.CAUGHT;
            caught++;
            return atTheLine;
        }
        Fish coming = nextInSight(lane);
        if (coming != null) {
            coming.state = State.MISSED; // too early: scared off
            missed++;
        }
        return coming;
    }

    /** The coming fish of this lane close enough to the line to be caught now, or null. */
    private Fish atTheLine(int lane) {
        for (Fish f : fish) {
            if (f.lane == lane && f.state == State.COMING && Math.abs(f.arrival - time) <= rules.window()) {
                return f;
            }
        }
        return null;
    }

    /** The first coming fish of this lane already in sight (fallen in from the top), or null. */
    private Fish nextInSight(int lane) {
        for (Fish f : fish) {
            if (f.lane == lane && f.state == State.COMING && progress(f) >= 0) {
                return f;
            }
        }
        return null;
    }

    /** Is a fish of this lane coming in sight but not at the line yet (a tap now scares it off)? */
    public boolean isTooEarly(int lane) {
        return !isOver() && atTheLine(lane) == null && nextInSight(lane) != null;
    }

    /** Would a tap on this lane's net catch a fish now (one at the line)? */
    public boolean isCatchable(int lane) {
        return !isOver() && atTheLine(lane) != null;
    }

    /** How far down its lane a fish is: 0 at the top, 1 at the line (more past it; less than 0: not in yet). */
    float progress(Fish f) {
        return 1 - (f.arrival - time) / rules.fallSeconds();
    }

    List<Fish> getFish() {
        return Collections.unmodifiableList(fish);
    }

    /** How many fish come this game. */
    public int getFishCount() {
        return fish.size();
    }

    public RhythmRules getRules() {
        return rules;
    }

    public int getCaught() {
        return caught;
    }

    public int getMissed() {
        return missed;
    }

    /** Seconds since the game opened. */
    public float getTime() {
        return time;
    }

    /** Lost (enough misses), or every fish caught or missed. */
    public boolean isOver() {
        return missed >= rules.toLose() || caught + missed >= fish.size();
    }

    public boolean isWon() {
        return isOver() && missed < rules.toLose();
    }
}
