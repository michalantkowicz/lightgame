package com.mantkowicz.light.quest;

import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.ProduceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Something an animal asks for, to give something back: e.g. a cow that gets a carrot gives milk. Write one as a class
 * (one per quest), list it in {@link QuestBook} under a name, and give an animal that name in Tiled (its
 * {@code quests} property, e.g. "giveMilk", several with commas). The animal keeps its quests for good, also through
 * doors; whether and when one starts is up to {@link #shouldStart}.
 * <pre>{@code
 * public class GiveMilk extends Quest {
 *     public GiveMilk() {
 *         super("giveMilk");
 *     }
 *
 *     public boolean shouldStart(QuestWorld world) {
 *         return !world.isWaiting() && !world.isQuestActive() && world.getPartOfDay() == DayClock.Part.DAY;
 *     }
 *
 *     protected void run() {
 *         dialog("A carrot, please!");
 *         waitFor(CropType.CARROT);
 *         dialog("Thank you! Here is some milk.");
 *         give(ProduceType.MILK);
 *     }
 * }
 * }</pre>
 * Starting it: the animal shows a bubble with the quest's picture ({@link #getIcon}, the first thing it waits for);
 * the player, next to it, taps the button with the same picture. Then it goes step by step: a dialog (not shown yet:
 * passed by), waiting until the player gives it what it waits for, giving something back (it lands next to the
 * animal). It runs out after {@link #getTimeLimit} seconds; the player can cancel it. One quest at a time goes on in
 * the whole game, and an animal offers one quest at a time.
 */
public abstract class Quest {
    /** How long the player has to do a quest once started, unless the quest says otherwise. */
    public static final float DEFAULT_TIME_LIMIT = 180;

    private final String name;
    private final List<QuestStep> steps = new ArrayList<>();

    protected Quest(String name) {
        this.name = name;
    }

    /** Its name in Tiled, e.g. "giveMilk". */
    public final String getName() {
        return name;
    }

    /**
     * May it start now (the animal shows its bubble)? Asked every second while the animal offers no quest and none of
     * its quests goes on; its first quest that says yes is offered. Say what you like: other quests done, the time of
     * day, time passed since the last quest, chance ({@code MathUtils.random}).
     */
    public abstract boolean shouldStart(QuestWorld world);

    /** How it goes, step by step: {@link #dialog}, {@link #waitFor}, {@link #give}, in order. */
    protected abstract void run();

    /** Seconds the player has once it started; then it runs out (as if cancelled). */
    public float getTimeLimit() {
        return DEFAULT_TIME_LIMIT;
    }

    /** The picture of its bubble and its button: the first thing it waits for (or gives, with nothing to wait for). */
    public Thing getIcon() {
        Thing given = null;
        for (QuestStep step : getSteps()) {
            if (step instanceof QuestStep.WaitFor waitFor) {
                return waitFor.getThing();
            }
            if (given == null && step instanceof QuestStep.Give give) {
                given = give.getThing();
            }
        }
        return given;
    }

    /** The animal says this (not shown yet: dialogs come later). */
    protected final void dialog(String text) {
        steps.add(new QuestStep.Dialog(text));
    }

    /** It waits until the player gives it a crop of this type. */
    protected final void waitFor(CropType crop) {
        waitFor(Thing.crop(crop));
    }

    /** It waits until the player gives it produce of this type. */
    protected final void waitFor(ProduceType produce) {
        waitFor(Thing.produce(produce));
    }

    protected final void waitFor(Thing thing) {
        steps.add(new QuestStep.WaitFor(thing));
    }

    /** It gives the player produce of this type (it lands next to it). */
    protected final void give(ProduceType produce) {
        give(Thing.produce(produce));
    }

    /** It gives the player a crop of this type. */
    protected final void give(CropType crop) {
        give(Thing.crop(crop));
    }

    protected final void give(Thing thing) {
        steps.add(new QuestStep.Give(thing));
    }

    /** Its steps, from {@link #run()} (run once). */
    final List<QuestStep> getSteps() {
        if (steps.isEmpty()) {
            run();
        }
        return steps;
    }

    @Override
    public String toString() {
        return name;
    }
}
