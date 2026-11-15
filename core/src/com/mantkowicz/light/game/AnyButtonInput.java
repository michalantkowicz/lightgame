package com.mantkowicz.light.game;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputMultiplexer;

/**
 * Hands input on to the stages with every mouse button reported as the left one: young players click with whichever
 * button, and the right one should do exactly what the left does (scene2d's buttons listen to the left one only,
 * while the board took any). Pressing a second button while one is held does nothing more: one press, one click.
 * The HTML page also keeps the browser's own right-click menu away (html/webapp/index.html).
 */
class AnyButtonInput extends InputMultiplexer {
    /** More than any device has fingers (libGDX tracks 20 pointers). */
    private static final int POINTERS = 20;

    /** Per pointer, the buttons held down, one bit each. */
    private final int[] held = new int[POINTERS];

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!press(pointer, button)) {
            return true; // another button is already down: the press it started goes on
        }
        return super.touchDown(screenX, screenY, pointer, Buttons.LEFT);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!release(pointer, button)) {
            return true; // another button is still down: the press ends with that one
        }
        return super.touchUp(screenX, screenY, pointer, Buttons.LEFT);
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (pointer >= 0 && pointer < POINTERS) {
            held[pointer] = 0;
        }
        return super.touchCancelled(screenX, screenY, pointer, Buttons.LEFT);
    }

    /** @return true when it is the first button down on the pointer */
    private boolean press(int pointer, int button) {
        if (pointer < 0 || pointer >= POINTERS || button < 0 || button >= Integer.SIZE) {
            return true;
        }
        boolean first = held[pointer] == 0;
        held[pointer] |= 1 << button;
        return first;
    }

    /** @return true when it was the last button down on the pointer */
    private boolean release(int pointer, int button) {
        if (pointer < 0 || pointer >= POINTERS || button < 0 || button >= Integer.SIZE) {
            return true;
        }
        held[pointer] &= ~(1 << button);
        return held[pointer] == 0;
    }
}
