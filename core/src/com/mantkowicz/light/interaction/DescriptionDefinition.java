package com.mantkowicz.light.interaction;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.GameActorDefinition;
import com.mantkowicz.light.stage.StageType;

import static com.mantkowicz.light.stage.StageType.NOTIFICATION_STAGE;

public class DescriptionDefinition implements GameActorDefinition<Description> {
    private GameActor parent;
    private String description;
    private Drawable background;
    private BitmapFont font;

    @Override
    public Description getActor() {
        return new Description(this);
    }

    @Override
    public StageType getTargetStageType() {
        return NOTIFICATION_STAGE;
    }

    public Drawable getBackground() {
        return background;
    }

    public BitmapFont getFont() {
        return font;
    }

    public GameActor getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public void setParent(GameActor parent) {
        this.parent = parent;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBackground(Drawable background) {
        this.background = background;
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }
}
