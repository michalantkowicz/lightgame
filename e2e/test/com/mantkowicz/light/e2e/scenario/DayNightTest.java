package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.LevelVariant;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day and night (FeatureFlag.DAY_AND_NIGHT, on as the game ships) on a map with daylight: 30 s of day, the light
 * dimming in its last 5 s, then 30 s of night at a tenth of the day's light, brightening in its last 5 s, and again.
 * The house (a map without ambientLight) keeps its light.
 */
class DayNightTest {
    private static final TestMap MAP = TestMap.of(
            "~~~~~",
            "~.P.~",
            "~~~~~");

    @Test
    void dayDimsToNightAndBack() {
        GameDriver game = GameDriver.start(daylight("daylight-1.tmx", "1"));
        game.seconds(20);
        assertEquals(1f, brightness(game), "the game starts in the day");

        List<Float> dusk = new ArrayList<>();
        for (int second = 0; second < 7; second++) {
            game.seconds(1);
            dusk.add(brightness(game));
        }
        assertEquals(1f, dusk.get(0), "full daylight until 5 s before the night: " + dusk);
        for (int i = 1; i < dusk.size(); i++) {
            assertTrue(dusk.get(i) <= dusk.get(i - 1), "it only gets darker: " + dusk);
        }
        assertTrue(dusk.stream().anyMatch(light -> light > 0.1f && light < 1f), "on the way: " + dusk);

        game.seconds(5); // 32 s
        assertEquals(0.1f, brightness(game), "night: a tenth of the day's light");
        game.seconds(20); // 52 s
        assertEquals(0.1f, brightness(game), "still night");
        game.seconds(5); // 57 s: dawn
        float dawn = brightness(game);
        assertTrue(dawn > 0.1f && dawn < 1f, "getting brighter: " + dawn);
        game.seconds(5); // 62 s
        assertEquals(1f, brightness(game), "day again");
        assertFalse(game.crashed());
    }

    @Test
    void theNightIsATenthOfTheMapsDaylight() {
        GameDriver game = GameDriver.start(daylight("daylight-dusk.tmx", "0.5"));
        assertEquals("0.00,0.00,0.00,0.50", game.ambientLight());
        game.seconds(40);
        assertEquals("0.00,0.00,0.00,0.05", game.ambientLight());
    }

    @Test
    void theHouseHasNoNight() {
        GameDriver game = GameDriver.start(MAP.write("no-daylight"));
        String light = game.ambientLight();
        game.seconds(40);
        assertEquals(light, game.ambientLight());
    }

    @Test
    void switchedOffItStaysDay() {
        GameDriver.dayAndNight(false);
        try {
            GameDriver game = GameDriver.start(daylight("daylight-off.tmx", "1"));
            game.seconds(40);
            assertEquals("0.00,0.00,0.00,1.00", game.ambientLight());
        } finally {
            GameDriver.dayAndNight(true);
        }
    }

    private static String daylight(String fileName, String ambientLight) {
        return LevelVariant.of(MAP).mapProperty("ambientLight", ambientLight).write(fileName);
    }

    /** The ambient light's brightness (its alpha), rounded to two places. */
    private static float brightness(GameDriver game) {
        String[] rgba = game.ambientLight().split(",");
        return Float.parseFloat(rgba[3]);
    }
}
