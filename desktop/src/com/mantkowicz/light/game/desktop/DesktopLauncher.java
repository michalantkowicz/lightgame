package com.mantkowicz.light.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mantkowicz.light.game.Main;

import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

public class DesktopLauncher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Light Game");
        config.setWindowedMode((int) SCREEN_WIDTH / 2, (int) SCREEN_HEIGHT / 2);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.disableAudio(true); // the game has no sound; avoids OpenAL init and its log noise
        new Lwjgl3Application(new Main(), config);
    }
}
