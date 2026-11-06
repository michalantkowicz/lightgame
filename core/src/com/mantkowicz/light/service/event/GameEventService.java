package com.mantkowicz.light.service.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A polled queue of game events, keyed by the event class.
 * Events live until they are polled, unless they have a max age.
 */
public class GameEventService {
    private final Map<Class<?>, List<GameEvent>> queue = new HashMap<>();

    public void post(GameEvent gameEvent) {
        eventsOf(gameEvent.getClass()).add(gameEvent);
    }

    /**
     * @return the oldest event of this class without removing it, or {@code null} if there is none
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> T peek(Class<T> type) {
        List<GameEvent> events = eventsOf(type);
        return events.isEmpty() ? null : (T) events.get(0);
    }

    /**
     * @return the oldest event of this class, removed from the queue, or {@code null} if there is none
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> T poll(Class<T> type) {
        List<GameEvent> events = eventsOf(type);
        return events.isEmpty() ? null : (T) events.remove(0);
    }

    public boolean has(Class<? extends GameEvent> type) {
        return !eventsOf(type).isEmpty();
    }

    /**
     * Keyed by the exact class, because GWT (HTML5 build) does not support {@code Class.isInstance}.
     * Event classes are concrete leaves, so this is equivalent.
     */
    private List<GameEvent> eventsOf(Class<?> type) {
        List<GameEvent> events = queue.get(type);
        if (events == null) {
            events = new ArrayList<>();
            queue.put(type, events);
        }
        return events;
    }

    /**
     * this method should be called at the end of main loop of the application to provide housekeeping
     */
    public void updateEventsLivesAndClean() {
        for (List<GameEvent> events : queue.values()) {
            Iterator<GameEvent> iterator = events.iterator();
            while (iterator.hasNext()) {
                GameEvent gameEvent = iterator.next();
                gameEvent.increaseAge();
                if (gameEvent.isTooOld()) {
                    iterator.remove();
                }
            }
        }
    }
}
