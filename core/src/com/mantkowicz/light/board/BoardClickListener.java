package com.mantkowicz.light.board;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.event.GameEventService;

public class BoardClickListener extends ClickListener {
    private final Board board;
    private final BoardService boardService;
    private final GameEventService gameEventService;
    private final Vector2 point = new Vector2();

    public BoardClickListener(BoardService boardService, GameEventService gameEventService, Board board) {
        this.board = board;
        this.boardService = boardService;
        this.gameEventService = gameEventService;
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
                gameEventService.post(new NoPathEvent(touchedTile)); // NoPathFeature says so
            } else if (touchedTile.isMarked()) {
                touchedTile.unmark();
                gameEventService.post(new TileTouchedEvent(touchedTile));
            }
        }
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
