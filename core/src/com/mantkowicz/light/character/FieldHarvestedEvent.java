package com.mantkowicz.light.character;

import com.mantkowicz.light.event.GameEvent;

/**
 * The player took what was ripe on a field: its crop (the HUD's face is happy) or a weed (the face sweats and says it
 * is no use).
 */
public class FieldHarvestedEvent extends GameEvent {
    private final boolean weed;

    public FieldHarvestedEvent(boolean weed) {
        this.weed = weed;
    }

    public boolean isWeed() {
        return weed;
    }
}
