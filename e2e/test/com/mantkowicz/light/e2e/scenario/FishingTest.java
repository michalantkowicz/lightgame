package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fishing (FeatureFlag.FISHING, on as the game ships) from a fishing spot (Tiled's FISHING_SPOT): standing still on
 * it, the tool button shows the fishing rod; tapped, the rabbit casts to the left, the bobber drops into the water two
 * tiles away and bobs 1-5 times, then a fish pulls it under: the rod shakes, the face says "!!!", and a tap anywhere
 * within 2 s opens the fishing game (catch the leap: three catches win a fish, three misses lose it). No tap in time:
 * the fish is gone and the face is sad.
 */
class FishingTest {
    /** A pond on the left; the fishing spot 'S' two tiles right of the water. */
    private static final TestMap POND = TestMap.of(
            "~~~~~~~~~",
            "~~~.....~",
            "~~~.S.P.~",
            "~~~.....~",
            "~~~~~~~~~")
            .with('S', "FISHING_SPOT", Map.of());

    private static GameDriver onTheSpot() {
        GameDriver game = GameDriver.start(POND.write("fishing-pond"));
        game.click(POND.cell('S'));
        game.seconds(2);
        assertEquals(POND.cell('S'), game.playerCell());
        return game;
    }

    /** Casts and waits for the bite (the bobber bobs at most 5 times). */
    private static void castAndWaitForTheBite(GameDriver game) {
        game.tapToolButton();
        for (int frame = 0; frame < 20 * 60 && !"BITE".equals(game.fishing()); frame++) {
            game.frames(1);
        }
        assertEquals("BITE", game.fishing());
    }

    @Test
    void theRodIsOnTheToolButtonOnlyOnTheSpot() {
        GameDriver game = GameDriver.start(POND.write("fishing-pond"));
        assertEquals(List.of(POND.cell('S')), game.fishingSpots());
        assertEquals("hint", game.toolButton(), "not on the spot");
        game.click(POND.cell('S'));
        game.seconds(2);
        assertEquals("fishing rod", game.toolButton());
        game.click(new Cell(6, 2));
        game.seconds(2);
        assertEquals("hint", game.toolButton(), "walked off");
        assertFalse(game.crashed());
    }

    @Test
    void castTheBobberBobsAndAFishBites() {
        GameDriver game = onTheSpot();
        game.tapToolButton();
        assertEquals("CASTING", game.fishing());
        game.frames(1);
        assertEquals("rabbit_fishing/cast_left", game.playerAnimation());
        game.seconds(1);
        assertEquals("WAITING", game.fishing());
        Cell spot = POND.cell('S');
        assertTrue(game.bobber().endsWith("at " + new Cell(spot.col() - 2, spot.row())),
                "two tiles left, in the water: "
                        + game.bobber());
        assertEquals("rabbit_fishing/wait_left", game.playerAnimation());
        for (int frame = 0; frame < 20 * 60 && !"BITE".equals(game.fishing()); frame++) {
            game.frames(1);
        }
        assertEquals("BITE", game.fishing());
        game.frames(1);
        assertEquals("none", game.bobber(), "pulled under");
        assertEquals("rabbit_fishing/bite_left", game.playerAnimation(), "the rod shakes");
        assertEquals("!!!", game.faceSays());
        assertEquals("TEEMO_HAPPY", game.faceReaction());
        assertTrue(game.biteSecondsLeft() > 1.5f, "the paw's circle: " + game.biteSecondsLeft());
        assertFalse(game.crashed());
    }

    @Test
    void noTapInTimeTheFishIsGone() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.seconds(2.1f);
        assertEquals("READY", game.fishing());
        assertEquals("TEEMO_SAD", game.faceReaction());
        assertEquals("rabbit/idle_down", game.playerAnimation(), "the rod is away");
        game.frames(2);
        assertEquals("fishing rod", game.toolButton(), "it can cast again");
        assertFalse(game.crashed());
    }

    @Test
    void hookedAndPlayedWellAFishGoesIntoTheBackpack() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        assertEquals("GAME", game.fishing());
        assertNotEquals("none", game.fishingGame());
        for (int frame = 0; frame < 120 * 60 && "GAME".equals(game.fishing()); frame++) {
            if (game.fishInGreen()) {
                game.tapAnywhere();
            } else {
                game.frames(1);
            }
        }
        assertEquals("LANDING", game.fishing(), "won");
        game.frames(1);
        assertEquals("rabbit_fishing/pull_left", game.playerAnimation());
        assertEquals("TEEMO_COOL_ON", game.faceReaction(), "sunglasses on");
        assertTrue(game.backpackItems().get(0).toString().endsWith("fish"), "a fish: " + game.backpackItems());
        game.seconds(3);
        assertEquals("READY", game.fishing());
        assertFalse(game.crashed());
    }

    @Test
    void doingNothingInTheGameLoses() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        for (int frame = 0; frame < 120 * 60 && "GAME".equals(game.fishing()); frame++) {
            game.frames(1);
        }
        assertEquals("READY", game.fishing(), "lost");
        assertEquals("TEEMO_SAD", game.faceReaction());
        assertTrue(game.backpackItems().stream().allMatch(item -> item == null), "no fish");
        assertFalse(game.crashed());
    }

    @Test
    void aTapWhileTheFishIsBelowTheGreenZoneIsAMiss() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        game.tapAnywhere();
        assertTrue(game.fishingGame().startsWith("caught 0, missed 0"), "between leaps a tap does nothing: "
                + game.fishingGame());
        while (!game.fishLeaping()) {
            game.frames(1);
        }
        game.frames(3);
        assertFalse(game.fishInGreen());
        game.tapAnywhere();
        assertTrue(game.fishingGame().startsWith("caught 0, missed 1"), game.fishingGame());
        assertEquals("TEEMO_SAD", game.faceReaction());
        assertFalse(game.crashed());
    }

    @Test
    void walkingOffWhileWaitingTakesTheLineOut() {
        GameDriver game = onTheSpot();
        game.tapToolButton();
        game.seconds(1);
        assertEquals("WAITING", game.fishing());
        game.click(new Cell(6, 2));
        game.frames(5);
        assertEquals("READY", game.fishing());
        assertEquals("none", game.bobber());
        assertFalse(game.crashed());
    }

    /** A fishing spot walled in: only the cells left and right of it are free. */
    private static final TestMap NOOK = TestMap.of(
            "~~~~~~~",
            "~~~xxx~",
            "~~~aSP~",
            "~~~xxx~",
            "~~~~~~~")
            .with('S', "FISHING_SPOT", Map.of());

    /**
     * With a full backpack the fish lands next to the rabbit; with no free cell there either it isn't collected (the
     * owner's choice: fish don't pile up on the shore).
     */
    @Test
    void withNoRoomTheFishIsNotCollected() {
        GameDriver game = GameDriver.start(NOOK.write("fishing-nook"));
        Cell spot = NOOK.cell('S');
        Cell right = new Cell(spot.col() + 1, spot.row());
        game.click(spot);
        game.seconds(2);
        game.putIntoBackpack("carrot");
        game.putIntoBackpack("carrot");
        game.dropCrop("corn", NOOK.cell('a'));
        game.dropCrop("corn", right);
        game.seconds(1);
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        for (int frame = 0; frame < 120 * 60 && "GAME".equals(game.fishing()); frame++) {
            if (game.fishInGreen()) {
                game.tapAnywhere();
            } else {
                game.frames(1);
            }
        }
        assertEquals("LANDING", game.fishing(), "won");
        game.seconds(1);
        assertTrue(game.itemsOnGround().stream().noneMatch(item -> item.startsWith("Fish(")), "not on the ground: "
                + game.itemsOnGround());
        assertTrue(game.backpackItems().stream().noneMatch(item -> item.toString().endsWith("fish")),
                "not in the backpack: " + game.backpackItems());
        assertFalse(game.crashed());
    }

    /** Level 1 has a fishing spot at the end of a little pier on its left shore, water two tiles to its left. */
    @Test
    void levelOneHasAFishingSpot() {
        GameDriver game = GameDriver.start(GameDriver.defaultMap());
        assertEquals(List.of(new Cell(2, 6)), game.fishingSpots());
        assertTrue(game.isAccessible(new Cell(2, 6)), "the pier can be walked on");
        assertFalse(game.isAccessible(new Cell(0, 6)), "water two tiles left");
        assertFalse(game.crashed());
    }

    @Test
    void aMapCanSwitchFishingOff() {
        TestMap pond = TestMap.of(
                "~~~~~~~~~",
                "~~~.....~",
                "~~~.S.P.~",
                "~~~.....~",
                "~~~~~~~~~")
                .with('S', "FISHING_SPOT", Map.of())
                .withMapProperty("featuresOff", "FISHING");
        GameDriver game = GameDriver.start(pond.write("fishing-pond-off"));
        game.click(pond.cell('S'));
        game.seconds(2);
        assertEquals("none", game.fishing(), "the map says: no fishing here");
        assertEquals("hint", game.toolButton());
        assertFalse(game.crashed());
    }

    @Test
    void withTheFlagOffThereIsNoFishing() {
        GameDriver.fishing(false);
        try {
            GameDriver game = GameDriver.start(POND.write("fishing-pond"));
            game.click(POND.cell('S'));
            game.seconds(2);
            assertEquals("none", game.fishing());
            assertEquals("hint", game.toolButton());
            assertFalse(game.crashed());
        } finally {
            GameDriver.fishing(true);
        }
    }
}
