package com.mantkowicz.light.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class AbstractStage extends Stage {
    public AbstractStage(Viewport viewport) {
        super(viewport);
    }

    public abstract StageType getStageType();
}
