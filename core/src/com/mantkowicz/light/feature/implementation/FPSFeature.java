package com.mantkowicz.light.feature.implementation;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.notification.NotificationStyle;

import static com.badlogic.gdx.Gdx.graphics;
import static com.mantkowicz.light.notification.factory.LabelFactory.createLabel;
import static java.lang.String.valueOf;

public class FPSFeature implements Feature {
    private final Vector2 offset;
    private final Camera camera;
    private final Label fpsLabel;

    public FPSFeature(GameContext context, Vector2 offset) {
        this.offset = offset;
        camera = context.getGameStage().getCamera();
        fpsLabel = createLabel(context.getResourcesService().getFont(), NotificationStyle.DEFAULT, "");
        context.getGameStage().addActor(fpsLabel);
    }

    @Override
    public void run(float delta) {
        fpsLabel.toFront();
        fpsLabel.setPosition(camera.position.x + offset.x, camera.position.y + offset.y);
        fpsLabel.setText(valueOf(graphics.getFramesPerSecond()));
    }
}
