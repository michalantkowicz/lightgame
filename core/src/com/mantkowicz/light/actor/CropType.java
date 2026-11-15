package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.resources.Sprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The kinds of crop (the sprites pack's farm items sheet, one item per row): what a chest holds, what a cow wishes for,
 * what grows on a field (its plant, one frame per phase, and the sign showing it).
 */
public enum CropType {
    CORN("corn", "farm_items/row_01", Sprite.PLANT_CORN, Sprite.SIGN_CORN),
    CARROT("carrot", "farm_items/row_02", Sprite.PLANT_CARROT, Sprite.SIGN_CARROT),
    TOMATO("tomato", "farm_items/row_04", Sprite.PLANT_TOMATO, Sprite.SIGN_TOMATO),
    PUMPKIN("pumpkin", "farm_items/row_09", Sprite.PLANT_PUMPKIN, Sprite.SIGN_PUMPKIN),
    CUCUMBER("cucumber", "farm_items/row_14", Sprite.PLANT_CUCUMBER, Sprite.SIGN_CUCUMBER);

    private final String name;
    private final String picture;
    private final Sprite plant;
    private final Sprite sign;

    CropType(String name, String picture, Sprite plant, Sprite sign) {
        this.name = name;
        this.picture = picture;
        this.plant = plant;
        this.sign = sign;
    }

    /** How it grows on a field: its GROW row has a frame per phase, from its smallest plant to the ripe crop (a field shows the shared sprout, {@link Sprite#PLANT_SEEDLING}, before them). */
    public Sprite getPlant() {
        return plant;
    }

    /** The sign at a field's corner showing it (its SIGN row). */
    public Sprite getSign() {
        return sign;
    }

    /** What it is called, e.g. "carrot" (for tests and descriptions). */
    public String getName() {
        return name;
    }

    /** Its picture in the atlas. */
    public String getPicture() {
        return picture;
    }

    /** One of them, at random. */
    public static CropType random() {
        return randomOf(all());
    }

    /** One of these, at random. */
    public static CropType randomOf(List<CropType> types) {
        return types.get(MathUtils.random(types.size() - 1));
    }

    /** All the kinds, e.g. a cow's favourite dishes when nothing else is set. */
    public static List<CropType> all() {
        return new ArrayList<>(Arrays.asList(values()));
    }

    /**
     * The kinds named in a comma-separated list, e.g. "CARROT,CUCUMBER" (a Tiled property); all of them when it is
     * null or blank.
     */
    public static List<CropType> listOf(String names) {
        if (names == null || names.trim().isEmpty()) {
            return all();
        }
        List<CropType> types = new ArrayList<>();
        for (String name : names.split(",")) {
            types.add(named(name.trim()));
        }
        return types;
    }

    /** The kind with this name, e.g. "CARROT" (a Tiled property); {@code fallback} when it is null or blank. */
    public static CropType named(String name, CropType fallback) {
        return name == null || name.trim().isEmpty() ? fallback : named(name.trim());
    }

    private static CropType named(String name) {
        for (CropType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "No crop called '" + name + "', there are: " + Arrays.toString(values()));
    }
}
