package com.mantkowicz.light.resources;

/**
 * The UI pieces the game uses, from the Sprout Lands UI pack. tools/assets/build_assets.py packs only these into
 * ui-pack/ui.atlas, so to use another piece add a line here and run the tool. Names are the ones in tools/assets/ui.json, all
 * shown in build/assets-preview/index.html#ui: a sheet ("dialog_box"), a piece of a sheet ("icons/017") or an animation
 * ("continue_indicator").
 */
public enum UiImage {
    DIALOG_BOX("dialog_box"),
    CONTINUE_INDICATOR("continue_indicator"),
    BUTTON("play_button/blank"),
    BUTTON_DOWN("play_button/blank_down"),
    PANEL("settings_menu/panel"),
    SLOT("inventory/slot"),
    SLOT_PRESSED("inventory/slot_pressed"),
    SLOT_CHOSEN("inventory/slot_chosen"),
    SLOT_FREE("inventory/slot_free"),
    QUESTION_BUTTON("icon_buttons/question"),
    SUN("emojis/sun"),
    // the HUD (docs/hud-design.md)
    APPLE("emojis/apple"),
    GEAR_BUTTON("icon_buttons/gear"),
    CHECK_BUTTON("icon_buttons/check"),
    /** The dots of the ring that fills while the gear is held: lit and not yet lit. */
    RING_DOT("selector_dot_1"),
    RING_DOT_EMPTY("selector_dot_6");

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
