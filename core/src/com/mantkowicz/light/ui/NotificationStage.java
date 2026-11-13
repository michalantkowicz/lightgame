package com.mantkowicz.light.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.notification.Notification;
import com.mantkowicz.light.notification.NotificationType;
import com.mantkowicz.light.stage.StageType;

import static com.mantkowicz.light.stage.StageType.NOTIFICATION_STAGE;

public class NotificationStage extends AbstractStage {
    public NotificationStage(Viewport viewport) {
        super(viewport);
    }

    public boolean doesContainNotification(NotificationType notificationType) {
        for (Actor actor : getActors()) {
            if (actor instanceof Notification) {
                if (notificationType.equals(((Notification) actor).getNotificationType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getNotificationCount(NotificationType notificationType) {
        int count = 0;
        for (Actor actor : getActors()) {
            if (actor instanceof Notification) {
                if (notificationType.equals(((Notification) actor).getNotificationType())) {
                    count++;
                }
            }
        }
        return count;
    }

    public void removeAllNotificationsOfType(NotificationType notificationType) {
        for (Actor actor : getActors()) {
            if (actor instanceof Notification) {
                if (notificationType.equals(((Notification) actor).getNotificationType())) {
                    actor.remove();
                }
            }
        }
    }

    @Override
    public StageType getStageType() {
        return NOTIFICATION_STAGE;
    }
}
