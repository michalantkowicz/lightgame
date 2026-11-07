package com.mantkowicz.light.map;

public enum BorderLightPosition {
    TOP(0),
    RIGHT(-90),
    BOTTOM(180),
    LEFT(90);

    private final int rotation;

    BorderLightPosition(int rotation) {
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }
}
