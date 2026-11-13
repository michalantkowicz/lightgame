package com.mantkowicz.light.ui;

import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.notification.NotificationType;
import com.mantkowicz.light.notification.Notifications;
import com.mantkowicz.light.stage.StageType;

import static com.mantkowicz.light.stage.StageType.NOTIFICATION_STAGE;

public class NotificationStage extends AbstractStage {
    public NotificationStage(Viewport viewport) {
        super(viewport);
    }

    public boolean doesContainNotification(NotificationType notificationType) {
        return Notifications.contains(this, notificationType);
    }

    public int getNotificationCount(NotificationType notificationType) {
        return Notifications.count(this, notificationType);
    }

    public void removeAllNotificationsOfType(NotificationType notificationType) {
        Notifications.removeAll(this, notificationType);
    }

    @Override
    public StageType getStageType() {
        return NOTIFICATION_STAGE;
    }
}
