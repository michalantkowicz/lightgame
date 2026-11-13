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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks every map the game ships (android/assets), whatever its layout: it loads, the rabbit stands on a free cell, and
 * every chest can be used: a free cell next to it that the rabbit can walk to. No cells or goldens here, so editing a
 * level only fails when the level itself is broken.
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
        assertTrue(game.isAccessible(game.playerCell()), map + ": the rabbit starts on " + game.playerCell());
    }

    @ParameterizedTest
    @MethodSource("shippedMaps")
    void everyChestCanBeReached(String map) {
        GameDriver game = GameDriver.start(map);
        Cell start = game.playerCell();
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
