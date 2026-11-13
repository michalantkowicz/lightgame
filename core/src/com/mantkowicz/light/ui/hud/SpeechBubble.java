package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import static com.mantkowicz.light.resources.ResourcesService.UI_SCALE;

/**
 * A short line of text, with or without a picture after it ("find" and a carrot), in a box with a small pointer at the
 * middle of its bottom, e.g. over the HUD's face. It goes
 * away by itself after a few seconds; its parent places it (the pointer's tip is its bottom middle).
 * <p>
 * Drawn from the pack's dialog box turned so its pointer is at the bottom ({@link UiImage#DIALOG_BOX_DOWN}): the box
 * (without the pointer) stretches around the text, its edges taken from a plain row and column, and the pointer is
 * drawn on its own at the middle of the box's bottom, so it stays small however long the text is.
 */
public class SpeechBubble extends Group {
    /** How long the bubble stays, and how long it then takes to fade away. */
    static final float SECONDS = 2.5f, FADE_SECONDS = 0.3f;
    /**
     * The turned dialog box (28x30, pack pixels, from its top left): the box is 28x26 with borders of 4 (left, top,
     * bottom) and 5 (right, where its side shows); a plain column and row away from the corners and the pointer; the
     * pointer, 12x7, overlapping the box's last 3 rows.
     */
    private static final int BOX_WIDTH = 28, BOX_HEIGHT = 26, LEFT = 4, RIGHT = 5, TOP = 4, BOTTOM = 4, PLAIN_X = 5,
            PLAIN_Y = 10, POINTER_X = 9, POINTER_Y = 23, POINTER_WIDTH = 12, POINTER_HEIGHT = 7;
    /** Between the text and the box's outer edge, in pack pixels. */
    private static final int PAD_X = 7, PAD_Y = 6;
    /** Between the text and the picture after it, in pack pixels. */
    private static final int PICTURE_GAP = 3;

    private final Label label;

    public SpeechBubble(ResourcesService resources, String text) {
        this(resources, text, null);
    }

    /**
     * @param picture shown after the text at the UI's scale (a 16 px item is 48 px), or null for text only
     */
    public SpeechBubble(ResourcesService resources, String text, TextureRegion picture) {
        setTouchable(Touchable.disabled);

        TextureRegion box = resources.getUiImage(UiImage.DIALOG_BOX_DOWN);
        int right = BOX_WIDTH - RIGHT, bottom = BOX_HEIGHT - BOTTOM;
        NinePatch patch = new NinePatch(
                part(box, 0, 0, LEFT, TOP), part(box, PLAIN_X, 0, 1, TOP), part(box, right, 0, RIGHT, TOP),
                part(box, 0, PLAIN_Y, LEFT, 1), part(box, PLAIN_X, PLAIN_Y, 1, 1), part(box, right, PLAIN_Y, RIGHT, 1),
                part(box, 0, bottom, LEFT, BOTTOM), part(box, PLAIN_X, bottom, 1, BOTTOM),
                part(box, right, bottom, RIGHT, BOTTOM));
        patch.scale(UI_SCALE, UI_SCALE);

        label = new Label(text, new Label.LabelStyle(resources.getFont(), UiStyle.TEXT));
        float pictureWidth = picture == null ? 0 : (PICTURE_GAP + picture.getRegionWidth()) * UI_SCALE;
        float pictureHeight = picture == null ? 0 : picture.getRegionHeight() * UI_SCALE;
        float contentWidth = label.getPrefWidth() + pictureWidth;
        float width = snap(contentWidth + (PAD_X + PAD_X + RIGHT - LEFT) * UI_SCALE);
        float height = snap(Math.max(label.getPrefHeight(), pictureHeight) + 2 * PAD_Y * UI_SCALE);
        float below = (POINTER_Y + POINTER_HEIGHT - BOX_HEIGHT) * UI_SCALE; // how far the pointer reaches under the box

        Image body = new Image(new NinePatchDrawable(patch));
        body.setBounds(0, below, width, height);
        addActor(body);
        Image pointer = new Image(part(box, POINTER_X, POINTER_Y, POINTER_WIDTH, POINTER_HEIGHT));
        pointer.setBounds(snap((width - POINTER_WIDTH * UI_SCALE) / 2f), 0, POINTER_WIDTH * UI_SCALE,
                POINTER_HEIGHT * UI_SCALE);
        addActor(pointer);
        // the text in the middle of the box's face (its right border is wider: the box's side)
        float face = width - (RIGHT - LEFT) * UI_SCALE;
        label.setPosition(Math.round((face - contentWidth) / 2f),
                below + Math.round((height - label.getPrefHeight()) / 2f));
        addActor(label);
        if (picture != null) {
            Image shown = new Image(picture);
            shown.setBounds(label.getX() + contentWidth - picture.getRegionWidth() * UI_SCALE,
                    below + Math.round((height - pictureHeight) / 2f), picture.getRegionWidth() * UI_SCALE,
                    pictureHeight);
            addActor(shown);
        }
        setSize(width, below + height);

        addAction(sequence(delay(SECONDS), fadeOut(FADE_SECONDS), Actions.removeActor()));
    }

    private static TextureRegion part(TextureRegion region, int x, int y, int width, int height) {
        return new TextureRegion(region, x, y, width, height);
    }

    /** Rounded down to whole pack pixels, so the bubble's pixels line up with the text's. */
    private static float snap(float value) {
        return (float) Math.floor(value / UI_SCALE) * UI_SCALE;
    }

    public String getText() {
        return label.getText().toString();
    }
}
