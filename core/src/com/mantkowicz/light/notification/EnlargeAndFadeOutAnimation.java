package com.mantkowicz.light.notification;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.TimeUtils;

public class EnlargeAndFadeOutAnimation implements NotificationAnimation {
    private static final int SCALE = 5;
    private static final float SCALE_STEP = 0.01f;
    private static final float DURATION = 2f;

    @Override
    public Action getAction(Notification notification) {
        return Actions.parallel(getEnlargeAction(notification, getCurrentTime()), Actions.fadeOut(DURATION));
    }

    private long getCurrentTime() {
        return TimeUtils.millis();
    }

    private Action getEnlargeAction(final Notification notification, final long startTime) {
        return new Action() {
            @Override
            public boolean act(float delta) {
                Label label = notification.getLabel();
                float currentScale = label.getFontScaleX();
                if (shouldContinueAction(currentScale)) {
                    label.setFontScale(currentScale + SCALE_STEP);
                    return false;
                } else {
                    return true;
                }
            }

            private boolean shouldContinueAction(float currentScale) {
                float durationInMillis = DURATION * 1000;
                return currentScale < SCALE && (getCurrentTime() - startTime) < durationInMillis;
            }
        };
    }
}
