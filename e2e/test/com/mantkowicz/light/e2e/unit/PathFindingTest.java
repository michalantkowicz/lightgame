package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path finding on the real board. Only path lengths are recorded: where several shortest paths exist, any of
 * them is correct. The structure of every path (neighbour steps, accessible tiles, ends at target) is asserted.
 */
class PathFindingTest {
    private static final Cell[][] ROUTES = {
            {new Cell(10, 22), new Cell(13, 24)},
            {new Cell(10, 22), new Cell(7, 23)},
            {new Cell(10, 22), new Cell(5, 27)},
            {new Cell(10, 22), new Cell(14, 28)},
            {new Cell(7, 22), new Cell(14, 28)},
            {new Cell(9, 26), new Cell(13, 24)},
            {new Cell(13, 28), new Cell(6, 26)},
            {new Cell(10, 22), new Cell(10, 22)},   // already there
            {new Cell(10, 22), new Cell(12, 24)},   // target blocked by furniture
            {new Cell(12, 24), new Cell(10, 22)},   // start blocked by furniture
    };

    private static GameDriver game;

    @BeforeAll
    static void startGame() {
        game = GameDriver.start();
    }

    @Test
    void pathLengths() {
        StringBuilder lengths = new StringBuilder();
        for (Cell[] route : ROUTES) {
            lengths.append(route[0]).append(" -> ").append(route[1]).append(": ")
                    .append(game.path(route[0], route[1]).size()).append('\n');
        }
        Golden.assertMatches("path-lengths", lengths.toString());
    }

    @Test
    void everyPathIsAWalkableChainOfNeighbours() {
        for (Cell[] route : ROUTES) {
            List<Cell> path = game.path(route[0], route[1]);
            if (path.isEmpty()) {
                continue;
            }
            assertEquals(route[1], path.get(path.size() - 1), "path must end at the target");
            assertTrue(!path.contains(route[0]), "path must not contain the start");
            Cell previous = route[0];
            for (Cell cell : path) {
                assertTrue(game.neighbours(previous).contains(cell), cell + " is not a neighbour of " + previous);
                assertTrue(game.isAccessible(cell), cell + " is not accessible");
                previous = cell;
            }
        }
    }
}
