package com.mantkowicz.light.quest;

import com.mantkowicz.light.light.DayClock;

/** What a quest's {@link Quest#shouldStart} can ask about the game. */
public interface QuestWorld {
    /**
     * The animal offers one of its quests, or one of its quests goes on: it waits for nothing else meanwhile. (Its
     * quests are only asked while it doesn't, so this is false there; it is here for quests that want to say so.)
     */
    boolean isWaiting();

    /** A quest goes on (any animal's): there is one at a time in the whole game. */
    boolean isQuestActive();

    /** How many times this quest (by its name, any animal's) has been done in this game. */
    int timesFinished(String questName);

    /** Seconds since a quest last ended (done, cancelled or run out of time), or since the game started. */
    float secondsSinceLastQuest();

    /** The part of the day it is: day, dusk, night or dawn. */
    DayClock.Part getPartOfDay();
}
