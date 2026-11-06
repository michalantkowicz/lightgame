package com.mantkowicz.light.e2e;

import com.badlogic.gdx.math.Vector2;

import java.util.Locale;

/** Builds the line-based, rounded text that golden files contain. */
final class Snapshot {
    private final StringBuilder text = new StringBuilder();

    Snapshot line(String format, Object... args) {
        text.append(String.format(Locale.ROOT, format, args)).append('\n');
        return this;
    }

    static String number(float value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return formatted.equals("-0.0") ? "0.0" : formatted;
    }

    static String point(float x, float y) {
        return "(" + number(x) + "," + number(y) + ")";
    }

    static String point(Vector2 point) {
        return point(point.x, point.y);
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
