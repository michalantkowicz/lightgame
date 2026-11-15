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
 * field, which is then empty but for a tiny sprout (the same for every crop). Then the button shows the watering can: the rabbit waters the field (its
 * watering animation, water falling onto it), and once the water has fallen the crop grows a phase at once and then
 * by itself, a phase every 30 s, with no button meanwhile. Ripe again, the button shows the crop again. The can never
 * runs dry. Picking a crop makes the HUD's face happy.
 * <p>
 * Instead of the crop a weed may grow (Tiled's weedChance, 0.4 without it; the maps here say 0 or 1 so that chance
 * doesn't decide): its button pulls it out, it disappears, and the face sweats and says it is no use.
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
            .with('F', "FIELD", Map.of("weedChance", "0"));

    /** The same with corn, which grows in six phases (the sprout and five pictures) instead of five. */
    private static final TestMap CORN = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..F..~",
            "~..P..~",
            "~.....~",
            "~~~~~~~")
            .with('F', "FIELD", Map.of("cropType", "CORN", "weedChance", "0"));

    /** The rabbit stands right of a field. */
    private static final TestMap SIDE = TestMap.of(
            "~~~~~~",
            "~....~",
            "~.FP.~",
            "~....~",
            "~~~~~~")
            .with('F', "FIELD", Map.of("cropType", "TOMATO", "weedChance", "0"));

    /** The rabbit right of a field that always grows a weed. */
    private static final TestMap WEED = TestMap.of(
            "~~~~~~",
            "~....~",
            "~.FP.~",
            "~....~",
            "~~~~~~")
            .with('F', "FIELD", Map.of("weedChance", "1"));

    /** A field without weedChance. */
    private static final TestMap DEFAULT = TestMap.of(
            "~~~~~~",
            "~.FP.~",
            "~~~~~~")
            .with('F', "FIELD", Map.of());

    @Test
    void aRipeFieldGivesItsCropAndGrowsAgainWhenWatered() {
        GameDriver game = GameDriver.start(MAP.write("field"));
        Cell field = MAP.cell('F');
        assertEquals("carrot 4/4", game.field(), "no cropType: carrots, ripe at the start");
        assertEquals("carrot", game.fieldSign());
        assertFalse(game.isAccessible(field), "it blocks its cell, like a chest");

        game.click(field);
        game.seconds(2);
        assertTrue(game.usedFromNeighbours(field).contains(game.playerCell()), "walked next to it");
        assertEquals("crop", game.fieldButton(), "ripe: the button shows the crop");

        game.clickButton("COLLECT");
        game.frames(2);
        assertEquals("TEEMO_HAPPY", game.faceReaction(), "a crop: the face is happy...");
        game.seconds(1.5f);
        assertEquals("TEEMO_HAPPY", game.faceReaction(), "...three times over (2.5 s), once is easy to miss");
        game.seconds(1.2f);
        assertEquals("idle", game.faceReaction());
        assertEquals("carrot 0/4", game.field(), "an empty field is left, a tiny sprout");
        assertEquals("?", game.fieldSign(), "empty: a question mark on the sign");
        assertEquals(1, game.itemsOnGround().size(), game.itemsOnGround().toString());
        assertTrue(game.itemsOnGround().get(0).startsWith("Carrot cell="), game.itemsOnGround().toString());
        assertTrue(game.neighbours(field).contains(game.itemCell()), "the carrot lands next to the field");
        assertEquals("water", game.fieldButton(), "picked: the button shows the watering can");

        game.clickButton("COLLECT");
        game.frames(2);
        assertEquals("rabbit/water_up", game.playerAnimation(), "below the field, the rabbit waters upwards");
        assertEquals("carrot 0/4 + water", game.field(), "the water falls...");
        assertTrue(game.buttonAlphas("COLLECT").get(0) < 1, "the button waits meanwhile");
        game.clickButton("COLLECT"); // nothing more while the water falls
        game.seconds(1);
        assertEquals("carrot 1/4 growing", game.field(), "...it grows a phase at once and goes on growing");
        assertEquals("carrot", game.fieldSign(), "growing: the sign shows the crop again");
        assertEquals("rabbit/idle_down", game.playerAnimation(), "the rabbit stands again");
        game.seconds(2);
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "nothing to do while it grows: no button");
        assertTrue(squashes(game), "growing: the plant squashes and stretches");

        game.seconds(25); // 1.5 s went by in squashes()
        assertEquals("carrot 1/4 growing", game.field(), "a phase takes 30 s (counted from when the water has fallen)");
        game.seconds(1.5f);
        assertEquals("carrot 2/4 growing", game.field());
        game.seconds(30);
        assertEquals("carrot 3/4 growing", game.field());
        game.seconds(30);
        assertEquals("carrot 4/4", game.field(), "ripe: it stops growing");
        game.seconds(1);
        assertEquals("1.0/1.0", game.fieldPlantScale(), "ripe: the plant stands still");
        assertEquals("crop", game.fieldButton(), "and the button is back, with the crop");

        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(2, game.itemsOnGround().size(), "a second carrot: " + game.itemsOnGround());
        assertEquals("carrot 0/4", game.field());
        assertFalse(game.crashed());
    }

    @Test
    void cornGrowsInSixPhases() {
        GameDriver game = GameDriver.start(CORN.write("field-corn"));
        assertEquals("corn 5/5", game.field());
        assertTrue(game.fieldTopInTiles() > 1.5f, "ripe corn is tall: its button sits over it");
        game.seconds(0.5f);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(1, game.fieldTopInTiles(), "empty: the watering can's button as high as on any field");
        assertTrue(game.itemsOnGround().get(0).startsWith("Corn"), game.itemsOnGround().toString());
        assertEquals("water", game.fieldButton());
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals("corn 1/5 growing", game.field(), "one watering is enough");
        game.seconds(4 * 30);
        assertEquals("corn 5/5", game.field(), "four more phases, 30 s each");
        assertEquals("crop", game.fieldButton());
        assertFalse(game.crashed());
    }

    /** Within a second and a half the plant gets wider and shorter than it is (and never taller or thinner). */
    private static boolean squashes(GameDriver game) {
        boolean squashed = false;
        for (int frame = 0; frame < 90; frame++) {
            game.frames(1);
            String[] scale = game.fieldPlantScale().split("/");
            float x = Float.parseFloat(scale[0]);
            float y = Float.parseFloat(scale[1]);
            assertTrue(x >= 1 && y <= 1, "squashes, never stretches up: " + game.fieldPlantScale());
            squashed |= x > 1.02f && y < 0.98f;
        }
        return squashed;
    }

    /** Next to the field on its left or right, the rabbit waters facing down (the side poses aren't used). */
    @Test
    void besideTheFieldTheRabbitWatersFacingDown() {
        GameDriver game = GameDriver.start(SIDE.write("field-side"));
        assertEquals("tomato 4/4", game.field());
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
        assertEquals("tomato 1/4 growing", game.field());
        assertEquals(List.of(), game.buttonAlphas("COLLECT"), "away from the field: no button");
        game.seconds(90);
        assertEquals("tomato 4/4", game.field(), "it grew with nobody next to it");
        assertFalse(game.crashed());
    }

    /** Without weedChance in Tiled, a weed grows 4 times in 10. */
    @Test
    void aFieldGrowsAWeedFourTimesInTen() {
        GameDriver game = GameDriver.start(DEFAULT.write("field-default"));
        assertEquals(0.4f, game.fieldWeedChance());
        assertFalse(game.crashed());
    }

    /** A weed grows from the sprout instead of the crop; pulled out, it gives nothing and the face sweats. */
    @Test
    void aWeedIsPulledOutAndGivesNothing() {
        GameDriver game = GameDriver.start(WEED.write("field-weed"));
        assertEquals("carrot 4/4", game.field(), "it starts with its crop, ripe");
        game.seconds(0.5f);
        game.clickButton("COLLECT");
        game.seconds(1.5f);
        assertEquals(1, game.itemsOnGround().size());
        game.clickButton("COLLECT"); // waters it
        game.seconds(1.5f);
        assertEquals("weed 1/3 growing", game.field(), "a weed instead of the carrot");
        assertEquals("?", game.fieldSign(), "the sign keeps its question mark");
        game.seconds(2 * 30);
        assertEquals("weed 3/3", game.field(), "a ripe weed");
        game.seconds(0.5f);
        assertEquals("crop", game.fieldButton(), "the button is back (showing the weed)");

        game.clickButton("COLLECT");
        game.frames(2);
        assertEquals("carrot 0/4", game.field(), "pulled out: the field is empty again...");
        assertTrue(game.fieldWeedFadingOut(), "...while the weed rises a little and fades out");
        assertEquals("?", game.fieldSign());
        assertEquals(1, game.itemsOnGround().size(), "nothing comes out: " + game.itemsOnGround());
        assertEquals("TEEMO_SWEAT", game.faceReaction(), "the face sweats...");
        assertEquals("Beautiful weed but unusable!", game.faceSays(), "...and says so");
        assertEquals("water", game.fieldButton(), "empty: water it again");
        game.seconds(0.6f);
        assertFalse(game.fieldWeedFadingOut(), "gone after half a second");
        game.seconds(2.3f);
        assertEquals("Beautiful weed but unusable!", game.faceSays(), "for 3 s");
        game.seconds(0.5f);
        assertEquals("idle", game.faceReaction(), "then the face is idle again");
        game.seconds(0.5f);
        assertEquals(null, game.faceSays(), "and the bubble has faded out");
        assertFalse(game.crashed());
    }
}
