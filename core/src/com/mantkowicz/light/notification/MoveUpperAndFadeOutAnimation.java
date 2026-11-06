package com.mantkowicz.light.notification;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

public class MoveUpperAndFadeOutAnimation implements NotificationAnimation {
    private static final float DURATION = 2f;

    @Override
    public Action getAction(Notification notification) {
        return Actions.parallel(Actions.moveBy(0, 20, DURATION), Actions.fadeOut(DURATION));
    }
}
