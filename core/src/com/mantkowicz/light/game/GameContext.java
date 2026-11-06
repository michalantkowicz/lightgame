package com.mantkowicz.light.game;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.World;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.AbstractStage;
import com.mantkowicz.light.ui.GameStage;
import com.mantkowicz.light.ui.MenuStage;
import com.mantkowicz.light.ui.NotificationStage;
import com.mantkowicz.light.ui.UiStage;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Everything the game objects need, created once by the game screen.
 * Classes take what they need from it in their constructor and keep only those fields.
 */
public class GameContext {
    private final ResourcesService resourcesService;
    private final GameEventService gameEventService;
    private final PhraseService phraseService;
    private final World world;
    private final RayHandler rayHandler;
    private final OrthographicCamera camera;
    private final GameStage gameStage;
    private final NotificationStage notificationStage;
    private final MenuStage menuStage;
    private final UiStage uiStage;
    private final BoardService boardService;

    public GameContext(ResourcesService resourcesService,
                       GameEventService gameEventService,
                       PhraseService phraseService,
                       World world,
                       RayHandler rayHandler,
                       OrthographicCamera camera,
                       GameStage gameStage,
                       NotificationStage notificationStage,
                       MenuStage menuStage,
                       UiStage uiStage,
                       BoardService boardService) {
        this.resourcesService = resourcesService;
        this.gameEventService = gameEventService;
        this.phraseService = phraseService;
        this.world = world;
        this.rayHandler = rayHandler;
        this.camera = camera;
        this.gameStage = gameStage;
        this.notificationStage = notificationStage;
        this.menuStage = menuStage;
        this.uiStage = uiStage;
        this.boardService = boardService;
    }

    public ResourcesService getResourcesService() {
        return resourcesService;
    }

    public GameEventService getGameEventService() {
        return gameEventService;
    }

    public PhraseService getPhraseService() {
        return phraseService;
    }

    public World getWorld() {
        return world;
    }

    public RayHandler getRayHandler() {
        return rayHandler;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public GameStage getGameStage() {
        return gameStage;
    }

    public NotificationStage getNotificationStage() {
        return notificationStage;
    }

    public MenuStage getMenuStage() {
        return menuStage;
    }

    public UiStage getUiStage() {
        return uiStage;
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public List<AbstractStage> getStages() {
        return asList(gameStage, notificationStage, menuStage, uiStage);
    }
}
