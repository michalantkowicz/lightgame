package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.actor.Stamina;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * A character's {@link Stamina} as a small face and a row of segments from the pack (the bars on its all-in-one
 * sheet): green segments for what is left, brown ones for what is used, filling from the left. The face says how it
 * feels: happy with more than half left, neutral (yellow) at half or less, sad (red) at a quarter or less. It always
 * shows the stamina's current value.
 */
public class StaminaBar extends Actor {
    /**
     * Segments in the bar, pack pixels between the starts of two segments (a segment is 4 wide, then a gap), and pack
     * pixels per screen pixel: 14 segments 4x12 drawn 2x make a bar 138 px wide, 3/4 of the HUD's face.
     */
    static final int SEGMENTS = 14, PITCH = 5, SCALE = 2;
    /** Up to these values the face is neutral, sad. */
    static final float NEUTRAL = 0.5f, SAD = 0.25f;
    /** Between the face and the first segment, in screen pixels. */
    private static final float FACE_GAP = 6;

    private final ResourcesService resources;
    private final Stamina stamina;
    private final TextureRegion full, empty;
    private final float faceWidth;

    public StaminaBar(ResourcesService resources, Stamina stamina) {
        this.resources = resources;
        this.stamina = stamina;
        this.full = resources.getUiImage(UiImage.BAR_FULL);
        this.empty = resources.getUiImage(UiImage.BAR_EMPTY);
        TextureRegion face = resources.getUiImage(UiImage.MOOD_HAPPY);
        faceWidth = face.getRegionWidth() * SCALE;
        setSize(faceWidth + FACE_GAP + ((SEGMENTS - 1) * PITCH + empty.getRegionWidth()) * SCALE,
                Math.max(face.getRegionHeight(), empty.getRegionHeight()) * SCALE);
    }

    /** How many segments are green: any strength left shows at least one, only none shows none. */
    public int getFullSegments() {
        return (int) Math.ceil(stamina.get() * SEGMENTS - 0.0001f);
    }

    public int getSegments() {
        return SEGMENTS;
    }

    /** The face for how much is left. */
    public UiImage getMood() {
        float value = stamina.get();
        return value <= SAD ? UiImage.MOOD_SAD : value <= NEUTRAL ? UiImage.MOOD_NEUTRAL : UiImage.MOOD_HAPPY;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1, 1, 1, parentAlpha * getColor().a);
        TextureRegion face = resources.getUiImage(getMood());
        float faceHeight = face.getRegionHeight() * SCALE;
        batch.draw(face, getX(), getY() + Math.round((getHeight() - faceHeight) / 2f), face.getRegionWidth() * SCALE, faceHeight);

        int fullSegments = getFullSegments();
        float width = empty.getRegionWidth() * SCALE, height = empty.getRegionHeight() * SCALE;
        float x = getX() + faceWidth + FACE_GAP, y = getY() + Math.round((getHeight() - height) / 2f);
        for (int i = 0; i < SEGMENTS; i++) {
            batch.draw(i < fullSegments ? full : empty, x + i * PITCH * SCALE, y, width, height);
        }
    }
}
