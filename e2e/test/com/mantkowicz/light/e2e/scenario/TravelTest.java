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
    private static final TestMap FARM = farm(COOP_NAME);

    private static TestMap farm(String coop) {
        return TestMap.of(
                "~~~~~~~~",
                "~.....O~",
                "~.FP...~",
                "~~~~~~~~")
                .with('F', "FIELD", Map.of("weedChance", "0"))
                .with('O', "DOOR", door(coop));
    }

    /** The coop: its door to the farm, a corner to walk to. */
    private static final TestMap COOP = coop(FARM_NAME);

    private static TestMap coop(String farm) {
        return TestMap.of(
                "~~~~~",
                "~O..~",
                "~..a~",
                "~~~~~")
                .with('O', "DOOR", door(farm));
    }

    private static Map<String, String> door(String to) {
        return Map.of("door", "coop", "targetMap", TestMap.pathOf(to), "passes", "PLAYER,CHICKEN");
    }

    @Test
    void theFieldGrowsWhileTheRabbitIsInTheCoop() {
        COOP.write(COOP_NAME);
        String farm = FARM.write(FARM_NAME);
        GameDriver game = GameDriver.start(farm);
        assertEquals(List.of(farm, TestMap.pathOf(COOP_NAME)), game.maps(),
                "the coop is built at the start: the farm's door leads there");

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
    private static final TestMap CHICKEN_FARM = chickenFarm(COOP_NAME + "-k");

    private static TestMap chickenFarm(String coop) {
        return TestMap.of(
                "~~~~~~~",
                "~KO..P~",
                "~~~~~~~")
                .with('K', "CHICKEN", Map.of())
                .with('O', "DOOR", door(coop));
    }

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
        assertFalse(game.crashed());
    }

    /** A coop of one cell: once a hen has come in, there is no room for the rabbit. */
    private static final TestMap TINY_COOP = TestMap.of(
            "~~~",
            "~O~",
            "~~~")
            .with('O', "DOOR", door(FARM_NAME + "-full"));

    @Test
    void withNoRoomBehindTheDoorTheRabbitStaysAndCanWalkOff() {
        TINY_COOP.write(COOP_NAME + "-full");
        String farm = chickenFarm(COOP_NAME + "-full").write(FARM_NAME + "-full");
        String coop = TestMap.pathOf(COOP_NAME + "-full");
        GameDriver game = GameDriver.start(farm);
        for (int frames = 0; frames < 60 * 60 && !coop.equals(game.whereIs(Kind.CHICKEN)); frames++) {
            game.frames(1);
        }
        assertEquals(coop, game.whereIs(Kind.CHICKEN), "the hen took the coop's only cell");

        game.click(CHICKEN_FARM.cell('O'));
        game.seconds(2);
        assertEquals(CHICKEN_FARM.cell('O'), game.playerCell(), "on the door");
        assertEquals(0, game.curtain(), "no room in the coop: the curtain doesn't come down");
        game.seconds(3);
        assertEquals(farm, game.currentMap(), "and the rabbit stays on the farm");
        assertEquals(0, game.curtain(), "not again and again either");

        game.click(CHICKEN_FARM.cell('P'));
        game.seconds(2);
        assertEquals(CHICKEN_FARM.cell('P'), game.playerCell(), "taps work: it walks off the door");
        assertFalse(game.crashed());
    }

    @Test
    void theScytheGoesFromTheToolButtonWhenTheRabbitGoes() {
        coop(FARM_NAME + "-s").write(COOP_NAME + "-s");
        String farm = farm(COOP_NAME + "-s").write(FARM_NAME + "-s");
        GameDriver game = GameDriver.start(farm);
        game.sproutGrass(FARM.cell('O'));
        game.click(FARM.cell('O'));
        game.seconds(1.5f);
        assertEquals("scythe", game.toolButton(), "standing on grass on the door: the scythe");
        game.seconds(1);
        assertEquals(TestMap.pathOf(COOP_NAME + "-s"), game.currentMap());
        assertEquals("door", game.toolButton(), "in the coop the farm's scythe is gone (the coop's door is there)");
        assertFalse(game.crashed());
    }

    @Test
    void theDoorOnTheToolButtonTakesTheRabbitBack() {
        coop(FARM_NAME + "-t").write(COOP_NAME + "-t");
        String farm = farm(COOP_NAME + "-t").write(FARM_NAME + "-t");
        String coop = TestMap.pathOf(COOP_NAME + "-t");
        GameDriver game = GameDriver.start(farm);
        assertEquals("hint", game.toolButton(), "it didn't come out of a door: no door on the button");

        game.click(FARM.cell('O'));
        game.seconds(3);
        assertEquals(coop, game.currentMap());
        assertEquals("door", game.toolButton(), "standing on the coop's door it came out of: the door on the button");

        game.tapToolButton();
        game.seconds(1);
        assertEquals(farm, game.currentMap(), "tapped: back through the door, as if it had walked onto it again");
        assertEquals(FARM.cell('O'), game.playerCell(), "out of the farm's door");
        assertEquals("door", game.toolButton(), "on the farm's door it came out of: the door again");

        game.click(FARM.cell('P'));
        game.seconds(2);
        assertEquals("hint", game.toolButton(), "walked off the door: the door goes");
        assertFalse(game.crashed());
    }

    /** The farm leads to the coop, the coop to a cellar too: the doors' chain. */
    private static final TestMap CHAIN_COOP = TestMap.of(
            "~~~~~",
            "~O.L~",
            "~~~~~")
            .with('O', "DOOR", door(FARM_NAME + "-c"))
            .with('L', "DOOR", Map.of("door", "cellar", "targetMap", TestMap.pathOf("travel-cellar")));

    private static final TestMap CELLAR = TestMap.of(
            "~~~~",
            "~L.~",
            "~~~~")
            .with('L', "DOOR", Map.of("door", "cellar", "targetMap", TestMap.pathOf(COOP_NAME + "-c")));

    @Test
    void everyMapAChainOfDoorsLeadsToIsBuiltAtTheStart() {
        CELLAR.write("travel-cellar");
        CHAIN_COOP.write(COOP_NAME + "-c");
        String farm = farm(COOP_NAME + "-c").write(FARM_NAME + "-c");
        GameDriver game = GameDriver.start(farm);
        assertEquals(List.of(farm, TestMap.pathOf(COOP_NAME + "-c"), TestMap.pathOf("travel-cellar")), game.maps());
        assertFalse(game.crashed());
    }
}
