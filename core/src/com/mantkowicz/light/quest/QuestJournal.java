package com.mantkowicz.light.quest;

import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.light.DayClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The quests of the whole game (one for all maps): the one that goes on (one at a time), its slot (a place for what it
 * waits for, besides the backpack), how many times each was done, when the last one ended. The map the player is in
 * runs it every frame ({@link #update}) and hands out what it leaves: what it gives ({@link #takeRewards}) and what was
 * in its slot when it was cancelled or ran out ({@link #takeEjected}).
 */
public class QuestJournal {
    private final DayClock clock;
    private QuestRun active;
    /** What is in the quest's slot, or null. */
    private Item slot;
    private float sinceLastEnded;
    private final Map<String, Integer> finished = new HashMap<>();
    private final List<Thing> rewards = new ArrayList<>();
    /** Whose rewards they are (they land next to it). */
    private CreatureQuests rewardsFrom;
    private final List<Item> ejected = new ArrayList<>();

    public QuestJournal(DayClock clock) {
        this.clock = clock;
    }

    /** Time passes: the quest going on runs out of time (it ends as if cancelled). */
    public void update(float delta) {
        if (active == null) {
            sinceLastEnded += delta;
            return;
        }
        active.tick(delta);
        if (active.getTimeLeft() <= 0) {
            end(false);
        }
    }

    /** Is a quest going on? */
    public boolean isActive() {
        return active != null;
    }

    /** The quest going on, or null. */
    public QuestRun getActive() {
        return active;
    }

    /** Is one of this animal's quests going on? */
    public boolean isRunningFor(CreatureQuests owner) {
        return active != null && active.getOwner() == owner;
    }

    /**
     * Starts the quest the animal offers.
     *
     * @return false (nothing starts) when another quest goes on or it offers none
     */
    public boolean start(CreatureQuests owner) {
        if (active != null || owner.getOffered() == null) {
            return false;
        }
        active = new QuestRun(owner.takeOffered(), owner);
        collectRewards();
        if (active.isFinished()) {
            end(true);
        }
        return true;
    }

    /** The player cancels the quest going on (what is in its slot jumps out). */
    public void cancel() {
        if (active != null) {
            end(false);
        }
    }

    /** What this animal's quest waits for from the player now, or null (none of its quests goes on). */
    public Thing waitingFrom(CreatureQuests owner) {
        return isRunningFor(owner) ? active.getWaitingFor() : null;
    }

    /**
     * The player gave the animal what its quest waited for: on to the next step (and done after the last). What is in
     * the slot and no longer wanted jumps out.
     */
    public void delivered() {
        active.delivered();
        collectRewards();
        if (active.isFinished()) {
            end(true);
        } else if (slot != null && !active.getWaitingFor().matches(slot)) {
            ejected.add(slot);
            slot = null;
        }
    }

    private void collectRewards() {
        List<Thing> given = active.takeRewards();
        if (!given.isEmpty()) {
            rewards.addAll(given);
            rewardsFrom = active.getOwner();
        }
    }

    private void end(boolean done) {
        if (done) {
            String name = active.getQuest().getName();
            finished.put(name, timesFinished(name) + 1);
        }
        if (slot != null) {
            ejected.add(slot);
            slot = null;
        }
        active = null;
        sinceLastEnded = 0;
    }

    int timesFinished(String name) {
        Integer times = finished.get(name);
        return times == null ? 0 : times;
    }

    // the quest's slot

    /** Should this item, just picked up, go into the quest's slot (it is what the quest waits for, the slot is empty)? */
    public boolean wantsInSlot(Item item) {
        return active != null && slot == null && active.getWaitingFor() != null
                && active.getWaitingFor().matches(item);
    }

    public void putInSlot(Item item) {
        slot = item;
    }

    /** What is in the quest's slot, or null. */
    public Item getSlot() {
        return slot;
    }

    /** Takes what is in the quest's slot out of it (null when empty). */
    public Item takeSlot() {
        Item item = slot;
        slot = null;
        return item;
    }

    // what the quest leaves

    /** What quests gave since asked last; they land next to {@link #getRewardsFrom()}. */
    public List<Thing> takeRewards() {
        List<Thing> taken = new ArrayList<>(rewards);
        rewards.clear();
        return taken;
    }

    /** Whose quest gave the rewards, or null. */
    public CreatureQuests getRewardsFrom() {
        return rewardsFrom;
    }

    /** What was in the slot when its quest ended or no longer wanted it: it jumps out next to the player. */
    public List<Item> takeEjected() {
        List<Item> taken = new ArrayList<>(ejected);
        ejected.clear();
        return taken;
    }

    /** What the animal's quests see when asked whether to start. */
    QuestWorld worldFor(CreatureQuests owner) {
        return new QuestWorld() {
            @Override
            public boolean isWaiting() {
                return owner.getOffered() != null || isRunningFor(owner);
            }

            @Override
            public boolean isQuestActive() {
                return active != null;
            }

            @Override
            public int timesFinished(String questName) {
                return QuestJournal.this.timesFinished(questName);
            }

            @Override
            public float secondsSinceLastQuest() {
                return sinceLastEnded;
            }

            @Override
            public DayClock.Part getPartOfDay() {
                return clock.getPart();
            }
        };
    }
}
