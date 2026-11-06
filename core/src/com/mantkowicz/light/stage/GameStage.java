package com.mantkowicz.light.stage;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.service.resources.ResourcesService;

import static com.mantkowicz.light.service.resources.ShaderType.FADE_OUT;
import static com.mantkowicz.light.stage.StageType.GAME_STAGE;

public class GameStage extends AbstractStage {
    private final ShaderProgram shaderProgram;

    public GameStage(Viewport viewport, ResourcesService resourcesService) {
        super(viewport);
        shaderProgram = new ShaderProgram(resourcesService.getShaderVertex(FADE_OUT), resourcesService.getShaderFragment(FADE_OUT));
    }

    public void fadeOut() {
        shaderProgram.bind();
        shaderProgram.setUniformf("saturation", 0.25f);
        shaderProgram.setUniformf("brightness", 0.25f);

        getBatch().setShader(shaderProgram);
    }

    public void fadeIn() {
        getBatch().setShader(null);
    }

    /**
     * Disposes the fade shader too.
     */
    @Override
    public void dispose() {
        shaderProgram.dispose();
        super.dispose();
    }

    @Override
    public StageType getStageType() {
        return GAME_STAGE;
    }
}
