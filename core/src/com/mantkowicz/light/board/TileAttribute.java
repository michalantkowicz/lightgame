package com.mantkowicz.light.board;

public enum TileAttribute {
    TILE_CLASS("tileClass"),
    TEXTURE_NAME("textureName");

    private final String value;

    TileAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
