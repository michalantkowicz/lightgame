package com.mantkowicz.light.plugin.implementation;

import box2dLight.RayHandler;
import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.notification.Notification;
import com.mantkowicz.light.notification.animation.EnlargeAndFadeOutAnimation;
import com.mantkowicz.light.notification.factory.NotificationBuilder;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.service.phrase.PhraseService;
import com.mantkowicz.light.stage.NotificationStage;

import static com.mantkowicz.light.notification.NotificationType.PLAYER_NOTIFICATION;
import static com.mantkowicz.light.service.phrase.PhraseGroup.DARKNESS_EXCLAMATION;

public class PlayerNotificationPlugin implements Plugin {
    private static final int MILLIS_TO_EXCLAIM_AT_SHADOW = 2000;

    private final Player player;
    private final RayHandler rayHandler;
    private final NotificationStage notificationStage;
    private final PhraseService phraseService;

    public PlayerNotificationPlugin(Player player, GameContext context) {
        this.player = player;
        this.rayHandler = context.getRayHandler();
        this.notificationStage = context.getNotificationStage();
        this.phraseService = context.getPhraseService();
    }

    @Override
    public void run(float delta) {
        if (shouldPlayerExclaimAtShadow()) {
            Notification shadowNotification = getExclamationAtShadow();
            notificationStage.addActor(shadowNotification);
            shadowNotification.toFront();
        } else if (player.getIdleLength() < MILLIS_TO_EXCLAIM_AT_SHADOW) {
            notificationStage.removeAllNotificationsOfType(PLAYER_NOTIFICATION);
        }
    }

    private boolean shouldPlayerExclaimAtShadow() {
        return isPlayerAtShadow()
                && player.getIdleLength() >= MILLIS_TO_EXCLAIM_AT_SHADOW
                && !notificationStage.doesContainNotification(PLAYER_NOTIFICATION);
    }

    /**
     * Check whether the player is at shadow. Method check four corners of actor bounding box
     *
     * @return whether at least one corner is at shadow
     */
    private boolean isPlayerAtShadow() {
        return rayHandler.pointAtShadow(player.getX(), player.getY())
                || rayHandler.pointAtShadow(player.getX() + player.getWidth(), player.getY())
                || rayHandler.pointAtShadow(player.getX() + player.getWidth(), player.getY() + player.getHeight())
                || rayHandler.pointAtShadow(player.getX(), player.getY() + player.getHeight());
    }

    private Notification getExclamationAtShadow() {
        String exclamationText = phraseService.getRandomPhrase(DARKNESS_EXCLAMATION);
        Vector2 notificationCenter = player.getTile().getNotificationCenterPosition();
        return new NotificationBuilder(exclamationText)
                .notificationAnimation(new EnlargeAndFadeOutAnimation())
                .notificationType(PLAYER_NOTIFICATION)
                .centerAt(notificationCenter)
                .build();
    }
}
