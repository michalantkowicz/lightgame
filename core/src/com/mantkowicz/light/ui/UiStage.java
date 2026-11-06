package com.mantkowicz.light.ui;

import com.badlogic.gdx.utils.viewport.Viewport;

import static com.mantkowicz.light.ui.StageType.UI_STAGE;

/**
 * Buttons and windows of the game UI (the COLLECT button, the collect window). Drawn on top of everything and gets input first.
 */
public class UiStage extends AbstractStage {
    public UiStage(Viewport viewport) {
        super(viewport);
    }

    @Override
    public StageType getStageType() {
        return UI_STAGE;
    }
}
