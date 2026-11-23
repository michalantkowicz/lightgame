package com.mantkowicz.light.quest;

import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.ProduceType;
import com.mantkowicz.light.light.DayClock;

/** A cow asks for a carrot and gives a bottle of milk for it: by day, a while after the last quest. */
public class GiveMilk extends Quest {
    /** It starts at least this long after a quest last ended (or after the start). */
    static final float AFTER_SECONDS = 20;

    public GiveMilk() {
        super("giveMilk");
    }

    @Override
    public boolean shouldStart(QuestWorld world) {
        return !world.isWaiting() && !world.isQuestActive() && world.getPartOfDay() == DayClock.Part.DAY
                && world.secondsSinceLastQuest() >= AFTER_SECONDS;
    }

    @Override
    protected void run() {
        dialog("Could you bring me a carrot?");
        waitFor(CropType.CARROT);
        dialog("Thank you! Here is some milk.");
        give(ProduceType.MILK);
    }
}
