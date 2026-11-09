package com.mantkowicz.light.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A small map made for one test, drawn as text: one character per cell, the first line is the top row. Tests use it
 * instead of the shipped levels, so editing a level never breaks a feature test.
 * <pre>{@code
 * TestMap map = TestMap.of(
 *         "~~~~~~~",
 *         "~..C..~",
 *         "~..P.a~",
 *         "~~~~~~~");
 * GameDriver game = GameDriver.start(map.write("collect"));
 * game.click(map.cell('a'));
 * }</pre>
 * Cells:
 * <ul>
 * <li>{@code .} grass, walkable;</li>
 * <li>{@code ~} water: drawn, nobody walks on it (collision layer);</li>
 * <li>{@code x} blocked grass: drawn, nobody walks on it, light passes (collision layer);</li>
 * <li>{@code #} wall: drawn, blocks walking and light (walls layer);</li>
 * <li>a space: no tile at all;</li>
 * <li>{@code P} player, {@code C} chest, {@code M} cow, {@code D} dog, {@code *} torch: that object on grass;</li>
 * <li>{@code a}…{@code z}: grass marked for the test, found with {@link #cell(char)}.</li>
 * </ul>
 * Cells are counted like the game does: column from the left, row from the bottom. It is written as a packed map (like
 * tools/assets makes) drawing the game's own tiles atlas, so it can only use tiles a shipped level uses: plain water and
 * plain grass. Written to e2e/build/test-maps.
 */
public final class TestMap {
    private static final Path ROOT = Path.of(System.getProperty("repo.root", "../..")).toAbsolutePath().normalize();
    private static final Path ATLAS = ROOT.resolve("android/assets/maps/tiles.atlas");
    private static final Path OUTPUT = ROOT.resolve("e2e/build/test-maps");
    private static final int TILE = 16;
    /** Tiles of the atlas: the first water tile, and plain grass (id 12 of grass_tiles_v2). */
    private static final int WATER = 1, GRASS = 5 + 12;
    private static final Map<Character, String> OBJECTS = Map.of(
            'P', "PLAYER", 'C', "CHEST", 'M', "COW", 'D', "DOG", '*', "LIGHT");

    private final String[] lines;
    private final int width;
    private final int height;

    private TestMap(String[] lines) {
        this.lines = lines;
        this.width = lines[0].length();
        this.height = lines.length;
        for (String line : lines) {
            if (line.length() != width) {
                throw new IllegalArgumentException("Every line needs " + width + " cells: '" + line + "'");
            }
            for (char c : line.toCharArray()) {
                if (".~x# ".indexOf(c) < 0 && !OBJECTS.containsKey(c) && !(c >= 'a' && c <= 'z')) {
                    throw new IllegalArgumentException("Unknown cell '" + c + "' in '" + line + "'");
                }
            }
        }
    }

    public static TestMap of(String... lines) {
        return new TestMap(lines);
    }

    /**
     * The only cell showing this character, e.g. {@code cell('a')} or {@code cell('C')}.
     */
    public Cell cell(char c) {
        Cell found = null;
        for (int line = 0; line < height; line++) {
            for (int col = 0; col < width; col++) {
                if (lines[line].charAt(col) == c) {
                    if (found != null) {
                        throw new IllegalArgumentException("'" + c + "' is on more than one cell");
                    }
                    found = new Cell(col, height - 1 - line);
                }
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("No '" + c + "' on the map");
        }
        return found;
    }

    /**
     * Writes the map as e2e/build/test-maps/{name}.tmx and returns its path, for {@link GameDriver#start(String)}.
     */
    public String write(String name) {
        StringBuilder tmx = new StringBuilder();
        tmx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        tmx.append(String.format("<map version=\"1.10\" orientation=\"orthogonal\" renderorder=\"right-down\" width=\"%d\" "
                + "height=\"%d\" tilewidth=\"%d\" tileheight=\"%d\" infinite=\"0\">\n", width, height, TILE, TILE));
        tmx.append(" <properties>\n");
        tmx.append(String.format("  <property name=\"atlas\" value=\"%s\"/>\n",
                OUTPUT.relativize(ATLAS).toString().replace('\\', '/')));
        tmx.append(" </properties>\n");
        tmx.append(" <tileset firstgid=\"1\" name=\"water\" tilewidth=\"16\" tileheight=\"16\" tilecount=\"4\" columns=\"4\">\n");
        tmx.append("  <image source=\"water.png\" width=\"64\" height=\"16\"/>\n");
        tmx.append(" </tileset>\n");
        tmx.append(" <tileset firstgid=\"5\" name=\"grass_tiles_v2\" tilewidth=\"16\" tileheight=\"16\" tilecount=\"77\" columns=\"11\">\n");
        tmx.append("  <image source=\"grass_tiles_v2.png\" width=\"176\" height=\"112\"/>\n");
        tmx.append(" </tileset>\n");
        layer(tmx, 1, "ground", c -> c == ' ' ? 0 : c == '~' ? WATER : GRASS);
        layer(tmx, 2, "collision", c -> c == '~' || c == 'x' ? GRASS : 0);
        layer(tmx, 3, "walls", c -> c == '#' ? GRASS : 0);
        objects(tmx);
        tmx.append("</map>\n");
        try {
            Files.createDirectories(OUTPUT);
            Path file = OUTPUT.resolve(name + ".tmx");
            Files.writeString(file, tmx);
            return file.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Can't write " + name, e);
        }
    }

    private interface Gid {
        int of(char c);
    }

    private void layer(StringBuilder tmx, int id, String name, Gid gid) {
        tmx.append(String.format(" <layer id=\"%d\" name=\"%s\" width=\"%d\" height=\"%d\">\n", id, name, width, height));
        tmx.append("  <data encoding=\"csv\">\n");
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            StringBuilder row = new StringBuilder();
            for (char c : line.toCharArray()) {
                row.append(row.length() == 0 ? "" : ",").append(gid.of(c));
            }
            rows.add(row.toString());
        }
        tmx.append(String.join(",\n", rows)).append("\n  </data>\n </layer>\n");
    }

    /** Each object is a small ellipse in the middle of its cell (Tiled counts pixels from the top). */
    private void objects(StringBuilder tmx) {
        tmx.append(" <objectgroup id=\"4\" name=\"objects\">\n");
        int id = 1;
        for (int line = 0; line < height; line++) {
            for (int col = 0; col < width; col++) {
                String objectClass = OBJECTS.get(lines[line].charAt(col));
                if (objectClass != null) {
                    tmx.append(String.format("  <object id=\"%d\" x=\"%d\" y=\"%d\" width=\"8\" height=\"8\">\n", id++,
                            col * TILE + 4, line * TILE + 4));
                    tmx.append("   <ellipse/>\n");
                    tmx.append(String.format("   <properties><property name=\"objectClass\" value=\"%s\"/></properties>\n",
                            objectClass));
                    tmx.append("  </object>\n");
                }
            }
        }
        tmx.append(" </objectgroup>\n");
    }
}
