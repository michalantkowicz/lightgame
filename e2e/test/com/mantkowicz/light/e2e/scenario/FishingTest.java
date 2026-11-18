package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fishing (FeatureFlag.FISHING, on as the game ships) from a fishing spot (Tiled's FISHING_SPOT): standing still on
 * it, the tool button shows the fishing rod; tapped, the rabbit casts to the left, the bobber drops into the water two
 * tiles away and bobs 3-10 s, then a fish pulls it under: the rod shakes, the face says "!!!", and a tap anywhere
 * within 1.2 s opens the fishing game (fish down three lanes, a tap on a lane's net catches the fish on the line;
 * three misses lose). No tap in time: the fish is gone and the face is sad. Now and then (30 %) no fish comes at all.
 * The tests switch that off, but one.
 */
class FishingTest {
    @BeforeEach
    void aFishAlwaysComes() {
        GameDriver.fishingNoFishChance(0);
    }

    @AfterEach
    void asTheGameShips() {
        GameDriver.fishingNoFishChance(0.3f);
    }

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

    /** Casts and waits for the bite (the bobber bobs 3 to 10 s). */
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
        assertTrue(game.biteSecondsLeft() > 1f, "the paw's circle: " + game.biteSecondsLeft());
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

    /** Taps each fish's net when it is on the line, until the game is over. */
    private static void playWell(GameDriver game) {
        for (int frame = 0; frame < 30 * 60 && "GAME".equals(game.fishing()); frame++) {
            int lane = game.fishOnTheLine();
            if (lane >= 0) {
                game.tapNet(lane);
            } else {
                game.frames(1);
            }
        }
    }

    @Test
    void hookedAndPlayedWellAFishGoesIntoTheBackpack() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        assertEquals("GAME", game.fishing());
        assertEquals("caught 0, missed 0, of 12 fish", game.fishingGame());
        playWell(game);
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
        while (game.fishingGame().contains("missed 0")) {
            game.frames(1);
        }
        assertTrue(game.fishingGame().startsWith("caught 0, missed 1"), "a fish past the line: " + game.fishingGame());
        assertEquals("TEEMO_SAD", game.faceReaction());
        for (int frame = 0; frame < 30 * 60 && "GAME".equals(game.fishing()); frame++) {
            game.frames(1);
        }
        assertEquals("READY", game.fishing(), "lost at the third miss");
        assertEquals("TEEMO_SAD", game.faceReaction());
        assertTrue(game.backpackItems().stream().allMatch(item -> item == null), "no fish");
        assertFalse(game.crashed());
    }

    @Test
    void aTapWithNoFishOnTheLineTanglesTheNet() {
        GameDriver game = onTheSpot();
        castAndWaitForTheBite(game);
        game.tapAnywhere();
        game.tapNet(1);
        assertTrue(game.netTangled(1), "nothing on the line yet");
        assertTrue(game.fishingGame().startsWith("caught 0, missed 0"), "not a miss: " + game.fishingGame());
        game.seconds(0.5f);
        assertFalse(game.netTangled(1), "only for a moment");
        assertFalse(game.crashed());
    }

    @Test
    void sometimesNoFishComes() {
        GameDriver.fishingNoFishChance(1);
        GameDriver game = onTheSpot();
        game.tapToolButton();
        game.seconds(1.5f);
        assertEquals("WAITING", game.fishing());
        game.seconds(9);
        assertEquals("WAITING", game.fishing(), "it bobs the longest wait");
        for (int frame = 0; frame < 3 * 60 && "WAITING".equals(game.fishing()); frame++) {
            game.frames(1);
        }
        assertEquals("READY", game.fishing(), "nothing came: the line is out");
        assertEquals("none", game.bobber());
        assertEquals("TEEMO_SAD", game.faceReaction());
        game.frames(2);
        assertEquals("fishing rod", game.toolButton(), "it can cast again");
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
        playWell(game);
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
