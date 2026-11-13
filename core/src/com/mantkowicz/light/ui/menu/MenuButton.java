package com.mantkowicz.light.ui.menu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.actor.Interaction;
import com.mantkowicz.light.notification.LabelFactory;
import com.mantkowicz.light.notification.NotificationStyle;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.UiStyle;

public class MenuButton extends Group {
    private static final float HOVER_SCALE = 1.5f;
    private static final float HOVER_DURATION = .2f;

    private final Interaction interaction;

    public MenuButton(ResourcesService resourcesService, Interaction interaction) {
        Image image = UiStyle.icon(resourcesService, interaction.getThumbnailType().getImage());
        image.setPosition(-image.getWidth() / 2f, -image.getHeight() / 2f);
        addActor(image);

        Label label = LabelFactory.createLabel(resourcesService.getFont(), NotificationStyle.DEFAULT, "button");
        label.setPosition(-label.getWidth() / 2f, image.getHeight() / 2f + 5f);
        addActor(label);

        this.interaction = interaction;

        setOrigin(Align.center);
        setTransform(true);

        addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                addAction(Actions.scaleTo(HOVER_SCALE, HOVER_SCALE, HOVER_DURATION));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                addAction(Actions.scaleTo(1f, 1f, HOVER_DURATION));
            }
        });
    }

    public void interact() {
        interaction.interact();
    }
}
