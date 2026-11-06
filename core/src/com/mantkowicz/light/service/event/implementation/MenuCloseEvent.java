package com.mantkowicz.light.service.event.implementation;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.service.event.GameEvent;

public class MenuCloseEvent extends GameEvent {
    private final Vector2 pointer;

    public MenuCloseEvent(Vector2 pointer) {
        this.pointer = pointer;
    }

    public Vector2 getPointer() {
        return pointer;
    }
}
