package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.event.GameEventService;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Board extends Actor {
    private final List<Tile> tiles;

    public Board(BoardService boardService, GameEventService gameEventService, List<Tile> tiles) {
        this.tiles = tiles;
        setupPositionAndSize(tiles);
        addListener(new BoardClickListener(boardService, gameEventService, this));
    }

    private void setupPositionAndSize(List<Tile> tiles) {
        if (!tiles.isEmpty()) {
            float minX = tiles.get(0).getX();
            float maxX = tiles.get(0).getX();
            float minY = tiles.get(0).getY();
            float maxY = tiles.get(0).getY();

            for (Tile tile : tiles) {
                minX = min(minX, tile.getX());
                maxX = max(maxX, tile.getX());
                minY = min(minY, tile.getY());
                maxY = max(maxY, tile.getY());
            }

            setPosition(minX, minY);
            setSize(maxX - minX + tiles.get(0).getWidth(), maxY - minY + tiles.get(0).getHeight());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        for (Tile tile : tiles) {
            tile.draw(batch, parentAlpha);
        }
        for (Tile tile : tiles) {
            tile.drawObjects(batch);
        }
    }

    public List<Tile> getTiles() {
        return tiles;
    }
}
