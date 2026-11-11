package com.mantkowicz.light.resources;

/**
 * The fonts the game uses, from the Sprout Lands UI pack. tools/assets/build_assets.py turns only these into bitmap
 * fonts (ui-pack/fonts), so to use another one add a line here and run the tool. Names are the ones in tools/assets/ui.json.
 */
public enum UiFont {
    PIXEL_8X14("pixel_8x14");

    private final String font;

    UiFont(String font) {
        this.font = font;
    }

    /**
     * The font's name in ui-pack/ui.json, e.g. "pixel_8x14".
     */
    public String getFont() {
        return font;
    }
}
