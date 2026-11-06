package com.mantkowicz.light.map;

public enum BorderLightPosition {
    TOP(0),
    TOP_RIGHT(-60),
    TOP_LEFT(60),
    BOTTOM(180),
    BOTTOM_RIGHT(-120),
    BOTTOM_LEFT(120);

    private final int rotation;

    BorderLightPosition(int rotation) {
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }
}
