package com.mantkowicz.light.e2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compares a test's recorded text with e2e/golden/&lt;name&gt;.txt. Goldens describe today's behaviour; a mismatch
 * means behaviour changed. Re-record only on purpose: ./gradlew e2e:test -Dgolden.update=true
 */
public final class Golden {
    private Golden() {
    }

    public static void assertMatches(String name, String actual) {
        Path golden = Path.of(System.getProperty("golden.dir"), name + ".txt");
        if (Boolean.parseBoolean(System.getProperty("golden.update"))) {
            write(golden, actual);
            return;
        }
        if (!Files.exists(golden)) {
            fail("Missing golden file " + golden + ". Record it with: ./gradlew e2e:test -Dgolden.update=true");
        }
        String expected = read(golden);
        if (!expected.equals(actual)) {
            Path actualFile = Path.of(System.getProperty("golden.actual.dir"), name + ".txt");
            write(actualFile, actual);
            assertEquals(expected, actual, "Behaviour differs from " + golden + " (actual output: " + actualFile + ")");
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
