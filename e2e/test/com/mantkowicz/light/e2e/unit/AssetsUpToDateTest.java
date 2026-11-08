package com.mantkowicz.light.e2e.unit;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.mantkowicz.light.resources.Sprite;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fails when a source of the generated assets changed but tools/assets/build_assets.py wasn't run afterwards (e.g. a
 * level saved in Tiled, pack.json edited, a sheet added to the Sprite enum). The tool records the SHA-1 of each source
 * in what it generates: the game copy of a map has it in its header comment, animations.json has the one of pack.json.
 * Needs neither the pack nor Python.
 */
class AssetsUpToDateTest {
    private static final String RUN_TOOL = "run: python3 tools/assets/build_assets.py";
    private static final Pattern MAP_SHA1 = Pattern.compile("source-sha1: ([0-9a-f]{40})");
    private static final Pattern PACK_JSON_SHA1 = Pattern.compile("\"packJsonSha1\": \"([0-9a-f]{40})\"");

    private final Path root = Path.of(System.getProperty("repo.root", "../.."));
    private final Path sourceMaps = root.resolve("assets-src/maps");
    private final Path gameMaps = root.resolve("android/assets/maps");

    @Test
    void gameMapsMatchTheirSources() throws IOException {
        List<String> stale = new ArrayList<>();
        for (Path source : tmxFiles(sourceMaps)) {
            Path game = gameMaps.resolve(source.getFileName());
            if (!Files.exists(game)) {
                stale.add(relative(source) + " has no game copy");
                continue;
            }
            Matcher recorded = MAP_SHA1.matcher(Files.readString(game));
            if (!recorded.find() || !recorded.group(1).equals(sha1(source))) {
                stale.add(relative(source) + " changed since its game copy was made");
            }
        }
        for (Path game : tmxFiles(gameMaps)) {
            if (!Files.exists(sourceMaps.resolve(game.getFileName()))) {
                stale.add(relative(game) + " has no source in assets-src/maps (the tool would remove it)");
            }
        }
        assertEquals(List.of(), stale, RUN_TOOL);
    }

    @Test
    void spritesMatchPackJson() throws IOException {
        Path packJson = root.resolve("tools/assets/pack.json");
        Matcher recorded = PACK_JSON_SHA1.matcher(Files.readString(root.resolve("android/assets/sprites/animations.json")));
        String found = recorded.find() ? recorded.group(1) : "nothing";
        assertEquals(sha1(packJson), found, "tools/assets/pack.json changed since the sprites were packed, " + RUN_TOOL);
    }

    @Test
    void spritesMatchTheSpriteEnum() throws IOException {
        JsonValue sheets = new JsonReader().parse(Files.readString(root.resolve("android/assets/sprites/animations.json")))
                .get("sheets");
        Set<String> packed = new TreeSet<>();
        for (JsonValue sheet = sheets.child; sheet != null; sheet = sheet.next) {
            packed.add(sheet.name);
        }
        Set<String> listed = new TreeSet<>();
        for (Sprite sprite : Sprite.values()) {
            listed.add(sprite.getSheet());
        }
        assertEquals(listed, packed, "the sheets in sprites.atlas aren't the ones in the Sprite enum, " + RUN_TOOL);
    }

    private static List<Path> tmxFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".tmx")).sorted().toList();
        }
    }

    private String relative(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    /**
     * Same as the tool's source_hash: SHA-1 of the text with \n line ends, so a Windows checkout gives the same hash.
     */
    private static String sha1(Path file) throws IOException {
        String text = Files.readString(file).replace("\r\n", "\n");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
