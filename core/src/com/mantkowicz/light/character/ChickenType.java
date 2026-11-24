package com.mantkowicz.light.character;

import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;

/**
 * The two chickens: a grown one and a chick. Both are restless (they move more than a cow) and peck at the ground
 * often, looking for something; the chick most of all. In Tiled: objectClass CHICKEN or CHICKEN_BABY.
 */
public enum ChickenType {
    CHICKEN(Sprite.CHICKEN, UiImage.CHICKEN_FACE, "chicken",
            "A chicken. It pecks at the ground, looking for something.",
            1.0f, 1.5f, 3, 3, 2),
    CHICKEN_BABY(Sprite.CHICKEN_BABY, UiImage.CHICK_FACE, "chick", "A chick. It never stands still for long.",
            0.7f, 0.8f, 2, 4, 2);

    private final Sprite sprite;
    /** Its face on the quest panel. */
    private final UiImage icon;
    private final String name;
    private final String description;
    private final float secondsPerTile, minPause, maxPause;
    private final int walkWeight, peckWeight;

    /**
     * @param secondsPerTile its step onto the next tile takes this long
     * @param minPause       it stands at least this long before it does something else...
     * @param maxPause       ...and at most this long
     * @param walkWeight     how often it then walks a step, compared with...
     * @param peckWeight     ...pecking at the ground and sitting down (always 1)
     */
    ChickenType(Sprite sprite, UiImage icon, String name, String description, float secondsPerTile, float minPause,
            float maxPause,
            int walkWeight, int peckWeight) {
        this.sprite = sprite;
        this.icon = icon;
        this.name = name;
        this.description = description;
        this.secondsPerTile = secondsPerTile;
        this.minPause = minPause;
        this.maxPause = maxPause;
        this.walkWeight = walkWeight;
        this.peckWeight = peckWeight;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public UiImage getIcon() {
        return icon;
    }

    /** What it is called, e.g. "chick". */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public float getSecondsPerTile() {
        return secondsPerTile;
    }

    public float getMinPause() {
        return minPause;
    }

    public float getMaxPause() {
        return maxPause;
    }

    public int getWalkWeight() {
        return walkWeight;
    }

    public int getPeckWeight() {
        return peckWeight;
    }
}
