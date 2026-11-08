package com.mantkowicz.light.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.mantkowicz.light.game.Main;

public class HtmlLauncher extends GwtApplication {
    /** The browser's camera zoom, see {@link Main#CAMERA_ZOOM} (1 = as designed, below 1 = closer). */
    private static final float CAMERA_ZOOM = Main.CAMERA_ZOOM;

    @Override
    public GwtApplicationConfiguration getConfig() {
        // fill the browser window and follow its size; the game uses an ExtendViewport
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(true);
        config.padHorizontal = 0;
        config.padVertical = 0;
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new Main(CAMERA_ZOOM);
    }
}
