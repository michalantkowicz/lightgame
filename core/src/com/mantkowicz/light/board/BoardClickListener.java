package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.TileTouchedEvent;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.notification.MoveUpperAndFadeOutAnimation;
import com.mantkowicz.light.notification.Notification;
import com.mantkowicz.light.notification.NotificationBuilder;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.ui.NotificationStage;

import static com.mantkowicz.light.notification.NotificationStyle.WARNING;
import static com.mantkowicz.light.notification.NotificationType.NO_PATH_NOTIFICATION;
import static com.mantkowicz.light.resources.PhraseGroup.NO_PATH;

public class BoardClickListener extends ClickListener {
    private final Board board;
    private final BoardService boardService;
    private final GameEventService gameEventService;
    private final NotificationStage notificationStage;
    private final PhraseService phraseService;
    private final BitmapFont font;
    private final Vector2 point = new Vector2();

    public BoardClickListener(GameContext context, Board board) {
        this.board = board;
        boardService = context.getBoardService();
        gameEventService = context.getGameEventService();
        notificationStage = context.getNotificationStage();
        phraseService = context.getPhraseService();
        font = context.getResourcesService().getFont();
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
            if (!touchedTile.isAccessible() && !boardService.isUsedFromNeighbour(touchedTile)) {
                handleNoPathTile(touchedTile);
            } else if (touchedTile.isMarked()) {
                touchedTile.unmark();
                gameEventService.post(new TileTouchedEvent(touchedTile));
            }
        }
    }

    private void handleNoPathTile(Tile touchedTile) {
        String noPathText = phraseService.getRandomPhrase(NO_PATH);
        Vector2 notificationCenter = touchedTile.getCenter();
        Notification noPathNotification = new NotificationBuilder(font, noPathText)
                .notificationAnimation(new MoveUpperAndFadeOutAnimation())
                .notificationType(NO_PATH_NOTIFICATION)
                .notificationStyle(WARNING)
                .centerAt(notificationCenter)
                .build();
        notificationStage.addActor(noPathNotification);
    }

    private Tile resolveTile(float x, float y) {
        Vector2 stagePoint = board.localToStageCoordinates(point.set(x, y));
        for (Tile tile : board.getTiles()) {
            if (tile.doesContainPoint(stagePoint)) {
                return tile;
            }
        }
        return null;
    }
}
