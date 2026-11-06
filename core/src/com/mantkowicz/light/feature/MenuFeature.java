package com.mantkowicz.light.feature;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.MenuCloseEvent;
import com.mantkowicz.light.event.MenuOpenEvent;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.MenuStage;
import com.mantkowicz.light.ui.menu.Interaction;
import com.mantkowicz.light.ui.menu.MenuButton;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.math.Interpolation.swingOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;

public class MenuFeature implements Feature {
    private static final float MENU_RADIUS = 150f;
    private static final float BUTTON_OFFSET_DEGREES = 60f;
    private static final float MENU_ANIMATION_DURATION = .5f;

    private final MenuStage stage;
    private final GameEventService gameEventService;
    private final ResourcesService resourcesService;

    public MenuFeature(GameContext context) {
        this.stage = context.getMenuStage();
        this.gameEventService = context.getGameEventService();
        this.resourcesService = context.getResourcesService();
    }

    @Override
    public void run(float delta) {
        if (gameEventService.has(MenuOpenEvent.class)) {
            MenuOpenEvent event = gameEventService.poll(MenuOpenEvent.class);
            GameBoardActor actor = event.getActor();

            for (MenuButton button : createMenuButtons(actor)) {
                stage.addActor(button);
            }
        } else if (gameEventService.has(MenuCloseEvent.class)) {
            gameEventService.poll(MenuCloseEvent.class);
            stage.interactWithButtonUnderPointer();
            stage.clear();
        }
    }

    private List<MenuButton> createMenuButtons(GameBoardActor actor) {
        List<MenuButton> buttons = new ArrayList<>();
        List<Interaction> interactions = actor.getInteractions();

        for (int i = 0; i < interactions.size(); i++) {
            MenuButton button = createButton(actor, i, interactions);
            buttons.add(button);
        }
        return buttons;
    }

    private MenuButton createButton(GameBoardActor actor, int elementIndex, List<Interaction> interactions) {
        MenuButton button = new MenuButton(resourcesService, interactions.get(elementIndex));
        button.setPosition(actor.getCenter().x, actor.getCenter().y);
        button.setScale(.1f);
        button.getColor().a = 0;

        Vector2 targetPosition = calculatePosition(button, elementIndex, interactions.size()).add(actor.getCenter());

        Action action = createAction(elementIndex, targetPosition);
        button.addAction(action);

        return button;
    }

    private DelayAction createAction(int elementIndex, Vector2 targetPosition) {
        float delayValue = elementIndex * 0.1f;
        return delay(
                delayValue,
                parallel(
                        moveTo(targetPosition.x, targetPosition.y, MENU_ANIMATION_DURATION, swingOut),
                        scaleTo(1f, 1f, MENU_ANIMATION_DURATION, swingOut),
                        alpha(1f, MENU_ANIMATION_DURATION / 2f)
                )
        );
    }

    private Vector2 calculatePosition(Actor button, int elementIndex, int elementsCount) {
        float firstElementRotation = (BUTTON_OFFSET_DEGREES * (elementsCount - 1)) / 2f;

        return new Vector2(0, MENU_RADIUS)
                .rotateDeg(firstElementRotation - (elementIndex * BUTTON_OFFSET_DEGREES))
                .sub(button.getWidth() / 2f, button.getHeight() / 2f);
    }
}
