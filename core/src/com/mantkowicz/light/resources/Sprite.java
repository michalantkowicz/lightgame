package com.mantkowicz.light.resources;

/**
 * The sprite sheets the game uses. tools/assets/build_assets.py packs only these into sprites/sprites.atlas, so to use
 * another sheet from the pack add a line here and run the tool. Sheet names are the ones in tools/assets/pack.json, all
 * shown in build/assets-preview/index.html.
 */
public enum Sprite {
    RABBIT("rabbit"),
    COW_BROWN("cow_brown"),
    /** A chicken and a chick; their love rows have the hearts on the row above (drawn over the bird). */
    CHICKEN("chicken"),
    CHICKEN_BABY("chicken_baby"),
    CHEST("chest"),
    /** Farm produce lying on the ground, one per row (only the right column: the left one has seed bags). */
    FARM_ITEMS("farm_items"),
    /** The shadow under an item lying on the ground. */
    ITEM_SHADOW("item_shadow"),
    /** Grass growing on the map, one frame per phase: a tuft, two tufts, a bush, a big bush. */
    GRASS_TUFT("grass_tuft"),
    /** The scythe's item icon (made by tools/assets/scythe_animation.py in the style of the pack's tool icons). */
    SCYTHE_ICON("scythe_icon"),
    /** The rabbit fishing (facing left): casting, waiting, a bite (the rod shakes), pulling the line out. */
    RABBIT_FISHING("rabbit_fishing"),
    /** The bobber in the water: dropping in, bobbing, pulled under, a splash. */
    FISHING_BOBBER("fishing_bobber"),
    FISHING_ROD_ICON("fishing_rod_icon"),
    /** The fishing net on the fishing game's lane buttons (drawn by tools/assets/net_icon.py, the pack has none). */
    FISHING_NET_ICON("fishing_net_icon"),
    /** A fish leaping out of the water (the fishing game). */
    FISH_LEAP("fish_leap"),
    /** Caught fish: the first five are pink, orange, yellow, green and blue. */
    FISH_CATCH("fish_catch"),
    /** The pack's water tile, rippling. */
    WATER_TILE("water_tile"),
    /** The watering can (the picture on a field's button while it needs water). */
    WATERING_CAN("watering_can"),
    /** Water falling from the can onto a field. */
    WATERING("watering"),
    /** A crop growing on a field, one frame per phase (the last one ripe). Corn is two tiles tall. */
    PLANT_CORN("plant_corn"),
    PLANT_CARROT("plant_carrot"),
    PLANT_TOMATO("plant_tomato"),
    PLANT_PUMPKIN("plant_pumpkin"),
    PLANT_CUCUMBER("plant_cucumber"),
    /** A field just picked, the same for every crop: a tiny sprout (it grows into the crop once watered). */
    PLANT_SEEDLING("plant_seedling"),
    /** A weed growing instead of the crop, after the sprout: the rest of the sprout's row (a blue flower). */
    PLANT_WEED("plant_weed"),
    /** The sign at a field's corner showing what grows there. */
    SIGN_CORN("sign_corn"),
    SIGN_CARROT("sign_carrot"),
    SIGN_TOMATO("sign_tomato"),
    SIGN_PUMPKIN("sign_pumpkin"),
    SIGN_CUCUMBER("sign_cucumber"),
    /** An empty sign: a field's sign while nothing grows there yet, with a question mark on it ({@link UiImage#HINT_ICON}). */
    SIGN_BLANK("sign_blank");

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
