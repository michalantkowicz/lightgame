package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.function.Supplier;

/**
 * Opens a fresh collect window on every click, so a cancelled window never shows its unsaved changes again.
 */
public class OpenCollectWindowButtonListener extends ClickListener {
    private final Supplier<CollectWindow> windowFactory;
    private final Stage uiStage;

    public OpenCollectWindowButtonListener(Supplier<CollectWindow> windowFactory, Stage uiStage) {
        this.windowFactory = windowFactory;
        this.uiStage = uiStage;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        super.clicked(event, x, y);
        uiStage.addActor(windowFactory.get());
    }
}
