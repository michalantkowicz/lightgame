package com.mantkowicz.light.light;

import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.mantkowicz.light.board.Tile;

public abstract class GameLight extends Group {
    private final LightType lightType;
    protected Light light;
    private Tile tile;
    /** It shines only at night: whoever runs day and night switches it ({@link #setShining}). */
    private boolean nightOnly;

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

    public boolean isNightOnly() {
        return nightOnly;
    }

    public void setNightOnly(boolean nightOnly) {
        this.nightOnly = nightOnly;
    }

    /** Switches the light on or off (it keeps blinking either way, unseen while off). */
    public void setShining(boolean shining) {
        light.setActive(shining);
    }

    public boolean isShining() {
        return light.isActive();
    }

    public LightType getLightType() {
        return lightType;
    }
}
