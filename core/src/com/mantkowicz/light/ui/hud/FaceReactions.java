package com.mantkowicz.light.ui.hud;

import com.mantkowicz.light.character.FieldHarvestedEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * How the HUD's face reacts to what happens in the game. For now only to the field: picking a crop makes it happy
 * ({@link #HAPPY_TIMES} times, once is easy to miss); pulling out a weed makes it sweat and say it is no use
 * ({@link #WEED}) for {@link #WEED_SECONDS}.
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

    /** Says something in a bubble over the face for a while. */
    interface Speaker {
        void say(String text, float seconds);
    }

    private final GameEventService events;
    private final ResourcesService resources;
    private final Portrait face;
    private final Speaker speaker;

    FaceReactions(GameEventService events, ResourcesService resources, Portrait face, Speaker speaker) {
        this.events = events;
        this.resources = resources;
        this.face = face;
        this.speaker = speaker;
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
}
