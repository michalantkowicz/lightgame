package com.mantkowicz.light.actor;

/**
 * Something that has an inventory: the one who collects ({@link Collecting}) and the one being collected ({@link Collectible}).
 */
public interface InventoryOwner {
    Inventory getInventory();
}
