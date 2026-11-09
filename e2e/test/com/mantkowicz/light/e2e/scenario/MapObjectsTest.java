package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Objects placed in Tiled: an objectClass says what the object is, an optional sprite property picks how a sprite actor
 * looks (a cow's colour). Mistakes stop the game and say which map and cell.
 */
class MapObjectsTest {
    private static TestMap cow(Map<String, String> properties) {
        return TestMap.of(
                "~~~~~",
                "~.K.~",
                "~.P.~",
                "~~~~~").with('K', "COW", properties);
    }

    @Test
    void aCowIsBrownUnlessTheMapPicksAnotherColour() {
        GameDriver brown = GameDriver.start(cow(Map.of()).write("cow-default"));
        assertEquals("cow_brown/idle", brown.cowAnimation());

        GameDriver pink = GameDriver.start(cow(Map.of("sprite", "COW_PINK")).write("cow-pink"));
        assertEquals("cow_pink/idle", pink.cowAnimation());
        assertFalse(pink.crashed());
    }

    @Test
    void mistakesInObjectsAreExplained() {
        assertMistake(cow(Map.of("sprite", "RABBIT")), "object-sprite-of-another",
                "the object on cell (2,2): COW can't be drawn with sprite 'RABBIT', only with one of [COW_BROWN");
        assertMistake(cow(Map.of("sprite", "COW_BLUE")), "object-unknown-sprite",
                "COW can't be drawn with sprite 'COW_BLUE'");

        TestMap typo = TestMap.of("~~~~", "~PK~", "~~~~").with('K', "CWO", Map.of());
        assertMistake(typo, "object-typo", "the object on cell (2,1): objectClass 'CWO' isn't one of [SPRITE, LIGHT");

        TestMap dogWithSprite = TestMap.of("~~~~", "~PK~", "~~~~").with('K', "DOG", Map.of("sprite", "COW_PINK"));
        assertMistake(dogWithSprite, "object-needless-sprite", "DOG has no sprites to pick from, remove its sprite property");
    }

    private static void assertMistake(TestMap map, String name, String expected) {
        String path = map.write(name);
        RuntimeException error = assertThrows(RuntimeException.class, () -> GameDriver.start(path));
        String message = messages(error);
        assertTrue(message.contains(expected), message);
        assertTrue(message.contains(name + ".tmx"), "names the map: " + message);
    }

    /** Every message in the chain: the game's own is wrapped by the framework's. */
    private static String messages(Throwable error) {
        StringBuilder all = new StringBuilder();
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            all.append(cause.getMessage()).append(" / ");
        }
        return all.toString();
    }
}
