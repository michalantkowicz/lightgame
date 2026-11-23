package com.mantkowicz.light.quest;

import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Produce;
import com.mantkowicz.light.actor.ProduceType;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.function.Function;
import java.util.function.Predicate;

/** What a quest waits for or gives: a kind of item (a carrot, milk), not one item itself. */
public final class Thing {
    private final String name;
    private final Function<ResourcesService, Item> maker;
    private final Predicate<Item> matches;

    private Thing(String name, Function<ResourcesService, Item> maker, Predicate<Item> matches) {
        this.name = name;
        this.maker = maker;
        this.matches = matches;
    }

    /** A crop of this type, e.g. a carrot. */
    public static Thing crop(CropType type) {
        return new Thing(type.getName(), resources -> new Crop(type, resources),
                item -> item instanceof Crop crop && crop.getType() == type);
    }

    /** Produce of this type, e.g. milk. */
    public static Thing produce(ProduceType type) {
        return new Thing(type.getName(), resources -> new Produce(type, resources),
                item -> item instanceof Produce produce && produce.getType() == type);
    }

    /** A new item of this kind (what is given, or a picture of what is waited for). */
    public Item create(ResourcesService resources) {
        return maker.apply(resources);
    }

    /** Is the item of this kind (any carrot for a carrot)? */
    public boolean matches(Item item) {
        return item != null && matches.test(item);
    }

    /** What it is called, e.g. "carrot". */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
