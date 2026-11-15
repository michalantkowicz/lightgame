package com.mantkowicz.light.ui.hud;

import com.mantkowicz.light.character.FieldHarvestedEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.UiImage;

/**
 * How the HUD's face reacts to the field: picking a crop makes it happy (once); pulling out a weed makes it sweat and
 * say it is no use ({@link #WEED}) for {@link #WEED_SECONDS}.
 */
class HarvestReactions {
    /** What the player says after pulling a weed out of a field, and for how long (the face sweats meanwhile). */
    static final String WEED = "Beautiful weed but unusable!";
    static final float WEED_SECONDS = 3;

    /** Says something in a bubble over the face for a while. */
    interface Speaker {
        void say(String text, float seconds);
    }

    private final GameEventService events;
    private final Portrait face;
    private final Speaker speaker;

    HarvestReactions(GameEventService events, Portrait face, Speaker speaker) {
        this.events = events;
        this.face = face;
        this.speaker = speaker;
    }

    /** Reacts to what was harvested since the last frame. */
    void update() {
        for (FieldHarvestedEvent harvested = events.poll(
                FieldHarvestedEvent.class); harvested != null; harvested = events.poll(FieldHarvestedEvent.class)) {
            if (harvested.isWeed()) {
                speaker.say(WEED, WEED_SECONDS);
                face.play(UiImage.TEEMO_SWEAT, WEED_SECONDS);
            } else {
                face.play(UiImage.TEEMO_HAPPY);
            }
        }
    }
}
