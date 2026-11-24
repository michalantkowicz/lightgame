package com.mantkowicz.light.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every quest by the name Tiled gives it (an animal's {@code quests} property, e.g. "giveMilk,layEgg"). A new quest:
 * its class, and a line in the list below.
 */
public final class QuestBook {
    private static final Map<String, Supplier<Quest>> QUESTS = new LinkedHashMap<>();

    static {
        register("giveMilk", GiveMilk::new);
        register("layEgg", LayEgg::new);
    }

    private QuestBook() {
    }

    /** Lists a quest under its name (the game's own above; tests add theirs). */
    public static void register(String name, Supplier<Quest> quest) {
        QUESTS.put(name, quest);
    }

    /**
     * New quests (each animal has its own) for names like "giveMilk,layEgg"; none for null or "".
     *
     * @throws IllegalArgumentException for a name not listed
     */
    public static List<Quest> create(String names) {
        List<Quest> quests = new ArrayList<>();
        if (names == null) {
            return quests;
        }
        for (String name : names.split(",")) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Supplier<Quest> quest = QUESTS.get(trimmed);
            if (quest == null) {
                throw new IllegalArgumentException("No quest named '" + trimmed + "' (see QuestBook)");
            }
            quests.add(quest.get());
        }
        return quests;
    }
}
