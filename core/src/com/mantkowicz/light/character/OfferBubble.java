package com.mantkowicz.light.character;

import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.PictureBubble;
import com.mantkowicz.light.actor.WalkNextToOnTapListener;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.quest.Quest;
import com.mantkowicz.light.resources.ResourcesService;

/**
 * The bubble over a farm animal with the quest it offers (the quest's picture), by day, once it is on a map; tapping
 * it walks the player next to the animal, as tapping the animal does. Gone once the quest starts; hidden while the
 * button with the quest is over the animal, and while it eats what it was given and shows its hearts.
 */
final class OfferBubble {
    private final FarmAnimal animal;
    private final ResourcesService resources;
    private final GameEventService events;
    private PictureBubble bubble;
    /** False while the action button with the quest is over the animal. */
    private boolean shown = true;

    OfferBubble(FarmAnimal animal, ResourcesService resources, GameEventService events) {
        this.animal = animal;
        this.resources = resources;
        this.events = events;
    }

    /** Over the animal while it offers a quest by day, else gone; call it every frame. */
    void update(Quest offered, boolean night) {
        boolean show = offered != null && !night && !animal.isTakingAGift() && animal.getStage() != null;
        if (show && bubble == null) {
            Item picture = offered.getIcon().create(resources);
            bubble = new PictureBubble(resources, offered.getIcon().getName(), picture.getPicture());
            bubble.onTap(new WalkNextToOnTapListener(animal, events));
            bubble.showOver(animal);
        } else if (!show && bubble != null) {
            hide();
        }
        if (bubble != null) {
            bubble.setVisible(shown);
        }
    }

    void hide() {
        if (bubble != null) {
            bubble.remove();
            bubble = null;
        }
    }

    void setShown(boolean shown) {
        this.shown = shown;
        if (bubble != null) {
            bubble.setVisible(shown);
        }
    }

    /** The bubble, or null. */
    PictureBubble get() {
        return bubble;
    }
}
