package com.mantkowicz.light.board;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.notification.Notification;
import com.mantkowicz.light.notification.animation.MoveUpperAndFadeOutAnimation;
import com.mantkowicz.light.notification.factory.NotificationBuilder;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.TileTouchedEvent;
import com.mantkowicz.light.service.phrase.PhraseService;
import com.mantkowicz.light.stage.NotificationStage;

import static com.mantkowicz.light.notification.NotificationStyle.WARNING;
import static com.mantkowicz.light.notification.NotificationType.NO_PATH_NOTIFICATION;
import static com.mantkowicz.light.service.phrase.PhraseGroup.NO_PATH;

public class BoardClickListener extends ClickListener {
    private final Board board;
    private final GameEventService gameEventService;
    private final NotificationStage notificationStage;
    private final PhraseService phraseService;
    private final Vector2 point = new Vector2();

    public BoardClickListener(GameContext context, Board board) {
        this.board = board;
        gameEventService = context.getGameEventService();
        notificationStage = context.getNotificationStage();
        phraseService = context.getPhraseService();
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        Tile touchedTile = resolveTile(x, y);
        if (touchedTile != null) {
            touchedTile.mark();
        }
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        Tile touchedTile = resolveTile(x, y);
        if (touchedTile != null) {
            if (!touchedTile.isAccessible()) {
                handleNoPathTile(touchedTile);
            } else if (touchedTile.isMarked()) {
                touchedTile.unmark();
                gameEventService.addEvent(new TileTouchedEvent(touchedTile));
            }
        }
    }

    private void handleNoPathTile(Tile touchedTile) {
        String noPathText = phraseService.getRandomPhrase(NO_PATH);
        Vector2 notificationCenter = touchedTile.getCenter();
        Notification noPathNotification = new NotificationBuilder(noPathText)
                .notificationAnimation(new MoveUpperAndFadeOutAnimation())
                .notificationType(NO_PATH_NOTIFICATION)
                .notificationStyle(WARNING)
                .centerAt(notificationCenter)
                .build();
        notificationStage.addActor(noPathNotification);
    }

    private Tile resolveTile(float x, float y) {
        for (Tile tile : board.getTiles()) {
            if (tile.doesContainPoint(board.localToStageCoordinates(point.set(x, y)))) {
                return tile;
            }
        }
        return null;
    }
}