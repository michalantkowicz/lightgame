package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.LevelVariant;
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
 * transparent as in Tiled), animated water, a walls layer that blocks light, and the map's ambient light. Things the
 * starter level doesn't have are tried on changed copies of it ({@link LevelVariant}).
 */
class PackedMapTest {
    private static final String LEVEL = "maps/level1.tmx";
    private static final Cell DEEP_WATER = new Cell(0, 0);
    private static final Cell ISLAND = new Cell(8, 8);
    private static final String AS_IT_IS = "BL,TL,TR,BR";

    @Test
    void layersAreDrawnBottomFirst() {
        GameDriver game = GameDriver.start(LEVEL);
        List<String> island = game.cellDrawing(ISLAND);
        assertEquals(2, island.size(), "water below, grass on top: " + island);
        assertTrue(island.get(0).startsWith("water#"), island.toString());
        assertTrue(island.get(1).startsWith("grass_tiles_v2#"), island.toString());
        assertTrue(island.get(1).endsWith(AS_IT_IS + " alpha=1.0"), island.toString());

        List<String> water = game.cellDrawing(DEEP_WATER);
        assertEquals(1, water.size(), "only water out at sea: " + water);
    }

    @Test
    void waterIsAnimated() throws InterruptedException {
        GameDriver game = GameDriver.start(LEVEL);
        String before = game.cellDrawing(DEEP_WATER).get(0);
        // map tile animations run on the clock (4 frames of 500 ms): 1.1 s later it's 2 or 3 frames further
        Thread.sleep(1100);
        game.frames(1);
        assertNotEquals(before, game.cellDrawing(DEEP_WATER).get(0));
    }

    @Test
    void mirroredRotatedAndTransparentTilesLookAsInTiled() {
        LevelVariant variant = LevelVariant.ofLevel1();
        long grass = variant.gidAt("ground", ISLAND);
        Cell plain = new Cell(8, 9), mirrored = new Cell(9, 9), upsideDown = new Cell(10, 9), turned = new Cell(11, 9);
        String map = variant
                .addLayer("decor", Map.of(
                        plain, grass,
                        mirrored, grass | FLIP_HORIZONTALLY,
                        upsideDown, grass | FLIP_VERTICALLY,
                        turned, grass | FLIP_DIAGONALLY | FLIP_HORIZONTALLY)) // Tiled's "rotate clockwise" (Z)
                .addLayer("shadow", Map.of(plain, grass), 0.5f)
                .addLayer("top", Map.of(plain, grass))
                .write("flips.tmx");
        GameDriver game = GameDriver.start(map);

        assertEquals(AS_IT_IS, corners(game.cellDrawing(plain).get(2)));
        assertEquals("BR,TR,TL,BL", corners(game.cellDrawing(mirrored).get(2)), "left and right swapped");
        assertEquals("TL,BL,BR,TR", corners(game.cellDrawing(upsideDown).get(2)), "top and bottom swapped");
        assertEquals("BR,BL,TL,TR", corners(game.cellDrawing(turned).get(2)), "the picture's top-left is now top-right");
        assertTrue(game.cellDrawing(plain).get(3).endsWith("alpha=0.5"), "the shadow layer is half transparent");
        assertTrue(game.cellDrawing(plain).get(4).endsWith("alpha=1.0"), "the layer above it is opaque again");
    }

    @Test
    void theWallsLayerBlocksWalkingAndLight() {
        Cell above = new Cell(8, 8), wall = new Cell(8, 7), below = new Cell(8, 6), beside = new Cell(10, 8);
        LevelVariant variant = LevelVariant.ofLevel1();
        String map = variant.addLayer("walls", Map.of(wall, variant.gidAt("ground", wall))).write("walls.tmx");
        GameDriver game = GameDriver.start(map);

        assertFalse(game.isAccessible(wall));
        assertFalse(game.lightReaches(above, below), "the wall casts a shadow");
        assertTrue(game.lightReaches(above, beside), "only behind the wall");
        assertEquals(3, game.cellDrawing(wall).size(), "a visible walls layer is drawn too");
    }

    @Test
    void theCollisionLayerBlocksWalkingButNotLight() {
        GameDriver game = GameDriver.start(LEVEL);
        assertFalse(game.isAccessible(new Cell(15, 3)), "the pond is on the collision layer");
        assertTrue(game.lightReaches(new Cell(13, 4), new Cell(18, 4)), "light crosses the pond");
    }

    @Test
    void ambientLightComesFromTheMap() {
        assertEquals("0.00,0.00,0.00,1.00", GameDriver.start(LEVEL).ambientLight(), "level1 is daylight (ambientLight 1)");
        String dusk = LevelVariant.ofLevel1().mapProperty("ambientLight", "0.3").write("dusk.tmx");
        assertEquals("0.00,0.00,0.00,0.30", GameDriver.start(dusk).ambientLight());
        assertEquals("0.02,0.02,0.02,1.00", GameDriver.start().ambientLight(), "a map without it keeps the house's dim light");
    }

    @Test
    void mistakesInTheMapAreExplained() {
        String words = LevelVariant.ofLevel1().mapProperty("ambientLight", "bright").write("bad-ambient.tmx");
        RuntimeException ambient = assertThrows(RuntimeException.class, () -> GameDriver.start(words));
        assertTrue(message(ambient).contains("ambientLight must be a number"), message(ambient));

        String bigTiles = LevelVariant.ofLevel1().mapAttribute("tilewidth", "32").mapAttribute("tileheight", "32")
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
