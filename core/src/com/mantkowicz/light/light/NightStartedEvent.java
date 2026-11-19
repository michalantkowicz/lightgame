package com.mantkowicz.light.light;

import com.mantkowicz.light.event.GameEvent;

/** The sun starts to set ({@link DayClock}): posted on every map, lives one frame. */
public class NightStartedEvent extends GameEvent {
    public NightStartedEvent() {
        super(1);
    }
}
