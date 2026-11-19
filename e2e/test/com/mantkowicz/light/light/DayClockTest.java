package com.mantkowicz.light.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The time of day: day, dusk, night, dawn and again, the light dimming and coming back smoothly. */
class DayClockTest {
    private static final DayNightSettings SHORT = new DayNightSettings(10, 2, 6, 2, 0.2f);

    @Test
    void theDayGoesRound() {
        DayClock clock = new DayClock(SHORT);
        assertEquals(DayClock.Part.DAY, clock.getPart(), "the game starts in the morning");
        assertEquals(1, clock.getBrightness());
        clock.update(10.5f);
        assertEquals(DayClock.Part.DUSK, clock.getPart());
        assertTrue(clock.isNight(), "the night starts when the sun starts to set");
        clock.update(2);
        assertEquals(DayClock.Part.NIGHT, clock.getPart());
        assertEquals(0.2f, clock.getBrightness(), 1e-6);
        clock.update(6);
        assertEquals(DayClock.Part.DAWN, clock.getPart());
        assertFalse(clock.isNight(), "the day starts when the sun starts to rise");
        clock.update(2);
        assertEquals(DayClock.Part.DAY, clock.getPart(), "and round again");
        assertEquals(1, clock.getBrightness());
    }

    @Test
    void theLightChangesSmoothly() {
        DayClock clock = new DayClock(SHORT);
        clock.update(10);
        float last = clock.getBrightness();
        for (int i = 0; i < 20; i++) { // dusk, a tenth of a second at a time: darker and darker
            clock.update(0.1f);
            float now = clock.getBrightness();
            assertTrue(now <= last && last - now < 0.1f, "no jump at dusk: " + last + " -> " + now);
            last = now;
        }
        assertEquals(0.2f, last, 1e-4);
        clock.update(7); // halfway through dawn
        assertEquals(0.6f, clock.getBrightness(), 1e-4, "halfway up");
    }
}
