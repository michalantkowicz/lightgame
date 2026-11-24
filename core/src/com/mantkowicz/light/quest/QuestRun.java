package com.mantkowicz.light.quest;

import java.util.ArrayList;
import java.util.List;

/** A quest going on: where it is in its steps, what it waits for now, how much time is left. */
public class QuestRun {
    private final Quest quest;
    private final CreatureQuests owner;
    private final List<QuestStep> steps;
    private final float timeLimit;
    private float timeLeft;
    private int next;
    private Thing waitingFor;
    /** What it gave since asked last ({@link #takeRewards}). */
    private final List<Thing> rewards = new ArrayList<>();

    QuestRun(Quest quest, CreatureQuests owner) {
        this.quest = quest;
        this.owner = owner;
        this.steps = quest.getSteps();
        this.timeLimit = quest.getTimeLimit();
        this.timeLeft = timeLimit;
        advance();
    }

    /** On to the next thing to wait for: dialogs passed by (not shown yet), what it gives collected. */
    private void advance() {
        waitingFor = null;
        while (next < steps.size()) {
            QuestStep step = steps.get(next++);
            if (step instanceof QuestStep.WaitFor waitFor) {
                waitingFor = waitFor.getThing();
                return;
            }
            if (step instanceof QuestStep.Give give) {
                rewards.add(give.getThing());
            }
        }
    }

    /** The player gave it what it waited for: on to the next step. */
    void delivered() {
        advance();
    }

    void tick(float delta) {
        timeLeft = Math.max(0, timeLeft - delta);
    }

    boolean isFinished() {
        return waitingFor == null;
    }

    List<Thing> takeRewards() {
        List<Thing> taken = new ArrayList<>(rewards);
        rewards.clear();
        return taken;
    }

    public Quest getQuest() {
        return quest;
    }

    /** The animal whose quest it is (its quests, which go with it through doors). */
    public CreatureQuests getOwner() {
        return owner;
    }

    /** What it waits for now, or null once done. */
    public Thing getWaitingFor() {
        return waitingFor;
    }

    public float getTimeLeft() {
        return timeLeft;
    }

    /** How much of its time is left: 1 at its start, 0 when it runs out. */
    public float getTimeFraction() {
        return timeLimit <= 0 ? 0 : timeLeft / timeLimit;
    }
}
