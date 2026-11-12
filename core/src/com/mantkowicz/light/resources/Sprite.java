package com.mantkowicz.light.resources;

/**
 * The sprite sheets the game uses. tools/assets/build_assets.py packs only these into sprites/sprites.atlas, so to use
 * another sheet from the pack add a line here and run the tool. Sheet names are the ones in tools/assets/pack.json, all
 * shown in build/assets-preview/index.html.
 */
public enum Sprite {
    RABBIT("rabbit"),
    COW_BROWN("cow_brown"),
    CHEST("chest"),
    /** Farm produce lying on the ground, one per row (only the right column: the left one has seed bags). */
    FARM_ITEMS("farm_items"),
    /** The shadow under an item lying on the ground. */
    ITEM_SHADOW("item_shadow");

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
