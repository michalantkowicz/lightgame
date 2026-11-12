package com.mantkowicz.light.e2e.unit;

import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.resources.UiFont;
import com.mantkowicz.light.resources.UiImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The UI pack in the game: every piece and font the UiImage and UiFont enums list loads, nine-patches stretch,
 * animations have their frames, fonts measure text. Whatever the enums list, so adding a piece needs no change here.
 */
class UiAssetsTest {
    private static final String SIZE = "[A-Z0-9_]+ [1-9][0-9]*x[1-9][0-9]*";

    @Test
    void everyListedPieceAndFontLoads() {
        GameDriver game = GameDriver.start();
        List<String> ui = game.describeUi();
        assertEquals(UiImage.values().length + UiFont.values().length, ui.size(), ui.toString());
        for (String line : ui.subList(0, UiImage.values().length)) {
            assertTrue(line.matches(SIZE + "(, nine-patch, borders [0-9]+x[0-9]+)?"
                    + "(, animation of [1-9][0-9]* frames, [0-9.]+ s)?"), line);
        }
        for (String line : ui.subList(UiImage.values().length, ui.size())) {
            assertTrue(line.matches("[A-Z0-9_]+ line height [1-9][0-9]*, 'HELLO' is [1-9][0-9]* px wide"), line);
        }
    }

    /** The two kinds of special pieces, as listed now: if they are removed from the enum, pick others here. */
    @Test
    void ninePatchesAndAnimationsWork() {
        List<String> ui = GameDriver.start().describeUi();
        assertTrue(ui.contains("DIALOG_BOX 30x28, nine-patch, borders 15x12"), ui.toString());
        assertTrue(ui.contains("CONTINUE_INDICATOR 7x6, animation of 7 frames, 0.7 s"), ui.toString());
        assertTrue(ui.contains("PIXEL_7X7 line height 24, 'HELLO' is 87 px wide"), ui.toString());
    }
}
