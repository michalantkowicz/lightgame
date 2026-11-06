package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Inventory slots: a fixed number of slots, empty slots are null. */
class InventoryTest {
    @Test
    void emptyInventoryHasNullSlotsUpToCapacity() {
        Inventory inventory = new Inventory(3, new ArrayList<>());
        assertEquals(Arrays.asList(null, null, null), inventory.getItems());
    }

    @Test
    void itemsAreKeptAndRemainingSlotsAreEmpty() {
        Item lighter = anItem();
        Inventory inventory = new Inventory(3, new ArrayList<>(List.of(lighter)));
        assertSame(lighter, inventory.getItems().get(0));
        assertNull(inventory.getItems().get(1));
        assertEquals(3, inventory.getItems().size());
    }

    @Test
    void settingItemsRefillsEmptySlots() {
        Item lighter = anItem();
        Inventory inventory = new Inventory(2, new ArrayList<>());
        inventory.setItemsAndCompleteWithNulls(new ArrayList<>(List.of(lighter)));
        assertEquals(Arrays.asList(lighter, null), inventory.getItems());
    }

    @Test
    void settingMoreItemsThanCapacityFails() {
        Inventory inventory = new Inventory(1, new ArrayList<>());
        List<Item> tooMany = new ArrayList<>(List.of(anItem(), anItem()));
        assertThrows(IllegalArgumentException.class, () -> inventory.setItemsAndCompleteWithNulls(tooMany));
    }

    /** Any item will do; a proxy keeps this test independent of the Item interface's methods. */
    private static Item anItem() {
        return (Item) Proxy.newProxyInstance(Item.class.getClassLoader(), new Class<?>[]{Item.class}, (proxy, method, args) ->
                method.getName().equals("equals") ? proxy == args[0] : method.getName().equals("hashCode") ? System.identityHashCode(proxy) : null);
    }
}
