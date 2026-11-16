package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.RoundButton;

/**
 * The HUD's big round button right of the face: the tool for where the player stands (e.g. the scythe on grass), or
 * without one the hint (the question mark). Not to be mixed up with the small action buttons over things on the map
 * ({@link ActionButton}).
 */
public class ToolButton extends RoundButton {
    /** Its name for the question mark (no tool). */
    public static final String HINT = "hint";

    private final ResourcesService resources;
    private String toolName = HINT;
    private Runnable toolUse;

    /** @param hint what tapping it does without a tool (the face says the hint) */
    ToolButton(ResourcesService resources, Runnable hint) {
        super(resources, resources.getUiImage(UiImage.HINT_ICON));
        this.resources = resources;
        setName("TOOL");
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (toolUse != null) {
                    toolUse.run();
                } else {
                    hint.run();
                }
            }
        });
    }

    /**
     * It shows this tool and tapping it uses it, until {@link #resetTool()}.
     *
     * @param name what the tool is, e.g. "scythe" (for tests and descriptions)
     */
    public void setTool(String name, TextureRegion picture, Runnable use) {
        toolName = name;
        toolUse = use;
        setPicture(picture);
    }

    /** No tool: the question mark again, and tapping it gives the hint. */
    public void resetTool() {
        toolName = HINT;
        toolUse = null;
        setPicture(resources.getUiImage(UiImage.HINT_ICON));
    }

    /** The tool on it, e.g. "scythe", or {@link #HINT} for the question mark. */
    public String getToolName() {
        return toolName;
    }
}
