package com.mantkowicz.light.e2e.headless;

import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;

/** A fixed-size screen where every frame lasts exactly 1/60 s, so test runs are repeatable. */
final class FixedGraphics extends MockGraphics {
    static final float FRAME_TIME = 1 / 60f;

    private int width;
    private int height;
    private long frameId;

    FixedGraphics(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** Like resizing a real window. */
    void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void nextFrame() {
        frameId++;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getBackBufferWidth() {
        return width;
    }

    @Override
    public int getBackBufferHeight() {
        return height;
    }

    @Override
    public float getDeltaTime() {
        return FRAME_TIME;
    }

    @Override
    public int getFramesPerSecond() {
        return 60;
    }

    @Override
    public long getFrameId() {
        return frameId;
    }
}
