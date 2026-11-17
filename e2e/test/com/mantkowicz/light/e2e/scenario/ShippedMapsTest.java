package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks every map the game ships (android/assets), whatever its layout: it loads, the rabbit stands on a free cell (on
 * a map it starts on; one behind a door has no PLAYER: the rabbit comes out of the door), every chest can be used: a
 * free cell next to it that the rabbit can walk to, and every door leads to another shipped map with a door of the same
 * name and can be walked to. No cells or goldens here, so editing a level only fails when the level itself is broken.
 */
class ShippedMapsTest {
    @Test
    void theGameStartsOnLevel1() {
        assertEquals("maps/level1.tmx", GameDriver.defaultMap());
    }

    @ParameterizedTest
    @MethodSource("shippedMaps")
    void loadsWithTheRabbitOnAFreeCell(String map) {
        GameDriver game = GameDriver.start(map);
        assertFalse(game.crashed(), map);
        if (hasPlayer(game)) {
            assertTrue(game.isAccessible(game.playerCell()), map + ": the rabbit starts on " + game.playerCell());
        } else {
            assertFalse(game.doors().isEmpty(), map + ": no PLAYER and no door, the rabbit can't get there");
        }
    }

    @ParameterizedTest
    @MethodSource("shippedMaps")
    void everyChestCanBeReached(String map) {
        GameDriver game = GameDriver.start(map);
        Cell start = start(game);
        for (Map.Entry<Cell, List<String>> entry : game.objectsByCell().entrySet()) {
            if (!entry.getValue().contains("CHEST")) {
                continue;
            }
            Cell chest = entry.getKey();
            boolean reachable = game.usedFromNeighbours(chest).stream()
                    .anyMatch(
                            cell -> cell.equals(start) || game.isAccessible(cell) && !game.path(start, cell).isEmpty());
            assertTrue(reachable, map + ": the rabbit can't get next to the chest on " + chest);
        }
    }

    @ParameterizedTest
    @MethodSource("shippedMaps")
    void everyDoorLeadsToItsPairAndCanBeReached(String map) throws IOException {
        GameDriver game = GameDriver.start(map);
        Cell start = start(game);
        List<String> shipped = shippedMaps().toList();
        for (Map.Entry<Cell, String> entry : game.doors().entrySet()) {
            Cell door = entry.getKey();
            String name = entry.getValue().split(" -> ")[0];
            String target = entry.getValue().split(" -> ")[1];
            assertTrue(shipped.contains(target), map + ": the door on " + door + " leads to " + target
                    + ", which isn't shipped");
            assertNotEquals(map, target, map + ": the door on " + door + " leads to its own map");
            assertTrue(GameDriver.start(target).doors().values().stream()
                    .anyMatch(other -> other.startsWith(name + " -> ")),
                    target + " has no door named " + name + " to come out of");
            assertTrue(game.isAccessible(door), map + ": the door on " + door + " is blocked");
            assertTrue(door.equals(start) || !game.path(start, door).isEmpty(),
                    map + ": the rabbit can't walk to the door on " + door);
        }
    }

    /** The rabbit is there from the start on a map it starts on; one behind a door has none. */
    private static boolean hasPlayer(GameDriver game) {
        return game.objectsByCell().values().stream().anyMatch(objects -> objects.contains("PLAYER"));
    }

    /** Where the rabbit starts: where the map puts it, or behind a door the first door. */
    private static Cell start(GameDriver game) {
        return hasPlayer(game) ? game.playerCell() : game.doors().keySet().iterator().next();
    }

    /** Every .tmx in android/assets (the tests run there), e.g. "maps/level1.tmx". */
    static Stream<String> shippedMaps() throws IOException {
        Path assets = Path.of("").toAbsolutePath();
        try (Stream<Path> files = Files.walk(assets)) {
            return files.filter(file -> file.toString().endsWith(".tmx"))
                    .map(file -> assets.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList()
                    .stream();
        }
    }
}
