package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class OpenCollectWindowButtonListener extends ClickListener {
    private final CollectWindow window;
    private final Stage uiStage;

    public OpenCollectWindowButtonListener(CollectWindow window, Stage uiStage) {
        this.window = window;
        this.uiStage = uiStage;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        super.clicked(event, x, y);
        uiStage.addActor(window);
    }
}
