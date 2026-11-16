package com.mantkowicz.light.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

/** Random numbers for things that shouldn't change what the rest of the game rolls. */
public final class Dice {
    private Dice() {
    }

    /**
     * Dice that roll as the game's ({@link MathUtils#random}) would from now on, without taking any of the game's own
     * rolls: what uses them (grass, the face's answers to taps) leaves the animals' choices as they are.
     */
    public static RandomXS128 copyOfTheGames() {
        if (MathUtils.random instanceof RandomXS128 dice) {
            return new RandomXS128(dice.getState(0), dice.getState(1));
        }
        return new RandomXS128();
    }
}
