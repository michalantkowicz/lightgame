package com.mantkowicz.light.resources;

/**
 * The UI pieces the game uses, from the Sprout Lands UI pack. tools/assets/build_assets.py packs only these into
 * ui-pack/ui.atlas, so to use another piece add a line here and run the tool. Names are the ones in tools/assets/ui.json, all
 * shown in build/assets-preview/index.html#ui: a sheet ("dialog_box"), a piece of a sheet ("icons/017") or an animation
 * ("continue_indicator").
 */
public enum UiImage {
    DIALOG_BOX("dialog_box"),
    CONTINUE_INDICATOR("continue_indicator");

    private final String piece;

    UiImage(String piece) {
        this.piece = piece;
    }

    /**
     * The piece's name in ui-pack/ui.json, e.g. "dialog_box".
     */
    public String getPiece() {
        return piece;
    }
}
