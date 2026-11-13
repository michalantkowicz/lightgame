package com.mantkowicz.light.notification;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

/**
 * Finding and removing the notifications of one type on a stage (usually the notification stage). Works on any
 * libGDX stage, so code below the UI can use it without knowing the UI's stage classes.
 */
public final class Notifications {
    private Notifications() {
    }

    public static boolean contains(Stage stage, NotificationType type) {
        return count(stage, type) > 0;
    }

    public static int count(Stage stage, NotificationType type) {
        int count = 0;
        for (Actor actor : stage.getActors()) {
            if (actor instanceof Notification notification && type.equals(notification.getNotificationType())) {
                count++;
            }
        }
        return count;
    }

    public static void removeAll(Stage stage, NotificationType type) {
        for (Actor actor : stage.getActors()) {
            if (actor instanceof Notification notification && type.equals(notification.getNotificationType())) {
                actor.remove();
            }
        }
    }
}
