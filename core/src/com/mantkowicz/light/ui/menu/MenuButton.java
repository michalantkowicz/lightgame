package com.mantkowicz.light.ui.menu;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.interaction.Interaction;
import com.mantkowicz.light.notification.NotificationStyle;
import com.mantkowicz.light.notification.factory.LabelFactory;
import com.mantkowicz.light.service.resources.ResourcesService;

public class MenuButton extends Group {
    private final Interaction interaction;

    public MenuButton(ResourcesService resourcesService, Interaction interaction) {
        Image image = new Image(resourcesService.getThumbnail(interaction.getThumbnailType()));
        image.setPosition(-image.getWidth() / 2f, -image.getHeight() / 2f);
        addActor(image);

        Label label = LabelFactory.createLabel(resourcesService.getFont(), NotificationStyle.DEFAULT, "button");
        label.setPosition(-label.getWidth() / 2f, image.getHeight() / 2f + 5f);
        addActor(label);

        this.interaction = interaction;

        setOrigin(Align.center);
        setTransform(true);

        addListener(new MenuButtonListener(this));
    }

    public void interact() {
        interaction.interact();
    }
}
