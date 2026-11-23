package com.mantkowicz.light.quest;

import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.ProduceType;
import com.mantkowicz.light.light.DayClock;

/** A chicken asks for corn and lays an egg for it: by day, a good while after the last quest. */
public class LayEgg extends Quest {
    /** It starts at least this long after a quest last ended (or after the start). */
    static final float AFTER_SECONDS = 40;

    public LayEgg() {
        super("layEgg");
    }

    @Override
    public boolean shouldStart(QuestWorld world) {
        return !world.isWaiting() && !world.isQuestActive() && world.getPartOfDay() == DayClock.Part.DAY
                && world.secondsSinceLastQuest() >= AFTER_SECONDS;
    }

    @Override
    protected void run() {
        dialog("Some corn, please!");
        waitFor(CropType.CORN);
        dialog("Thank you! An egg for you.");
        give(ProduceType.EGG);
    }
}
