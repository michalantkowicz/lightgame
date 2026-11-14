package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The kinds of crop (the sprites pack's farm items sheet, one item per row): what a chest holds, what a cow wishes for.
 */
public enum CropType {
    CORN("corn", "farm_items/row_01"),
    CARROT("carrot", "farm_items/row_02"),
    TOMATO("tomato", "farm_items/row_04"),
    PUMPKIN("pumpkin", "farm_items/row_09"),
    CUCUMBER("cucumber", "farm_items/row_14");

    private final String name;
    private final String picture;

    CropType(String name, String picture) {
        this.name = name;
        this.picture = picture;
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
        return oneOf(Arrays.asList(values()));
    }

    /** One of these, at random. */
    public static CropType oneOf(List<CropType> types) {
        return types.get(MathUtils.random(types.size() - 1));
    }

    /**
     * The kinds named in a comma-separated list, e.g. "CARROT,CUCUMBER" (a Tiled property); all of them when it is
     * null or blank.
     */
    public static List<CropType> listOf(String names) {
        List<CropType> types = new ArrayList<>();
        if (names == null || names.trim().isEmpty()) {
            types.addAll(Arrays.asList(values()));
            return types;
        }
        for (String name : names.split(",")) {
            types.add(named(name.trim()));
        }
        return types;
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
