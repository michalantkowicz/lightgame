package com.mantkowicz.light.game;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mantkowicz.light.game.Main;

public class AndroidLauncher extends AndroidApplication {
	/** Android's camera zoom, see {@link Main#CAMERA_ZOOM} (1 = as designed, below 1 = closer). */
	private static final float CAMERA_ZOOM = Main.CAMERA_ZOOM;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new Main(CAMERA_ZOOM), config);
	}
}
