package com.mantkowicz.light.light;

public enum BlinkingSpeed {
    SLOW(0.35f), NORMAL(0.2f), FAST(0.1f);

    private final float value;

    BlinkingSpeed(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }
}
