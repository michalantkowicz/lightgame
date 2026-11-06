package com.mantkowicz.light.light;

public enum BlinkingIntensity {
    LOW(2), MEDIUM(5), HIGH(9);

    private final int value;

    BlinkingIntensity(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
