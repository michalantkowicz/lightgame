package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.math.RandomXS128;
import com.mantkowicz.light.character.FieldHarvestedEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.util.Dice;

import java.util.ArrayList;
import java.util.List;

/**
 * How the HUD's face reacts to what happens in the game. To the field: picking a crop makes it happy
 * ({@link #HAPPY_TIMES} times, once is easy to miss); pulling out a weed makes it sweat and say it is no use
 * ({@link #WEED}) for {@link #WEED_SECONDS}. To a tap on the face or the stamina bar: one of {@link #ON_TAP}, picked
 * at random (never the same twice in a row), so a tap there doesn't go unanswered; it doesn't cut short another
 * reaction (the weed's sweat).
 * <p>
 * Kept plain on purpose: once the face reacts to more things (the cow, the backpack, ...), rework this (e.g. what
 * reacts to what in one place, which reaction wins when two come at once) instead of adding more branches here.
 */
class FaceReactions {
    /** What the player says after pulling a weed out of a field, and for how long (the face sweats meanwhile). */
    static final String WEED = "Beautiful weed but unusable!";
    static final float WEED_SECONDS = 3;
    /** How many times the face plays its happy animation after a crop is picked. */
    static final int HAPPY_TIMES = 3;
    /** What the face may play, once, when it (or the stamina bar under it) is tapped. */
    static final List<UiImage> ON_TAP = List.of(UiImage.TEEMO_HAPPY, UiImage.TEEMO_CHEER, UiImage.TEEMO_EAR,
            UiImage.TEEMO_BLINK);

    /** Says something in a bubble over the face for a while. */
    interface Speaker {
        void say(String text, float seconds);
    }

    private GameEventService events;
    private final ResourcesService resources;
    private final Portrait face;
    private final Speaker speaker;
    /** Its own dice: a tap doesn't change what the animals do next. */
    private final RandomXS128 dice = Dice.copyOfTheGames();
    /** What the last tap played (a new tap may replace it), or null. */
    private UiImage tapped;

    FaceReactions(GameEventService events, ResourcesService resources, Portrait face, Speaker speaker) {
        this.events = events;
        this.resources = resources;
        this.face = face;
        this.speaker = speaker;
    }

    /** From now on reacts to what happens on this map (the player went through a door). */
    void listenTo(GameEventService events) {
        this.events = events;
    }

    /** Reacts to what happened since the last frame. */
    void react() {
        for (FieldHarvestedEvent harvested = events.poll(
                FieldHarvestedEvent.class); harvested != null; harvested = events.poll(FieldHarvestedEvent.class)) {
            if (harvested.isWeed()) {
                speaker.say(WEED, WEED_SECONDS);
                face.play(UiImage.TEEMO_SWEAT, WEED_SECONDS);
            } else {
                face.play(UiImage.TEEMO_HAPPY,
                        HAPPY_TIMES * resources.getUiAnimation(UiImage.TEEMO_HAPPY).getAnimationDuration());
            }
        }
    }

    /** The face (or the stamina bar) was tapped: it plays one of {@link #ON_TAP} once, unless it is busy reacting. */
    void tapped() {
        UiImage playing = face.getReaction();
        if (playing != null && playing != tapped) {
            return; // e.g. sweating over a weed: that goes on
        }
        List<UiImage> choices = new ArrayList<>(ON_TAP);
        choices.remove(tapped); // not the same twice in a row
        tapped = choices.get(dice.nextInt(choices.size()));
        face.play(tapped);
    }
}
