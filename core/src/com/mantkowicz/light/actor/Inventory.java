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
