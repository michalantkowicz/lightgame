package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

/**
 * The settings window: one panel on the dimmed screen, everything at once, a big check mark to close. Its settings
 * (age, music, sounds) come with step 4 of docs/hud-design.md; for now it holds the title and the check mark.
 */
public class SettingsPanel extends Group {
    private static final float WIDTH = 560, HEIGHT = 640;
    private final ImageButton close;

    public SettingsPanel(ResourcesService resources, float screenWidth, float screenHeight) {
        setSize(screenWidth, screenHeight);
        Image dim = new Image(resources.getDrawable("black.png"));
        dim.setSize(screenWidth, screenHeight);
        dim.getColor().a = 0.55f;
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        Table content = new Table();
        content.add(new Label("SETTINGS", new Label.LabelStyle(resources.getFont(), UiStyle.TEXT))).expandY().top()
                .padTop(40).row();
        close = new ImageButton(UiStyle.image(resources, UiImage.CHECK_BUTTON));
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });
        content.add(close).padBottom(40);

        Container<Table> window = new Container<>(content);
        window.setBackground(UiStyle.stretching(resources, UiImage.PANEL));
        window.fill();
        window.setBounds((screenWidth - WIDTH) / 2f, (screenHeight - HEIGHT) / 2f, WIDTH, HEIGHT);
        window.setTouchable(Touchable.enabled);
        addActor(window);

        // a tap anywhere on it (the dimmed screen, the window) is taken: the map under it doesn't get it
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
    }

    /** The check mark that closes the window. */
    public ImageButton getCloseButton() {
        return close;
    }
}
