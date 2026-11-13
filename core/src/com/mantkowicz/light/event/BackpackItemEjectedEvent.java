package com.mantkowicz.light.event;

/**
 * An item left the backpack without its slot being tapped (the backpack was full when the player stepped onto another
 * item, or the item was given to a cow): the HUD shakes the slot it came from.
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
