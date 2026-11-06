package com.mantkowicz.light.game;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.board.Board;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.PlayerCreatedEvent;
import com.mantkowicz.light.feature.CameraTrackingFeature;
import com.mantkowicz.light.feature.FPSFeature;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.feature.MenuFeature;
import com.mantkowicz.light.feature.StageFeature;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.GameStage;
import com.mantkowicz.light.ui.MenuStage;
import com.mantkowicz.light.ui.NotificationStage;
import com.mantkowicz.light.ui.UiStage;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.debug.Debug.Tool.FPS_LABEL;
import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

public class GameScreen extends ScreenAdapter {
    private final ResourcesService resourcesService;
    private final GameEventService gameEventService;
    private final World world;
    private GameStage gameStage;
    private MenuStage menuStage;
    private NotificationStage notificationStage;
    private UiStage uiStage;
    private Board board;
    private RayHandler rayHandler;
    private final List<Feature> features = new ArrayList<>();

    public GameScreen(ResourcesService resourcesService, GameEventService gameEventService, World world) {
        this.resourcesService = resourcesService;
        this.gameEventService = gameEventService;
        this.world = world;
    }

    @Override
    public void show() {
        Viewport viewport = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
        gameStage = new GameStage(viewport, resourcesService);
        menuStage = new MenuStage(viewport);
        notificationStage = new NotificationStage(viewport);
        uiStage = new UiStage(viewport);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(menuStage);
        multiplexer.addProcessor(gameStage);
        Gdx.input.setInputProcessor(multiplexer);

        rayHandler = prepareLights();

        List<Tile> tiles = Board.loadTiles(resourcesService, "map.tmx");
        BoardService boardService = new BoardService(gameEventService, tiles);
        GameContext context = createContext(boardService);
        board = new Board(context, tiles);

        for (Tile tile : tiles) {
            tile.prepare(context);
        }

        gameStage.getRoot().addActorAt(0, board); // drawn below everything else

        createFeatures(context);
    }

    /**
     * Features run every frame, in this order. Camera tracking needs the player, so it is only created once the player exists.
     */
    private void createFeatures(GameContext context) {
        PlayerCreatedEvent playerCreatedEvent = gameEventService.poll(PlayerCreatedEvent.class);
        if (playerCreatedEvent != null) {
            addFeature(new CameraTrackingFeature(context, playerCreatedEvent.getPlayer()));
        }
        addFeature(new MenuFeature(context));
        addFeature(new StageFeature(context));
        if (Debug.isOn(FPS_LABEL)) {
            addFeature(new FPSFeature(context, new Vector2(300, 520)));
        }
    }

    private RayHandler prepareLights() {
        RayHandler rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.02f, 0.02f, 0.02f, 2 * 0.5f);
        return rayHandler;
    }

    private GameContext createContext(BoardService boardService) {
        PhraseService phraseService = new PhraseService();
        OrthographicCamera camera = (OrthographicCamera) gameStage.getCamera();

        return new GameContext(resourcesService,
                gameEventService,
                phraseService,
                world,
                rayHandler,
                camera,
                gameStage,
                notificationStage,
                menuStage,
                uiStage,
                boardService);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            gameStage.fadeOut();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            gameStage.fadeIn();
        }

        world.step(1 / 60f, 6, 2);
        gameStage.act(delta);
        gameStage.draw();

        rayHandler.setCombinedMatrix((OrthographicCamera) gameStage.getCamera());
        rayHandler.updateAndRender();

        notificationStage.act(delta);
        notificationStage.draw();

        menuStage.act(delta);
        menuStage.draw();

        uiStage.act(delta);
        uiStage.draw();

        for (Feature feature : features) {
            feature.run(delta);
        }

        gameEventService.updateEventsLivesAndClean();
    }

    protected void addFeature(Feature feature) {
        features.add(feature);
    }

    @Override
    public void resize(int width, int height) {
        gameStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        gameStage.dispose();
        notificationStage.dispose();
        menuStage.dispose();
        uiStage.dispose();
        rayHandler.dispose();
    }
}
