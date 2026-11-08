package com.mantkowicz.light.resources;

/**
 * How the frames of one sprite sheet sit in the atlas (from sprites/animations.json). All frames of a sheet were cut to
 * one common box, so every animation of the sheet keeps the same anchor.
 *
 * @param frameWidth  size of a frame in the original sheet (art pixels)
 * @param frameHeight size of a frame in the original sheet (art pixels)
 * @param trimX       where the packed (trimmed) frame starts inside the original frame, from its left (art pixels)
 * @param trimY       where the packed (trimmed) frame starts inside the original frame, from its bottom (art pixels)
 */
public record SpriteSheet(int frameWidth, int frameHeight, int trimX, int trimY) {
}
