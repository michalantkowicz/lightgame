package com.mantkowicz.light.resources;

/**
 * How the frames of one sprite sheet sit in the atlas (from sprites/animations.json). All frames of a sheet were cut to
 * one common box, so every animation of the sheet keeps the same anchor.
 *
 * @param frameWidth  size of a frame in the original sheet (art pixels)
 * @param frameHeight size of a frame in the original sheet (art pixels)
 * @param trimX       where the packed (trimmed) frame starts inside the original frame, from its left (art pixels)
 * @param trimY       where the packed (trimmed) frame starts inside the original frame, from its bottom (art pixels)
 * @param anchorX     the frame's point drawn on the tile's middle, from the frame's left (art pixels): the frame's middle
 *                    unless pack.json gives the sheet an anchor (a character drawn away from the middle, e.g. the cow)
 * @param anchorY     the same point, from the frame's bottom (art pixels)
 */
public record SpriteSheet(int frameWidth, int frameHeight, int trimX, int trimY, float anchorX, float anchorY) {
}
