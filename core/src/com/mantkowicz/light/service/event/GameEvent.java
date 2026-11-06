package com.mantkowicz.light.service.event;

/**
 * An event in the {@link GameEventService} queue. Each event class declares its own payload with a named getter.
 */
public abstract class GameEvent {
    private static final int IMMORTAL = Integer.MIN_VALUE;

    private final int maxAge;
    private int age;

    /**
     * @param maxAge how many loops the event lives, must be greater or equal 0
     * @throws IllegalArgumentException if maxAge < 0
     */
    protected GameEvent(int maxAge) {
        if (maxAge < 0) {
            throw new IllegalArgumentException("The maxAge must be at least 0");
        }
        this.maxAge = maxAge;
        this.age = 0;
    }

    /**
     * creates immortal game event (will be deleted on demand)
     */
    protected GameEvent() {
        this.maxAge = IMMORTAL;
        this.age = 0;
    }

    public void increaseAge() {
        age++;
    }

    public boolean isTooOld() {
        return maxAge != IMMORTAL && age >= maxAge;
    }
}
