package com.mantkowicz.light.light;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import static com.badlogic.gdx.scenes.scene2d.actions.RepeatAction.FOREVER;
import static com.mantkowicz.light.light.BlinkingIntensity.MEDIUM;
import static com.mantkowicz.light.light.BlinkingSpeed.NORMAL;
import static com.mantkowicz.light.light.LightType.TORCH;

public class TorchLight extends GameLight {
    private static final int RAYS = 50;
    private static final int DISTANCE = 240;
    private static final Color COLOR = new Color(0.4f, 0.4f, 0.4f, 1f);

    public TorchLight(RayHandler rayHandler) {
        super(TORCH);

        light = new PointLight(rayHandler, RAYS);
        light.setDistance(DISTANCE);
        light.setColor(COLOR);

        BlinkingAction blinkingAction = new BlinkingAction(light, MEDIUM, NORMAL);
        addAction(Actions.repeat(FOREVER, blinkingAction));
    }
}
