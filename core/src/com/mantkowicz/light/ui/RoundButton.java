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
 * The pack's round brown button with a picture on it and no text. Pressed, it sinks (the pack's pressed piece) and the
 * picture goes down with it. Add listeners for what it does.
 */
public class RoundButton extends Group {
    /** Pack pixels per screen pixel: the button and the picture on it. */
    private static final int SCALE = 6, ICON_SCALE = 4;
    /** The pack's round button is its round top plus a side under it; pressed, the side is hidden. */
    private static final int SIDE = 3;

    private final TextureRegionDrawable up, down;
    private final Image background = new Image(), icon;
    private final ClickListener pressed = new ClickListener();

    public RoundButton(ResourcesService resources, TextureRegion picture) {
        up = new TextureRegionDrawable(resources.getUiImage(UiImage.ROUND_BUTTON));
        down = new TextureRegionDrawable(resources.getUiImage(UiImage.ROUND_BUTTON_DOWN));
        setSize(up.getRegion().getRegionWidth() * SCALE, up.getRegion().getRegionHeight() * SCALE);

        addActor(background);
        icon = new Image(picture);
        icon.setSize(picture.getRegionWidth() * ICON_SCALE, picture.getRegionHeight() * ICON_SCALE);
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
        background.setSize(drawable.getRegion().getRegionWidth() * SCALE, drawable.getRegion().getRegionHeight() * SCALE);
        float top = background.getHeight() - (up.getRegion().getRegionHeight() - SIDE) * SCALE;
        float faceMiddle = top + (getHeight() - SIDE * SCALE) / 2f;
        icon.setPosition(Math.round((getWidth() - icon.getWidth()) / 2f), Math.round(faceMiddle - icon.getHeight() / 2f));
    }
}
