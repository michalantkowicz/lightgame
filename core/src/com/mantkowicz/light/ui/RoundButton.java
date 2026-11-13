package com.mantkowicz.light.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * The pack's round button (brown, or another colour) with a picture on it and no text. Pressed, it sinks (the pack's pressed piece) and the
 * picture goes down with it. Add listeners for what it does.
 */
public class RoundButton extends Group {
    /** Screen pixels per pack pixel by default: the button and the picture on it. */
    private static final int SCALE = 6, ICON_SCALE = 4;
    /** The pack's round button is its round top plus a side under it; pressed, the side is hidden. */
    private static final int SIDE = 3;

    private final TextureRegionDrawable up, down;
    private final int scale;
    private final Image background = new Image(), icon;
    private final ClickListener pressed = new ClickListener();

    /** The brown one at the default size (the HUD's hint). */
    public RoundButton(ResourcesService resources, TextureRegion picture) {
        this(resources, picture, UiImage.ROUND_BUTTON, UiImage.ROUND_BUTTON_DOWN, SCALE, ICON_SCALE);
    }

    /**
     * @param upImage   the button, e.g. {@link UiImage#ROUND_BUTTON}
     * @param downImage the same button pressed
     * @param scale     screen pixels per pack pixel for the button, and for the picture on it ({@code iconScale})
     */
    public RoundButton(ResourcesService resources, TextureRegion picture, UiImage upImage, UiImage downImage,
            int scale, int iconScale) {
        this.scale = scale;
        up = new TextureRegionDrawable(resources.getUiImage(upImage));
        down = new TextureRegionDrawable(resources.getUiImage(downImage));
        setSize(up.getRegion().getRegionWidth() * scale, up.getRegion().getRegionHeight() * scale);

        addActor(background);
        icon = new Image(picture);
        icon.setSize(picture.getRegionWidth() * iconScale, picture.getRegionHeight() * iconScale);
        addActor(icon);
        showPressed(false);

        setTouchable(Touchable.enabled);
        background.setTouchable(Touchable.disabled);
        icon.setTouchable(Touchable.disabled);
        addListener(pressed);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        showPressed(pressed.isVisualPressed());
    }

    /** Pressed, the button is lower (its side hidden) and the picture goes down with it. */
    private void showPressed(boolean isPressed) {
        TextureRegionDrawable drawable = isPressed ? down : up;
        background.setDrawable(drawable);
        background.setSize(drawable.getRegion().getRegionWidth() * scale,
                drawable.getRegion().getRegionHeight() * scale);
        float top = background.getHeight() - (up.getRegion().getRegionHeight() - SIDE) * scale;
        float faceMiddle = top + (getHeight() - SIDE * scale) / 2f;
        icon.setPosition(Math.round((getWidth() - icon.getWidth()) / 2f),
                Math.round(faceMiddle - icon.getHeight() / 2f));
    }
}
