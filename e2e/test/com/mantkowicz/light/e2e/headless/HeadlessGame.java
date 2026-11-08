package com.mantkowicz.light.e2e.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.game.Main;

/**
 * Runs the real game without a window or GPU. Frames are advanced explicitly with a fixed time step and
 * input is injected by the test, so the same script always produces the same game state.
 * Window size and random seed match what a player gets on desktop (800x1280 world shown at half size).
 */
public final class HeadlessGame {
    public static final int SCREEN_WIDTH = 400;
    public static final int SCREEN_HEIGHT = 640;
    private static final long RANDOM_SEED = 42;

    private final FixedGraphics graphics = new FixedGraphics(SCREEN_WIDTH, SCREEN_HEIGHT);
    private final ScriptedInput input = new ScriptedInput();
    private final Main game;

    /** The map the tests were written for (map.tmx, the original dark house). */
    public static final String TEST_MAP = "map.tmx";
    /** The goldens were recorded at the designed zoom, whatever Main.CAMERA_ZOOM is set to. */
    public static final float TEST_CAMERA_ZOOM = 1f;

    public HeadlessGame() {
        this(TEST_MAP);
    }

    public HeadlessGame(String mapName) {
        this(mapName, TEST_CAMERA_ZOOM);
    }

    public HeadlessGame(String mapName, float cameraZoom) {
        initializeGdx();
        Gdx.graphics = graphics;
        Gdx.input = input;
        MathUtils.random.setSeed(RANDOM_SEED);

        game = new Main(mapName, cameraZoom);
        game.create();
        game.resize(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    /**
     * Starts libGDX once per JVM: loads the native libraries and sets up file access. The backend's own loop
     * renders an empty listener; the game is driven by {@link #frame()} on the caller's thread.
     */
    public static synchronized void initializeGdx() {
        if (Gdx.app != null) {
            return;
        }
        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 1;
        new HeadlessApplication(new ApplicationAdapter() {
        }, configuration);
        Gdx.gl = Gdx.gl20 = NoOpGL.create();
        // the no-op GL reports no shader uniforms; don't fail when the game sets one
        ShaderProgram.pedantic = false;
    }

    public void frame() {
        graphics.nextFrame();
        game.render();
        input.endFrame();
    }

    public int screenWidth() {
        return graphics.getWidth();
    }

    public int screenHeight() {
        return graphics.getHeight();
    }

    public ApplicationListener game() {
        return game;
    }

    public InputProcessor inputProcessor() {
        return input.getInputProcessor();
    }

    public void movePointer(int screenX, int screenY) {
        input.movePointer(screenX, screenY);
    }

    public void pressKey(int key) {
        input.pressKey(key);
    }

    public void resize(int width, int height) {
        graphics.resize(width, height);
        game.resize(width, height);
    }

    public void dispose() {
        game.dispose();
    }
}
