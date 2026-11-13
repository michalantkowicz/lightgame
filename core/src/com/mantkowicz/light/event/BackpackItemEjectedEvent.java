package com.mantkowicz.light.event;

/**
 * An item jumped out of the backpack by itself (the backpack was full when the player stepped onto another item): the
 * HUD shakes the slot it came from.
 */
public class BackpackItemEjectedEvent extends GameEvent {
    private final int slot;

    public BackpackItemEjectedEvent(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
}
