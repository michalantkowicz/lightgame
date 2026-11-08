package com.mantkowicz.light.resources;

/**
 * The sprite sheets the game uses. tools/assets/build_assets.py packs only these into sprites/sprites.atlas, so to use
 * another sheet from the pack add a line here and run the tool. Sheet names are the ones in tools/assets/pack.json, all
 * shown in build/assets-preview/index.html.
 */
public enum Sprite {
    RABBIT("rabbit");

    private final String sheet;

    Sprite(String sheet) {
        this.sheet = sheet;
    }

    /**
     * The sheet's name in tools/assets/pack.json, e.g. "rabbit".
     */
    public String getSheet() {
        return sheet;
    }
}
