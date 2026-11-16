package com.mantkowicz.light.map;

public enum TileObjectAttribute {
    OBJECT_CLASS("objectClass"),
    TEXTURE_NAME("textureName"),
    TEXTURE_OFFSET("textureOffset"),
    LIGHT_TYPE("lightType"),
    LIGHT_COLOR_HEX("lightColorHex"),
    BORDER_LIGHT_POSITION("borderLightPosition"),
    /** A cow's crops to wish for, e.g. "CARROT,CUCUMBER"; any crop without it. */
    FAVOURITE_DISH("favouriteDish"),
    /** What grows on a field, e.g. "CORN"; a carrot without it. */
    CROP_TYPE("cropType"),
    /** How likely a field grows a weed instead of its crop after watering, 0..1, e.g. "0" for never; 0.4 without it. */
    WEED_CHANCE("weedChance"),
    /** How far grass (objectClass GRASS) has grown: 1 a tuft, 2 two tufts, 3 a bush, 4 a big bush; 1 without it. */
    PHASE("phase");

    private final String value;

    TileObjectAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
