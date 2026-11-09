package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.Golden;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path finding around walls and blocked cells. Only path lengths are recorded: where several shortest paths exist, any of
 * them is correct. The structure of every path (neighbour steps, accessible tiles, ends at target) is asserted.
 */
class PathFindingTest {
    /** A walled room (c inside) open at its bottom, and one blocked cell x. */
    private static final TestMap MAP = TestMap.of(
            "~~~~~~~~~~",
            "~a.....b.~",
            "~.####...~",
            "~.#c.#...~",
            "~.#..#.x.~",
            "~.##.#...~",
            "~dP....e.~",
            "~~~~~~~~~~");
    private static final Cell A = MAP.cell('a'), B = MAP.cell('b'), C = MAP.cell('c'), D = MAP.cell('d'),
            E = MAP.cell('e'), BLOCKED = MAP.cell('x');
    private static final Cell[][] ROUTES = {
            {A, B},
            {A, C},                 // into the room through its opening
            {D, C},
            {B, E},
            {C, E},                 // out of the room
            {A, E},
            {E, A},
            {A, A},                 // already there
            {E, BLOCKED},           // target blocked
            {BLOCKED, E},           // start blocked
    };

    private static GameDriver game;

    @BeforeAll
    static void startGame() {
        game = GameDriver.start(MAP.write("path-finding"));
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
