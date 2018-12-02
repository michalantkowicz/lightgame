package com.mantkowicz.light.ui.window;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class CollectWindowOkListener extends ClickListener {
    private CollectWindow window;

    public CollectWindowOkListener(CollectWindow window) {
        this.window = window;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        super.clicked(event, x, y);
        window.saveState();
        window.remove();
    }
}
