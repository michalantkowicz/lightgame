package com.mantkowicz.light.actor;

import com.mantkowicz.light.resources.Sprite;

/** What an animal gives: milk (a cow), an egg (a chicken). */
public enum ProduceType {
    MILK(Sprite.MILK_ITEM, "milk"),
    EGG(Sprite.EGG_ITEM, "egg");

    private final Sprite sprite;
    private final String name;

    ProduceType(Sprite sprite, String name) {
        this.sprite = sprite;
        this.name = name;
    }

    public Sprite getSprite() {
        return sprite;
    }

    /** What it is called, e.g. "milk". */
    public String getName() {
        return name;
    }
}
