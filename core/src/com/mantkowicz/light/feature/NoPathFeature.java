package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.board.NoPathEvent;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.notification.MoveUpperAndFadeOutAnimation;
import com.mantkowicz.light.notification.Notification;
import com.mantkowicz.light.notification.NotificationBuilder;
import com.mantkowicz.light.resources.PhraseService;

import static com.mantkowicz.light.notification.NotificationStyle.WARNING;
import static com.mantkowicz.light.notification.NotificationType.NO_PATH_NOTIFICATION;
import static com.mantkowicz.light.resources.PhraseGroup.NO_PATH;

/**
 * Says "no path" over a tile nobody can walk to, when the player clicks it ({@link NoPathEvent}, posted by the
 * board): the text rises and fades.
 */
public class NoPathFeature implements Feature {
    private final GameEventService gameEventService;
    private final Stage notificationStage;
    private final PhraseService phraseService;
    private final BitmapFont font;

    public NoPathFeature(GameContext context) {
        gameEventService = context.getGameEventService();
        notificationStage = context.getNotificationStage();
        phraseService = context.getPhraseService();
        font = context.getResourcesService().getFont();
    }

    @Override
    public void run(float delta) {
        for (NoPathEvent event = gameEventService.poll(NoPathEvent.class); event != null; event = gameEventService
                .poll(NoPathEvent.class)) {
            Notification noPath = new NotificationBuilder(font, phraseService.getRandomPhrase(NO_PATH))
                    .notificationAnimation(new MoveUpperAndFadeOutAnimation())
                    .notificationType(NO_PATH_NOTIFICATION)
                    .notificationStyle(WARNING)
                    .centerAt(event.getTile().getCenter())
                    .build();
            notificationStage.addActor(noPath);
        }
    }
}
