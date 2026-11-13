package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.LevelVariant;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.mantkowicz.light.e2e.LevelVariant.FLIP_DIAGONALLY;
import static com.mantkowicz.light.e2e.LevelVariant.FLIP_HORIZONTALLY;
import static com.mantkowicz.light.e2e.LevelVariant.FLIP_VERTICALLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the game reads Tiled maps made with tools/assets: tile layers drawn on top of each other (mirrored, rotated and
 * transparent as in Tiled), animated water, a walls layer that blocks light, a collision layer that doesn't, and the
 * map's ambient light. Runs on {@link TestMap}s; what can't be drawn as text is added on changed copies of them
 * ({@link LevelVariant}).
 */
class PackedMapTest {
    /** A wall (#) on the top row and a cell on the collision layer (x) on the bottom row. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~~~",
            "~..#....~",
            "~.......~",
            "~..x...P~",
            "~~~~~~~~~");
    private static final Cell SEA = new Cell(0, 0);
    private static final String AS_IT_IS = "BL,TL,TR,BR";

    @Test
    void layersAreDrawnBottomFirst() {
        Cell cell = new Cell(1, 2);
        String map = LevelVariant.of(MAP).addLayer("puddle", Map.of(cell, TestMap.WATER)).write("puddle.tmx");
        List<String> drawn = GameDriver.start(map).cellDrawing(cell);
        assertEquals(2, drawn.size(), "grass below, the puddle on top: " + drawn);
        assertTrue(drawn.get(0).startsWith("grass_tiles_v2#"), drawn.toString());
        assertTrue(drawn.get(0).endsWith(AS_IT_IS + " alpha=1.0"), drawn.toString());
        assertTrue(drawn.get(1).startsWith("water#"), drawn.toString());

        List<String> water = GameDriver.start(MAP.write("packed")).cellDrawing(SEA);
        assertEquals(1, water.size(), "only water out at sea: " + water);
    }

    @Test
    void waterIsAnimated() throws InterruptedException {
        GameDriver game = GameDriver.start(MAP.write("packed"));
        String before = game.cellDrawing(SEA).get(0);
        // map tile animations run on the clock (4 frames of 500 ms): 1.1 s later it's 2 or 3 frames further
        Thread.sleep(1100);
        game.frames(1);
        assertNotEquals(before, game.cellDrawing(SEA).get(0));
    }

    @Test
    void mirroredRotatedAndTransparentTilesLookAsInTiled() {
        Cell plain = new Cell(1, 2), mirrored = new Cell(2, 2), upsideDown = new Cell(4, 2), turned = new Cell(6, 2);
        long grass = TestMap.GRASS;
        String map = LevelVariant.of(MAP)
                .addLayer("decor", Map.of(
                        plain, grass,
                        mirrored, grass | FLIP_HORIZONTALLY,
                        upsideDown, grass | FLIP_VERTICALLY,
                        turned, grass | FLIP_DIAGONALLY | FLIP_HORIZONTALLY)) // Tiled's "rotate clockwise" (Z)
                .addLayer("shadow", Map.of(plain, grass), 0.5f)
                .addLayer("top", Map.of(plain, grass))
                .write("flips.tmx");
        GameDriver game = GameDriver.start(map);

        assertEquals(AS_IT_IS, corners(game.cellDrawing(plain).get(1)));
        assertEquals("BR,TR,TL,BL", corners(game.cellDrawing(mirrored).get(1)), "left and right swapped");
        assertEquals("TL,BL,BR,TR", corners(game.cellDrawing(upsideDown).get(1)), "top and bottom swapped");
        assertEquals("BR,BL,TL,TR", corners(game.cellDrawing(turned).get(1)),
                "the picture's top-left is now top-right");
        assertTrue(game.cellDrawing(plain).get(2).endsWith("alpha=0.5"), "the shadow layer is half transparent");
        assertTrue(game.cellDrawing(plain).get(3).endsWith("alpha=1.0"), "the layer above it is opaque again");
    }

    @Test
    void theWallsLayerBlocksWalkingAndLight() {
        Cell wall = new Cell(3, 3), left = new Cell(1, 3), right = new Cell(5, 3), below = new Cell(3, 2);
        GameDriver game = GameDriver.start(MAP.write("packed"));

        assertFalse(game.isAccessible(wall));
        assertFalse(game.lightReaches(left, right), "the wall casts a shadow");
        assertTrue(game.lightReaches(left, below), "only behind the wall");
        assertEquals(2, game.cellDrawing(wall).size(), "a visible walls layer is drawn too (over the ground)");
    }

    @Test
    void theCollisionLayerBlocksWalkingButNotLight() {
        Cell blocked = new Cell(3, 1), left = new Cell(1, 1), right = new Cell(5, 1);
        GameDriver game = GameDriver.start(MAP.write("packed"));
        assertFalse(game.isAccessible(blocked), "x is on the collision layer");
        assertTrue(game.lightReaches(left, right), "light crosses it");
    }

    @Test
    void ambientLightComesFromTheMap() {
        String daylight = LevelVariant.of(MAP).mapProperty("ambientLight", "1").write("daylight.tmx");
        assertEquals("0.00,0.00,0.00,1.00", GameDriver.start(daylight).ambientLight(), "ambientLight 1 is daylight");
        String dusk = LevelVariant.of(MAP).mapProperty("ambientLight", "0.3").write("dusk.tmx");
        assertEquals("0.00,0.00,0.00,0.30", GameDriver.start(dusk).ambientLight());
        assertEquals("0.02,0.02,0.02,1.00", GameDriver.start(MAP.write("packed")).ambientLight(),
                "a map without it keeps the house's dim light");
    }

    @Test
    void mistakesInTheMapAreExplained() {
        String words = LevelVariant.of(MAP).mapProperty("ambientLight", "bright").write("bad-ambient.tmx");
        RuntimeException ambient = assertThrows(RuntimeException.class, () -> GameDriver.start(words));
        assertTrue(message(ambient).contains("ambientLight must be a number"), message(ambient));

        String bigTiles = LevelVariant.of(MAP).mapAttribute("tilewidth", "32").mapAttribute("tileheight", "32")
                .write("big-tiles.tmx");
        RuntimeException size = assertThrows(RuntimeException.class, () -> GameDriver.start(bigTiles));
        assertTrue(message(size).contains("made for 16 px"), message(size));
    }

    private static String corners(String drawn) {
        return drawn.split(" ")[1];
    }

    /** The innermost message: the game's own, not the framework's wrapping. */
    private static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() + " / " + error.getMessage();
    }
}
