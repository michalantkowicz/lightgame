package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

/** What a stage shows now, in its own coordinates: where a window over the whole screen goes. */
final class ScreenArea {
    private ScreenArea() {
    }

    static Rectangle of(Stage stage) {
        Camera camera = stage.getCamera();
        Viewport viewport = stage.getViewport();
        return new Rectangle(camera.position.x - viewport.getWorldWidth() / 2f,
                camera.position.y - viewport.getWorldHeight() / 2f, viewport.getWorldWidth(),
                viewport.getWorldHeight());
    }
}
