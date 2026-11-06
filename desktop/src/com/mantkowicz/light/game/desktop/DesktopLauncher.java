package com.mantkowicz.light.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mantkowicz.light.game.Main;
import org.lwjgl.system.Configuration;

import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

public class DesktopLauncher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        useFfmMemoryBackend();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Light Game");
        config.setWindowedMode((int) SCREEN_WIDTH / 2, (int) SCREEN_HEIGHT / 2);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.disableAudio(true); // the game has no sound; avoids OpenAL init and its log noise
        new Lwjgl3Application(new Main(), config);
    }

    /**
     * LWJGL's default memory backend calls sun.misc.Unsafe, which JDK 24+ reports with a "terminally deprecated" warning and a future
     * JDK will block. On JDK 25+ LWJGL can use the Foreign Function & Memory API instead. LWJGL notes it is a bit slower; this game
     * does very little native memory work, so it doesn't matter. Must run before LWJGL is first used.
     */
    private static void useFfmMemoryBackend() {
        if (Runtime.version().feature() >= 25) {
            Configuration.MEMORY_BACKEND.set("ffm");
        }
    }
}
