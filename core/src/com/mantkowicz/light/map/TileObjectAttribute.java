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
    CROP_TYPE("cropType");

    private final String value;

    TileObjectAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
