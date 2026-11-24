package com.mantkowicz.light.quest;

import com.mantkowicz.light.resources.UiImage;

import java.util.ArrayList;
import java.util.List;

/**
 * An animal's quests (Tiled's {@code quests}), for good: they go with it through doors. While it offers none and none
 * of them goes on, it asks them every {@link #CHECK_SECONDS} whether one should start ({@link Quest#shouldStart}), in turn,
 * beginning after the one it offered last (so a second quest comes after the first); the first that says yes it offers
 * (a bubble over it) until the player starts it. One at a time.
 */
public class CreatureQuests {
    /** How often it asks its quests whether one should start. */
    public static final float CHECK_SECONDS = 1;

    private final List<Quest> quests;
    /** The animal's face on the quest panel, e.g. a cow's. */
    private final UiImage icon;
    private Quest offered;
    private float sinceCheck;
    /** Where it begins asking next time: after the quest it offered last. */
    private int next;

    public CreatureQuests(List<Quest> quests, UiImage icon) {
        this.quests = new ArrayList<>(quests);
        this.icon = icon;
    }

    /** Time passes: now and then it asks its quests whether one should start (see the class). */
    public void update(float delta, QuestJournal journal) {
        if (offered != null || journal.isRunningFor(this)) {
            return;
        }
        sinceCheck += delta;
        if (sinceCheck < CHECK_SECONDS) {
            return;
        }
        sinceCheck = 0;
        QuestWorld world = journal.worldFor(this);
        for (int asked = 0; asked < quests.size(); asked++) {
            int index = (next + asked) % quests.size();
            if (quests.get(index).shouldStart(world)) {
                offered = quests.get(index);
                next = index + 1;
                return;
            }
        }
    }

    /** The quest it offers now (its bubble over it), or null. */
    public Quest getOffered() {
        return offered;
    }

    Quest takeOffered() {
        Quest quest = offered;
        offered = null;
        return quest;
    }

    public UiImage getIcon() {
        return icon;
    }

    public List<Quest> getQuests() {
        return quests;
    }
}
