package com.mantkowicz.light.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.mantkowicz.light.resources.Palette;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

import static com.mantkowicz.light.resources.ResourcesService.UI_SCALE;

/**
 * The game's UI look, from the Sprout Lands UI pack: pieces drawn at {@link ResourcesService#UI_SCALE} (every pack
 * pixel the same size on screen, also the font's), dark brown text on the pack's light panels and buttons.
 */
public final class UiStyle {
    /** The dark brown of the pack's outlines, for text on panels and buttons. */
    public static final Color TEXT = Palette.TEXT;
    private static final float BUTTON_WIDTH = 220, BUTTON_HEIGHT = 81;

    private UiStyle() {
    }

    /** A piece that keeps its size, scaled up. */
    public static Drawable image(ResourcesService resources, UiImage image) {
        TextureRegion region = resources.getUiImage(image);
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        drawable.setMinSize(region.getRegionWidth() * UI_SCALE, region.getRegionHeight() * UI_SCALE);
        return drawable;
    }

    /** A piece that stretches (a nine-patch), its borders scaled up. */
    public static Drawable stretching(ResourcesService resources, UiImage image) {
        NinePatch patch = new NinePatch(resources.getUiNinePatch(image));
        patch.scale(UI_SCALE, UI_SCALE);
        return new NinePatchDrawable(patch);
    }

    /** An icon as an image actor, at UI scale. */
    public static Image icon(ResourcesService resources, UiImage image) {
        Image icon = new Image(image(resources, image), Scaling.fit);
        icon.setSize(icon.getPrefWidth(), icon.getPrefHeight());
        return icon;
    }

    /** A button with a word on it (COLLECT, OK, CANCEL), all of the same size. */
    public static TextButton textButton(ResourcesService resources, String text) {
        TextButtonStyle style = new TextButtonStyle();
        style.up = stretching(resources, UiImage.BUTTON);
        style.down = stretching(resources, UiImage.BUTTON_DOWN);
        style.font = resources.getFont();
        style.fontColor = TEXT;
        TextButton button = new TextButton(text, style);
        button.setSize(Math.max(BUTTON_WIDTH, button.getPrefWidth()), BUTTON_HEIGHT);
        return button;
    }
}
