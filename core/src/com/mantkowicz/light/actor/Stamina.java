package com.mantkowicz.light.actor;

/**
 * How much strength a character has left, from 0 (none) to 1 (full); the HUD shows the player's as a bar. Game code
 * lowers it as time passes or when the character works hard ({@link #use}) and fills it again, e.g. after sleeping
 * ({@link #fill}).
 */
public class Stamina {
    private float value = 1;

    /** Takes some strength away (time passing, hard work), never below 0. */
    public void use(float amount) {
        set(value - amount);
    }

    /** Full again (e.g. after sleeping). */
    public void fill() {
        set(1);
    }

    /** Sets it directly, kept between 0 and 1. */
    public void set(float value) {
        this.value = Math.max(0, Math.min(1, value));
    }

    /** 0 (none left) to 1 (full). */
    public float get() {
        return value;
    }

    public boolean isEmpty() {
        return value <= 0;
    }
}
