package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Doors to other maps (Tiled's DOOR objects): whoever ends its walk on a door, and is let through, comes out of the
 * door with the same name on the other map. The player goes behind a curtain (the screen darkens and lightens, 0.3 s
 * each way), carrying its backpack; the map it left lives on (a watered field grows meanwhile). It doesn't go back
 * until it has walked off the door and back on. An animal goes at once, as hungry as it was; a door with "passes" lets
 * only those kinds through.
 */
class TravelTest {
    private static final String FARM_NAME = "travel-farm", COOP_NAME = "travel-coop";

    /** The farm: a field, the rabbit, a door to the coop on the right. */
    private static final TestMap FARM = TestMap.of(
            "~~~~~~~~",
            "~.....O~",
            "~.FP...~",
            "~~~~~~~~")
            .with('F', "FIELD", Map.of("weedChance", "0"))
            .with('O', "DOOR", door(COOP_NAME));

    /** The coop: its door to the farm, a corner to walk to. */
    private static final TestMap COOP = TestMap.of(
            "~~~~~",
            "~O..~",
            "~..a~",
            "~~~~~")
            .with('O', "DOOR", door(FARM_NAME));

    private static Map<String, String> door(String to) {
        return Map.of("door", "coop", "targetMap", TestMap.pathOf(to), "passes", "PLAYER,CHICKEN");
    }

    @Test
    void theFieldGrowsWhileTheRabbitIsInTheCoop() {
        COOP.write(COOP_NAME);
        String farm = FARM.write(FARM_NAME);
        GameDriver game = GameDriver.start(farm);
        assertEquals(List.of(farm), game.visitedMaps(), "only the map the game starts on is there");

        game.click(FARM.cell('F'));
        game.seconds(2);
        game.clickButton("COLLECT"); // the carrot jumps out next to the field
        game.seconds(2);
        game.clickButton("COLLECT"); // the watering can
        game.seconds(1);
        assertEquals("carrot 1/4 growing", game.field());
        game.click(game.itemCell());
        game.seconds(2);
        assertTrue(game.backpackItems().toString().contains("CARROT"), "picked up: " + game.backpackItems());

        game.click(FARM.cell('O'));
        game.seconds(1.5f); // three tiles at 0.4 s each
        assertEquals(farm, game.currentMap(), "on the door: the curtain comes down first");
        assertTrue(game.curtain() > 0, "the screen darkens");
        game.seconds(0.4f);
        assertEquals(TestMap.pathOf(COOP_NAME), game.currentMap(), "behind the curtain: in the coop");
        assertEquals(COOP.cell('O'), game.playerCell(), "out of the coop's door");
        assertEquals(List.of(farm, TestMap.pathOf(COOP_NAME)), game.visitedMaps(), "the coop is built on the way");
        game.seconds(0.5f);
        assertEquals(0, game.curtain(), "and the screen is light again");
        assertTrue(game.backpackItems().toString().contains("CARROT"),
                "the backpack came along: " + game.backpackItems());

        game.seconds(3);
        assertEquals(TestMap.pathOf(COOP_NAME), game.currentMap(), "standing on the door it came out of, it stays");

        game.seconds(60); // meanwhile on the farm...
        game.click(COOP.cell('a'));
        game.seconds(2);
        game.click(COOP.cell('O'));
        game.seconds(3);
        assertEquals(farm, game.currentMap(), "walked off and back onto the door: back on the farm");
        assertEquals(FARM.cell('O'), game.playerCell(), "out of the farm's door");
        assertEquals("carrot 3/4 growing", game.field(), "the field grew while the rabbit was in the coop");
        assertFalse(game.crashed());
    }

    /** A chicken in a dead end whose only way is the door. */
    private static final TestMap CHICKEN_FARM = TestMap.of(
            "~~~~~~~",
            "~KO..P~",
            "~~~~~~~")
            .with('K', "CHICKEN", Map.of())
            .with('O', "DOOR", door(COOP_NAME + "-k"));

    private static final TestMap CHICKEN_COOP = TestMap.of(
            "~~~~~~",
            "~O...~",
            "~~~~~~")
            .with('O', "DOOR", door(FARM_NAME + "-k"));

    @Test
    void aChickenGoesThroughTheDoorAsHungryAsItWas() {
        CHICKEN_COOP.write(COOP_NAME + "-k");
        String farm = CHICKEN_FARM.write(FARM_NAME + "-k");
        String coop = TestMap.pathOf(COOP_NAME + "-k");
        GameDriver game = GameDriver.start(farm);
        int frames = 0;
        while (frames < 60 * 60 && !coop.equals(game.whereIs(Kind.CHICKEN))) {
            game.frames(1);
            frames++;
        }
        assertEquals(coop, game.whereIs(Kind.CHICKEN), "its one way out was the door: it went into the coop");
        assertEquals(farm, game.currentMap(), "the rabbit stays on the farm");
        assertTrue(frames < 20 * 60, "it went before it got hungry (20 s): " + frames / 60f + " s");

        while (!"WISHING".equals(game.animalStateOnAnyMap(Kind.CHICKEN)) && frames < 60 * 60) {
            game.frames(1);
            frames++;
        }
        assertTrue(frames < 22 * 60, "it wishes for corn about 20 s after the start, as on the farm, not 20 s after it"
                + " came into the coop (it lives on there, unseen): " + frames / 60f + " s");

        assertTrue(List.of(farm, coop).contains(game.whereIs(Kind.CHICKEN)),
                "still there, on one map or the other (the coop's door lets it back out)");
        assertFalse(game.crashed());
    }

    /** A cow in a dead end whose only way is the door, which lets only the rabbit and chickens through. */
    private static final TestMap COW_FARM = TestMap.of(
            "~~~~~~~",
            "~MO..P~",
            "~~~~~~~")
            .with('O', "DOOR", door(COOP_NAME + "-m"));

    private static final TestMap COW_COOP = TestMap.of(
            "~~~~~~",
            "~O...~",
            "~~~~~~")
            .with('O', "DOOR", door(FARM_NAME + "-m"));

    @Test
    void theCowStaysOutOfTheCoop() {
        COW_COOP.write(COOP_NAME + "-m");
        String farm = COW_FARM.write(FARM_NAME + "-m");
        GameDriver game = GameDriver.start(farm);
        boolean steppedOnTheDoor = false;
        for (int second = 0; second < 300; second++) {
            game.seconds(1);
            steppedOnTheDoor |= COW_FARM.cell('O').equals(game.cellOf(Kind.COW));
        }
        assertTrue(steppedOnTheDoor, "the cow did walk onto the door");
        assertEquals(farm, game.whereIs(Kind.COW), "but the door doesn't let cows through");
        assertEquals(List.of(farm), game.visitedMaps(), "nobody went to the coop");
        assertFalse(game.crashed());
    }
}
