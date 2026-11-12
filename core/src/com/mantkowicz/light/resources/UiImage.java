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
    RING_DOT_EMPTY("selector_dot_6"),
    /** The circle at the gear's top right that fills while the gear is held (its frames follow the hold). */
    HOLD_CIRCLE("stamina_black"),
    /** The stars that pop out of a chest when it opens. */
    SPARKLE("stars/small"),
    /** A segment of the stamina bar: what is left, and what is used. */
    BAR_FULL("bar/full"),
    BAR_EMPTY("bar/empty"),
    /** Left of the stamina bar, by how much is left: more than half, half or less, a quarter or less. */
    MOOD_HAPPY("tiny_emotes/happy"),
    MOOD_NEUTRAL("tiny_emotes/neutral"),
    MOOD_SAD("tiny_emotes/sad"),
    /** The face's frame: dark wood with posts at the corners, as in the pack's Teemo emote preview. */
    PORTRAIT_FRAME("backdrops_c1"),
    /** The player's face (Teemo from the pack's premium emotes): still, blinking, twitching an ear. */
    TEEMO_STILL("emotes_premium/row_02"),
    TEEMO_BLINK("emotes_premium/row_05"),
    TEEMO_EAR("emotes_premium/row_04"),
    /** Teemo's reactions (the face's {@code play}): waving its arms with an open mouth. */
    TEEMO_CHEER("emotes_premium/row_15"),
    /** Teemo tired: a yawn; sleeping: falling asleep, then two rows of sleep with a bubble growing and shrinking. */
    TEEMO_YAWN("emotes_premium/row_29"),
    TEEMO_FALL_ASLEEP("emotes_premium/row_41"),
    TEEMO_SLEEP("emotes_premium/row_42"),
    TEEMO_SLEEP_MORE("emotes_premium/row_43"),
    /** The action button beside the face: up, and pressed (lower, its side hidden). */
    ROUND_BUTTON("round_buttons/brown"),
    ROUND_BUTTON_DOWN("round_buttons/brown_down"),
    /** On the round button when there is nothing to do: tapping it gives a hint. */
    HINT_ICON("icons/question"),
    /** The dialog box turned so its pointer is at the middle of its bottom (cut in parts by {@code SpeechBubble}). */
    DIALOG_BOX_DOWN("dialog_box_down");

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
