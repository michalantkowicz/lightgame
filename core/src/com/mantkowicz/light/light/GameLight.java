package com.mantkowicz.light.light;

import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.mantkowicz.light.board.Tile;

public abstract class GameLight extends Group {
    private final LightType lightType;
    protected Light light;
    private Tile tile;

    protected GameLight(LightType lightType) {
        this.lightType = lightType;
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
        setPosition(tile.getCenter().x, tile.getCenter().y);
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        light.setPosition(x, y);
    }

    public void setLightPosition(float x, float y) {
        light.setPosition(x, y);
    }

    @Override
    public void setColor(Color color) {
        super.setColor(color);
        light.setColor(color);
    }

    public void setLightColor(Color color) {
        light.setColor(color);
    }

    public LightType getLightType() {
        return lightType;
    }
}
