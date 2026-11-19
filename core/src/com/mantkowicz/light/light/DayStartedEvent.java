package com.mantkowicz.light.light;

import com.mantkowicz.light.event.GameEvent;

/** The sun starts to rise ({@link DayClock}): posted on every map, lives one frame. */
public class DayStartedEvent extends GameEvent {
    public DayStartedEvent() {
        super(1);
    }
}
