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
 * (0.1 %, 1 % next to grass), never a chest's or a field's; the map may put grass on tiles too (Tiled's GRASS
 * objects, with a phase 1-4). It grows a phase every 30 s: a tuft and two tufts under
 * whoever stands there, a bush and a big bush over them, a little see-through. It blocks nothing. Standing still on
 * grass, the HUD's tool button (the big round one right of the face) has the scythe instead of the hint (just next to
 * grass: the hint): tapped, the rabbit swings once and the grass on its tile and all eight around fades out; walking
 * off meanwhile, it stays. No action button over the grass.
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
        game.seconds(30 * 60); // slow on purpose: about 2 tufts after 10 minutes here, 17 after 30
        Map<Cell, Integer> grass = game.grass();
        assertTrue(grass.size() > 10, "after half an hour there is grass: " + grass);
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

    /** Grass put on the map in Tiled: as far grown as its phase says (1 a tuft ... 4 a big bush; 1 without it). */
    private static final TestMap PLANTED = TestMap.of(
            "~~~~~~~~",
            "~abcde.~",
            "~......~",
            "~..P...~",
            "~~~~~~~~")
            .with('a', "GRASS", Map.of())
            .with('b', "GRASS", Map.of("phase", "2"))
            .with('c', "GRASS", Map.of("phase", "3"))
            .with('d', "GRASS", Map.of("phase", "4"))
            .with('e', "GRASS", Map.of("phase", "9"));

    @Test
    void theMapPutsGrassOnTiles() {
        GameDriver game = GameDriver.start(PLANTED.write("grass-planted"));
        assertEquals(Map.of(PLANTED.cell('a'), 0, PLANTED.cell('b'), 1, PLANTED.cell('c'), 2, PLANTED.cell('d'), 3,
                PLANTED.cell('e'), 3), game.grass(), "phases 1-4 (0-3 here); without one a tuft, too big a big bush");
        assertEquals("under", game.grassDrawn(PLANTED.cell('b')));
        assertEquals("over alpha=0.9", game.grassDrawn(PLANTED.cell('c')), "tall from the start");
        assertTrue(game.isAccessible(PLANTED.cell('a')), "it blocks nothing");
        game.seconds(31);
        assertEquals(1, game.grass().get(PLANTED.cell('a')), "it grows on");
        assertFalse(game.crashed());
    }

    @Test
    void withTheFlagOffTheMapsGrassIsntThere() {
        GameDriver.growingGrass(false);
        try {
            GameDriver game = GameDriver.start(PLANTED.write("grass-planted"));
            assertEquals(Map.of(), game.grass());
            assertFalse(game.crashed());
        } finally {
            GameDriver.growingGrass(true);
        }
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
        assertEquals(1, game.grassFadingOut(), "...and fades out...");
        game.seconds(0.15f);
        float size = game.fadingGrassHeights().get(0);
        assertTrue(size < 0.8f && size > 0.4f, "...getting smaller meanwhile: " + size);
        assertEquals("rabbit/idle_down", game.playerAnimation());
        game.seconds(0.25f);
        assertEquals(0, game.grassFadingOut(), "gone (0.35 s after the cut)");
        assertEquals("hint", game.toolButton(), "nothing left to cut: the hint again");
        assertFalse(game.crashed());
    }

    /** One swing cuts the grass on the rabbit's tile and on all eight around it; grass two tiles away stays. */
    @Test
    void oneSwingCutsAllAround() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell here = game.playerCell();
        for (int col = 1; col <= 5; col++) {
            for (int row = 1; row <= 3; row++) {
                game.sproutGrass(new Cell(col, row));
            }
        }
        game.seconds(0.5f);
        game.tapToolButton();
        game.seconds(0.9f);
        for (Cell cell : game.grass().keySet()) {
            assertEquals(2, Math.abs(cell.col() - here.col()), "only grass two tiles away is left: " + cell);
        }
        assertEquals(6, game.grass().size(), "the two columns two tiles away: " + game.grass());
        assertEquals(9, game.grassFadingOut(), "the rabbit's tile and the eight around it fade out");
        assertFalse(game.crashed());
    }

    /** Grass only next to the rabbit doesn't bring the scythe: the rabbit has to stand on grass. */
    @Test
    void grassOnlyNextToTheRabbitBringsNoScythe() {
        GameDriver game = GameDriver.start(YARD.write("grass-yard"));
        Cell here = game.playerCell();
        game.sproutGrass(new Cell(here.col() + 1, here.row() + 1));
        game.sproutGrass(new Cell(here.col() - 1, here.row()));
        game.seconds(0.5f);
        assertEquals("hint", game.toolButton(), "grass around, none under the rabbit: the hint");
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
