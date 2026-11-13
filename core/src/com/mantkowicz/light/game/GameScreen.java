package com.mantkowicz.light.game;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.board.Board;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.PlayerCreatedEvent;
import com.mantkowicz.light.feature.CameraTrackingFeature;
import com.mantkowicz.light.feature.CollectFeature;
import com.mantkowicz.light.feature.FPSFeature;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.feature.MenuFeature;
import com.mantkowicz.light.feature.NoPathFeature;
import com.mantkowicz.light.feature.StageFeature;
import com.mantkowicz.light.map.TileObject;
import com.mantkowicz.light.map.TileSet;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.GameStage;
import com.mantkowicz.light.ui.MenuStage;
import com.mantkowicz.light.ui.NotificationStage;
import com.mantkowicz.light.ui.UiStage;
import com.mantkowicz.light.ui.hud.Hud;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.debug.Debug.Tool.FPS_LABEL;
import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

// baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
public class GameScreen extends ScreenAdapter {
    private static final float DEFAULT_AMBIENT_LIGHT = 0.02f;

    private final String mapName;
    private final float cameraZoom;
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

    public GameScreen(String mapName, float cameraZoom, ResourcesService resourcesService,
            GameEventService gameEventService,
            World world) {
        this.mapName = mapName;
        this.cameraZoom = cameraZoom;
        this.resourcesService = resourcesService;
        this.gameEventService = gameEventService;
        this.world = world;
    }

    @Override
    public void show() {
        // the world (board, actors, menus and notifications next to them) moves and zooms with the camera;
        // the UI (buttons, windows) has its own fixed camera, so it stays in place and keeps its size
        Viewport worldViewport = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
        ((OrthographicCamera) worldViewport.getCamera()).zoom = cameraZoom;
        Viewport uiViewport = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
        gameStage = new GameStage(worldViewport, resourcesService);
        menuStage = new MenuStage(worldViewport);
        notificationStage = new NotificationStage(worldViewport);
        uiStage = new UiStage(uiViewport);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(menuStage);
        multiplexer.addProcessor(gameStage);
        Gdx.input.setInputProcessor(multiplexer);

        TileSet tileSet = Board.loadTileSet(resourcesService, mapName);
        rayHandler = prepareLights(tileSet.getProperties().getAmbientLight());

        List<Tile> tiles = tileSet.getTiles();
        BoardService boardService = new BoardService(gameEventService, tiles);
        GameContext context = createContext(boardService);
        board = new Board(boardService, gameEventService, tiles);

        for (Tile tile : tiles) {
            for (TileObject object : tile.getTileObjects()) {
                object.prepare(tile, context); // characters, lights... come to life
            }
            tile.prepare(world);
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
            Player player = playerCreatedEvent.getPlayer();
            addFeature(new CameraTrackingFeature(context, player));
            CollectFeature collecting = new CollectFeature(context, player);
            addFeature(collecting);
            uiStage.addActor(new Hud(context, player, collecting::throwOut));
        }
        addFeature(new MenuFeature(context, menuStage));
        addFeature(new NoPathFeature(context));
        addFeature(new StageFeature(context));
        if (Debug.isOn(FPS_LABEL)) {
            addFeature(new FPSFeature(context, new Vector2(300, 520)));
        }
    }

    /**
     * box2dlights (without diffuse light) takes the brightness of unlit places from the ambient alpha and adds the
     * ambient colour on top, so a daylight map gets no colour and full alpha (a colour of 1 would paint everything white).
     *
     * @param ambientLight how bright the map is without lights (0 dark .. 1 daylight), {@code null} for the house's setting
     */
    private RayHandler prepareLights(Float ambientLight) {
        RayHandler rayHandler = new RayHandler(world);
        if (ambientLight == null) {
            rayHandler.setAmbientLight(DEFAULT_AMBIENT_LIGHT, DEFAULT_AMBIENT_LIGHT, DEFAULT_AMBIENT_LIGHT, 2 * 0.5f);
        } else {
            rayHandler.setAmbientLight(0, 0, 0, ambientLight);
        }
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

        AnimatedTiledMapTile.updateAnimationBaseTime(); // animated map tiles (water)
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
        uiStage.getViewport().update(width, height, true);
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
