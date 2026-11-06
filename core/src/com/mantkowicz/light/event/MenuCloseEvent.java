package com.mantkowicz.light.event;

import com.badlogic.gdx.math.Vector2;

public class MenuCloseEvent extends GameEvent {
    private final Vector2 pointer;

    public MenuCloseEvent(Vector2 pointer) {
        this.pointer = pointer;
    }

    public Vector2 getPointer() {
        return pointer;
    }
}
