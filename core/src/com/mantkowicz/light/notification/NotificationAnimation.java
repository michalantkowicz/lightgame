package com.mantkowicz.light.notification;

import com.badlogic.gdx.scenes.scene2d.Action;

public interface NotificationAnimation {
    Action getAction(Notification notification);
}
