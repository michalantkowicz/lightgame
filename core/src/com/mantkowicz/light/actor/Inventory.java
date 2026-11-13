package com.mantkowicz.light.actor;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final int capacity;
    private List<Item> items;

    /**
     * @param items the starting items, or {@code null} for an empty inventory; free slots are filled with {@code null}
     */
    public Inventory(int capacity, List<Item> items) {
        this.capacity = capacity;
        this.items = (items != null) ? items : new ArrayList<>();
        completeWithNulls();
    }

    public int getCapacity() {
        return capacity;
    }

    public List<Item> getItems() {
        return items;
    }

    /** Puts the item into the first free slot; false (nothing changes) when every slot is full. */
    public boolean add(Item item) {
        int free = items.indexOf(null);
        if (free < 0) {
            return false;
        }
        items.set(free, item);
        return true;
    }

    /** No slot is free. */
    public boolean isFull() {
        return !items.contains(null);
    }

    /** Takes the item out of a slot, which becomes free; null if it was free already. */
    public Item take(int slot) {
        Item item = items.get(slot);
        items.set(slot, null);
        return item;
    }

    public Inventory setItemsAndCompleteWithNulls(List<Item> inputItems) {
        if (inputItems == null) {
            throw new IllegalArgumentException("Items must not be null");
        }
        if (inputItems.size() > capacity) {
            throw new IllegalArgumentException("Inventory capacity " + capacity + " is less than " + inputItems.size() + " items");
        }
        this.items = inputItems;
        completeWithNulls();
        return this;
    }

    private void completeWithNulls() {
        while (items.size() < capacity) {
            items.add(null);
        }
    }
}
