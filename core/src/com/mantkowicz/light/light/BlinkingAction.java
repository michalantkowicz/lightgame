package com.mantkowicz.light.light;

import box2dLight.Light;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

public class BlinkingAction extends SequenceAction {

    public BlinkingAction(Light light, BlinkingIntensity intensity, BlinkingSpeed speed) {
        Action singleBlinkAction = getSingleBlinkAction(light, intensity, light.getDistance());
        addAction(singleBlinkAction);
        addAction(Actions.delay(speed.getValue()));
    }

    private Action getSingleBlinkAction(final Light light, final BlinkingIntensity intensity,
            final float baseDistance) {
        return new Action() {
            @Override
            public boolean act(float delta) {
                float randomStart = baseDistance - intensity.getValue();
                float randomEnd = baseDistance + intensity.getValue();
                light.setDistance(MathUtils.random(randomStart, randomEnd));
                return true;
            }
        };
    }
}
