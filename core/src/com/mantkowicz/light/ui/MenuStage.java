package com.mantkowicz.light.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.stage.StageType;
import com.mantkowicz.light.ui.menu.MenuButton;

import static com.mantkowicz.light.stage.StageType.MENU_STAGE;

public class MenuStage extends AbstractStage {
    public MenuStage(Viewport viewport) {
        super(viewport);
    }

    /**
     * Runs the interaction of the menu button under the pointer, if there is one.
     */
    public void interactWithButtonUnderPointer() {
        MenuButton button = getButtonUnderPointer();
        if (button != null) {
            button.interact();
        }
    }

    private MenuButton getButtonUnderPointer() {
        Vector2 pointerPosition = screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        Actor actor = hit(pointerPosition.x, pointerPosition.y, true);
        if (actor != null && actor.getParent() instanceof MenuButton) {
            return (MenuButton) actor.getParent();
        }
        return null;
    }

    @Override
    public void addActor(Actor actor) {
        if (!(actor instanceof MenuButton)) {
            throw new RuntimeException("Actor on this stage must be instanceof MenuButton");
        }
        super.addActor(actor);
    }

    @Override
    public StageType getStageType() {
        return MENU_STAGE;
    }
}
