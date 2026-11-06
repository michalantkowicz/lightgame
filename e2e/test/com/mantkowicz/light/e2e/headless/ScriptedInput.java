package com.mantkowicz.light.e2e.headless;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.headless.mock.input.MockInput;

/** Input whose pointer position and key presses are set by the test instead of a real device. */
final class ScriptedInput extends MockInput {
    private static final int NO_KEY = -1;

    private InputProcessor processor;
    private int x;
    private int y;
    private int keyJustPressed = NO_KEY;

    void movePointer(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void pressKey(int key) {
        keyJustPressed = key;
    }

    /** A key counts as "just pressed" for exactly one frame. */
    void endFrame() {
        keyJustPressed = NO_KEY;
    }

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.processor = processor;
    }

    @Override
    public InputProcessor getInputProcessor() {
        return processor;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getX(int pointer) {
        return x;
    }

    @Override
    public int getY(int pointer) {
        return y;
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return key == keyJustPressed;
    }
}
