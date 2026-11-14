package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A field grows one kind of crop (Tiled's cropType, a carrot without it) and starts ripe. Like a chest it blocks its
 * cell and is used from next to it: its button shows the crop and makes it jump out onto a free cell next to the
 * field, which starts again as a seedling. Then the button shows the watering can: the rabbit waters the field (its
 * watering animation towards it, water falling onto it), and the crop grows a stage once the water has fallen. Ripe
 * again, the button shows the crop again. The can never runs dry.
 */
class FieldTest {
    /** The rabbit starts two cells below a carrot field. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..F..~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~")
            .with('F', "FIELD", Map.of());

    /** The same with corn, which grows in five stages instead of four. */
    private static final TestMap CORN = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..F..~",
            "~..P..~",
            "~.....~",
            "~~~~~~~")
            .with('F', "FIELD", Map.of("cropType", "CORN"));

    /** The rabbit stands right of a field. */
    private static final TestMap SIDE = TestMap.of(
            "~~~~~~",
            "~....~",
            "~.FP.~",
            "~....~",
            "~~~~~~")
            .with('F', "FIELD", Map.of("cropType", "TOMATO"));

    @Test
    void aRipeFieldGivesItsCropAndGrowsAgainWhenWatered() {
        GameDriver game = GameDriver.start(MAP.write("field"));
        Cell field = MAP.cell('F');
        assertEquals("carrot 3/3", game.field(), "no cropType: carrots, ripe at the start");
        assertFalse(game.isAccessible(field), "it blocks its cell, like a chest");

        game.click(field);
        game.seconds(2);
        assertTrue(game.usedFromNeighbours(field).contains(game.playerCell()), "walked next to it");
        assertEquals("crop", game.fieldButton(), "ripe: the button shows the crop");

        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals("carrot 0/3", game.field(), "a seedling is left");
        assertEquals(1, game.itemsOnGround().size(), game.itemsOnGround().toString());
        assertTrue(game.itemsOnGround().get(0).startsWith("Carrot cell="), game.itemsOnGround().toString());
        assertTrue(game.neighbours(field).contains(game.itemCell()), "the carrot lands next to the field");
        assertEquals("water", game.fieldButton(), "growing: the button shows the watering can");

        for (int stage = 1; stage <= 3; stage++) {
            game.clickButton("COLLECT");
            game.frames(2);
            assertEquals("rabbit/water_up", game.playerAnimation(), "below the field, the rabbit waters upwards");
            assertEquals("carrot " + (stage - 1) + "/3 + water", game.field(), "the water falls...");
            assertTrue(game.buttonAlphas("COLLECT").get(0) < 1, "the button waits meanwhile");
            game.clickButton("COLLECT"); // nothing more while the water falls
            game.seconds(1);
            assertEquals("carrot " + stage + "/3", game.field(), "...and it grows a stage");
            assertEquals("rabbit/idle_down", game.playerAnimation(), "the rabbit stands again");
            game.seconds(0.5f);
        }
        assertEquals("crop", game.fieldButton(), "ripe again: the crop");

        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(2, game.itemsOnGround().size(), "a second carrot: " + game.itemsOnGround());
        assertEquals("carrot 0/3", game.field());
        assertFalse(game.crashed());
    }

    @Test
    void cornGrowsInFiveStages() {
        GameDriver game = GameDriver.start(CORN.write("field-corn"));
        assertEquals("corn 4/4", game.field());
        game.seconds(0.5f);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertTrue(game.itemsOnGround().get(0).startsWith("Corn"), game.itemsOnGround().toString());
        for (int watering = 0; watering < 4; watering++) {
            assertEquals("water", game.fieldButton());
            game.clickButton("COLLECT");
            game.seconds(1.5f);
        }
        assertEquals("corn 4/4", game.field());
        assertEquals("crop", game.fieldButton());
        assertFalse(game.crashed());
    }

    /** Next to the field on its left or right, the rabbit waters facing down (the side poses aren't used). */
    @Test
    void besideTheFieldTheRabbitWatersFacingDown() {
        GameDriver game = GameDriver.start(SIDE.write("field-side"));
        assertEquals("tomato 3/3", game.field());
        game.seconds(0.5f);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        game.clickButton("COLLECT");
        game.frames(2);
        assertEquals("rabbit/water_down", game.playerAnimation());
        assertFalse(game.crashed());
    }

    /** Walking away while the water falls: the rabbit stops watering, the field still grows. */
    @Test
    void walkingAwayWhileWatering() {
        GameDriver game = GameDriver.start(SIDE.write("field-side"));
        game.seconds(0.5f);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        game.clickButton("COLLECT");
        game.frames(2);
        game.click(new Cell(4, 1));
        game.frames(10);
        assertTrue(game.playerAnimation().startsWith("rabbit/walk"), game.playerAnimation());
        game.seconds(2);
        assertEquals("tomato 1/3", game.field());
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "away from the field: no button");
        assertFalse(game.crashed());
    }
}
