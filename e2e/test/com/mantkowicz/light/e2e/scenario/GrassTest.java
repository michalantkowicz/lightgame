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
 * Grass (FeatureFlag.GROWING_GRASS, on as the game ships): every 30 s each tile the rabbit can get to may sprout grass
 * (1 %, 10 % next to grass), never a chest's or a field's. It grows a phase every 30 s: a tuft and two tufts under
 * whoever stands there, a bush and a big bush over them, a little see-through. It blocks nothing. Standing still on
 * grass, the HUD's tool button (the big round one right of the face) has the scythe instead of the hint: tapped, the
 * rabbit swings once and the grass fades out; walking off meanwhile, it stays. No action button over the grass.
 */
class GrassTest {
    /** Open grass with a chest and a field. */
    private static final TestMap MEADOW = TestMap.of(
            "~~~~~~~~~~~~~~~~",
            "~..............~",
            "~..............~",
            "~...C..........~",
            "~..............~",
            "~.......F......~",
            "~......P.......~",
            "~..............~",
            "~..............~",
            "~..............~",
            "~..............~",
            "~~~~~~~~~~~~~~~~")
            .with('F', "FIELD", Map.of());

    /** A small yard, all on screen. */
    private static final TestMap YARD = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~..P..~",
            "~.....~",
            "~~~~~~~");

    @Test
    void grassSproutsAndSpreadsWhereTheRabbitCanGo() {
        GameDriver game = GameDriver.start(MEADOW.write("grass-meadow"));
        assertEquals(Map.of(), game.grass(), "no grass at the start");
        game.seconds(10 * 60);
        Map<Cell, Integer> grass = game.grass();
        assertTrue(grass.size() > 10, "after ten minutes there is grass: " + grass);
        assertFalse(grass.containsKey(MEADOW.cell('C')), "never on the chest");
        assertFalse(grass.containsKey(MEADOW.cell('F')), "never on the field");
        for (Cell cell : grass.keySet()) {
            assertTrue(cell.col() >= 1 && cell.col() <= 14 && cell.row() >= 1 && cell.row() <= 10,
                    "only on the grass of the map: " + cell);
        }
        long nextToGrass = grass.keySet().stream()
                .filter(cell -> grass.keySet().stream()
                        .anyMatch(other -> !other.equals(cell) && Math.abs(other.col() - cell.col()) <= 1
                                && Math.abs(other.row() - cell.row()) <= 1))
                .count();
        assertTrue(nextToGrass > grass.size() / 2, "it spreads: most grass is next to grass (" + nextToGrass + " of "
                + grass.size() + ")");
        assertFalse(game.crashed());
    }

    @Test
    void withTheFlagOffNoGrassGrows() {
        GameDriver.growingGrass(false);
        try {
            GameDriver game = GameDriver.start(MEADOW.write("grass-meadow"));
            game.seconds(10 * 60);
            assertEquals(Map.of(), game.grass());
            assertFalse(game.crashed());
        } finally {
            GameDriver.growingGrass(true);
        }
    }

    /** A phase every 30 s; the third (a bush) and the fourth are drawn over whoever stands there, 90 % opaque. */
    @Test
    void grassGrowsAndGetsTall() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell cell = new Cell(1, 1);
        game.sproutGrass(cell);
        assertEquals(Map.of(cell, 0), game.grass());
        assertEquals("under", game.grassDrawn(cell), "a tuft: under whoever stands there");
        assertEquals(2, game.grassBelowTileBottom(cell),
                "the tuft's blades on the tile's bottom (2 empty rows under it)");
        game.seconds(30.1f);
        assertEquals(1, game.grass().get(cell));
        assertEquals("under", game.grassDrawn(cell), "two tufts: still under");
        assertEquals(0, game.grassBelowTileBottom(cell), "the other phases' blades reach their box's bottom");
        game.seconds(30);
        assertEquals(2, game.grass().get(cell));
        assertEquals("over alpha=0.9", game.grassDrawn(cell), "a bush: over everyone, a little see-through");
        game.seconds(30);
        assertEquals(3, game.grass().get(cell));
        game.seconds(60);
        assertEquals(3, game.grass().get(cell), "a big bush is as tall as it gets");
        assertFalse(game.crashed());
    }

    @Test
    void theRabbitCutsTheGrassItStandsOn() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell here = game.playerCell();
        game.sproutGrass(here);
        game.seconds(0.5f);
        assertEquals("scythe", game.toolButton(), "on grass: the scythe on the tool button");
        game.tapToolButton();
        game.frames(2);
        assertEquals("rabbit/scythe_down", game.playerAnimation(), "it swings its scythe");
        assertTrue(game.grass().containsKey(here), "the grass is still there while it swings");
        assertEquals(List.of(), game.buttonAlphas("CUT"), "no action button over the grass");
        game.seconds(0.8f);
        assertFalse(game.grass().containsKey(here), "swung once (0.8 s): the grass is cut...");
        assertEquals(1, game.grassFadingOut(), "...and fades out");
        assertEquals("rabbit/idle_down", game.playerAnimation());
        game.seconds(0.6f);
        assertEquals(0, game.grassFadingOut(), "gone");
        assertEquals("hint", game.toolButton(), "nothing left to cut: the hint again");
        assertFalse(game.crashed());
    }

    @Test
    void walkingOffWhileSwingingLeavesTheGrass() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell here = game.playerCell();
        game.sproutGrass(here);
        game.seconds(0.5f);
        game.tapToolButton();
        game.frames(10);
        game.click(new Cell(here.col() + 1, here.row()));
        game.seconds(2);
        assertTrue(game.grass().containsKey(here), "it walked off: the grass stays");
        assertEquals(0, game.grassFadingOut());
        assertFalse(game.crashed());
    }

    /** Grass blocks nothing: the rabbit walks onto it; walking across it shows no button, standing on it does. */
    @Test
    void grassBlocksNothing() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell grass = new Cell(game.playerCell().col() + 1, game.playerCell().row());
        game.sproutGrass(grass);
        game.seconds(70); // a bush: over everyone
        assertTrue(game.isAccessible(grass));
        game.click(grass);
        game.frames(10);
        assertEquals("hint", game.toolButton(), "walking: no scythe");
        game.seconds(2);
        assertEquals(grass, game.playerCell(), "it walked onto the grass (taps went through it)");
        assertEquals("scythe", game.toolButton(), "standing on it: the scythe");
        assertFalse(game.crashed());
    }
}
