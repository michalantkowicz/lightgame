package com.mantkowicz.light.context;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.stage.StageType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Everything the game objects need, created once by the game screen.
 * Classes take what they need from it in their constructor and keep only those fields.
 * <p>
 * It sits low in the architecture (with the world: board, actors, events), so it hands out the stages as plain libGDX
 * stages; code that needs more of one gets it from the game screen.
 */
public class GameContext {
    private final ResourcesService resourcesService;
    private final GameEventService gameEventService;
    private final PhraseService phraseService;
    private final World world;
    private final RayHandler rayHandler;
    private final OrthographicCamera camera;
    private final Stage gameStage;
    private final Stage notificationStage;
    private final Stage menuStage;
    private final Stage uiStage;
    private final BoardService boardService;

    // baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public GameContext(ResourcesService resourcesService,
            GameEventService gameEventService,
            PhraseService phraseService,
            World world,
            RayHandler rayHandler,
            OrthographicCamera camera,
            Stage gameStage,
            Stage notificationStage,
            Stage menuStage,
            Stage uiStage,
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

    public Stage getGameStage() {
        return gameStage;
    }

    public Stage getNotificationStage() {
        return notificationStage;
    }

    public Stage getMenuStage() {
        return menuStage;
    }

    public Stage getUiStage() {
        return uiStage;
    }

    public BoardService getBoardService() {
        return boardService;
    }

    /** The stages by what they show, where a game actor that names its stage is added. */
    public Map<StageType, Stage> getStages() {
        Map<StageType, Stage> stages = new EnumMap<>(StageType.class);
        stages.put(StageType.GAME_STAGE, gameStage);
        stages.put(StageType.NOTIFICATION_STAGE, notificationStage);
        stages.put(StageType.MENU_STAGE, menuStage);
        stages.put(StageType.UI_STAGE, uiStage);
        return stages;
    }
}
