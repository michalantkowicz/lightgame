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
    /** A big gold star: on a chest's button, there is treasure inside. */
    TREASURE("stars/treasure"),
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
    /** Teemo happy, arms up and eyes closed (a crop picked from a field). */
    TEEMO_HAPPY("emotes_premium/row_16"),
    /** Teemo sweating, a drop at its head (a weed pulled from a field: all that work for nothing). */
    TEEMO_SWEAT("emotes_premium/row_10"),
    /** Teemo tired: a yawn; sleeping: falling asleep, then two rows of sleep with a bubble growing and shrinking. */
    TEEMO_YAWN("emotes_premium/row_29"),
    TEEMO_FALL_ASLEEP("emotes_premium/row_41"),
    TEEMO_SLEEP("emotes_premium/row_42"),
    TEEMO_SLEEP_MORE("emotes_premium/row_43"),
    /** The hint button beside the face: up, and pressed (lower, its side hidden). */
    ROUND_BUTTON("round_buttons/brown"),
    ROUND_BUTTON_DOWN("round_buttons/brown_down"),
    /** The action buttons over things on the map: up, and pressed. */
    ACTION_BUTTON_DARK_BROWN("round_buttons/dark_brown"),
    ACTION_BUTTON_DARK_BROWN_DOWN("round_buttons/dark_brown_down"),
    /** On the round button when there is nothing to do: tapping it gives a hint. Also on an empty field's sign. */
    HINT_ICON("icons/question"),
    /** On the tool button while the player stands on the door it came out of: tapped, it goes back through. */
    DOOR_ICON("icons/door"),
    /**
     * Between the two items on the wide tool button while the player stands on an item with a full backpack: they swap.
     * Drawn for the game (tools/assets/swap_icon.py): the pack has single arrows only.
     */
    SWAP_ICON("icons_swap"),
    /**
     * Over a sleeping animal now and then: a cloud with what it dreams about, little circles trailing down to its
     * head. Drawn for the game (tools/assets/dream_bubble.py): the pack's bubbles are for talking.
     */
    DREAM_BUBBLE("dream_bubble"),
    /** The animals' faces on the quest panel (who asks): the emoji sheet's brown cow, chicken and chick. */
    COW_FACE("emojis/285"),
    /** The quest panel's cancel button (an X) and the arrow between who asks and what it asks for (turned right). */
    CLOSE_ICON("icons/close"),
    /** The quest's cancel button's X: the pack's X in red (drawn by tools/assets/quest_icons.py). */
    RED_CLOSE_ICON("close_red"),
    /** Who asks -> what for, on the quest row (drawn by tools/assets/quest_icons.py). */
    QUEST_ARROW("quest_arrow"),
    /** The quest row's background: the pack's brown rectangle button, a nine-patch. */
    QUEST_BUTTON("square_buttons_26x19/brown"),
    /** The small beige round button with the quest's X. */
    SMALL_BEIGE_BUTTON("round_buttons_small/beige"),
    ARROW_LEFT_ICON("icons/arrow_left"),
    CHICKEN_FACE("emojis/250"),
    CHICK_FACE("emojis/252"),
    /**
     * The day clock at the top left (the weather UI's frame without its label): a picture for the part of the day in
     * its left window, the day's scale in its right one with an arrow over it.
     */
    DAY_CLOCK_FRAME("day_clock_frame/0"),
    /** The day's scale, from the sun (top) to the moon (bottom): it fills the clock's right window. */
    DAY_CLOCK_SCALE("day_clock_scale/0"),
    /**
     * The clock's arrows, clockwise from nearly straight up (80°, at the sun) to nearly straight down (at the moon). The
     * pack has five, from about 50° up to 50° down; the steepest two are drawn for the game
     * (tools/assets/day_clock_arrows.py).
     */
    DAY_CLOCK_ARROW_UP("day_clock_arrow_up"),
    DAY_CLOCK_ARROW_STEEP_UP("day_clock_arrows/0"),
    DAY_CLOCK_ARROW_UP_RIGHT("day_clock_arrows/1"),
    DAY_CLOCK_ARROW_RIGHT("day_clock_arrows/2"),
    DAY_CLOCK_ARROW_DOWN_RIGHT("day_clock_arrows/3"),
    DAY_CLOCK_ARROW_STEEP_DOWN("day_clock_arrows/4"),
    DAY_CLOCK_ARROW_DOWN("day_clock_arrow_down"),
    /** The parts of the day on the clock: the sun, the setting sun, the moon with a star, the sun behind a cloud. */
    TIME_DAY("weather_icons/01"),
    TIME_DUSK("weather_icons/04"),
    TIME_NIGHT("weather_icons/09"),
    TIME_DAWN("weather_icons/03"),
    /** The dialog box turned so its pointer is at the middle of its bottom (cut in parts by {@code SpeechBubble}). */
    DIALOG_BOX_DOWN("dialog_box_down"),
    /** A small bubble popping up over a character on the map, with a picture put inside it (what a cow wishes for). */
    PICTURE_BUBBLE("speech_bubble_grey/row_01"),
    /** Floats up over the player after stroking an animal. */
    HEART("emojis/heart"),
    /** Over a farm animal sleeping the night. */
    ZZZ("emojis/zzz"),
    /** On the round button next to an animal that can be stroked: a paw (the pack has no hand). */
    HAND_ICON("mouse_catpaw_mouse_icon"),
    /** The pack's paw pointing: where to tap (the bobber a fish just took). */
    PAW_POINTING("mouse_catpaw_pointing_mouse_icon"),
    /** A pink X: a fish missed in the fishing game. */
    MISS("marks/miss"),
    /**
     * A hand pointer, the finger up (drawn by tools/assets/hand_pointer.py, the pack has none): tap now! Its finger's
     * tip is {@link #HAND_TIP_X} pack pixels from its left, on its top.
     */
    HAND_POINTING("mouse_hand_pointing_mouse_icon"),
    /** The face sad (closes its eyes, then a red mark): a fish got away. */
    TEEMO_SAD("emotes_premium/row_24"),
    /** The face puts its sunglasses on, then wears them with a sparkle: a fish caught. */
    TEEMO_COOL_ON("emotes_premium/row_19"),
    TEEMO_COOL("emotes_premium/row_22");

    /** Where {@link #HAND_POINTING}'s finger tip is: this many pack pixels from the picture's left (on its top). */
    public static final int HAND_TIP_X = 5;

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
