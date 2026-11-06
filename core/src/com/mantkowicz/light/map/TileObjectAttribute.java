package com.mantkowicz.light.map;

public enum TileObjectAttribute {
    OBJECT_CLASS("objectClass"),
    TEXTURE_NAME("textureName"),
    TEXTURE_OFFSET("textureOffset"),
    LIGHT_TYPE("lightType"),
    LIGHT_COLOR_HEX("lightColorHex"),
    BORDER_LIGHT_POSITION("borderLightPosition");

    private final String value;

    TileObjectAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
