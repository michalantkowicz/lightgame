package com.mantkowicz.light.game;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.actor.CollectibleFinder;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.board.Board;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.character.PlayerCreatedEvent;
import com.mantkowicz.light.character.SpriteActor;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.FeatureFlag;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.feature.CameraTrackingFeature;
import com.mantkowicz.light.feature.CollectFeature;
import com.mantkowicz.light.feature.CreatureFeature;
import com.mantkowicz.light.feature.DoorFeature;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.feature.GrassFeature;
import com.mantkowicz.light.feature.MenuFeature;
import com.mantkowicz.light.feature.NoPathFeature;
import com.mantkowicz.light.feature.StageFeature;
import com.mantkowicz.light.feature.Travel;
import com.mantkowicz.light.map.Maps;
import com.mantkowicz.light.map.TileObject;
import com.mantkowicz.light.map.TileSet;
import com.mantkowicz.light.resources.PhraseService;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.GameStage;
import com.mantkowicz.light.ui.NotificationStage;
import com.mantkowicz.light.ui.UiStage;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.menu.MenuStage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

/**
 * One map of the game (the farm, the chicken coop) with everything on it: its board, its stages, its lights, its own
 * event bus and its features. The game screen keeps every map the player (or an animal) has been to: each frame all of
 * them act, so a field keeps growing and the animals get hungry while the player is elsewhere, but only the one the
 * player is in is drawn (with its lights) and gets the taps.
 * <p>
 * Each map has a rabbit of its own, on the map only while the player is there; they all carry the same backpack and
 * stamina ({@link Player#Player(GameContext, Player)}), so the features of a map keep their rabbit.
 */
// baseline (docs/code-quality-plan.md): what GameScreen built for its one map, moved here with its PMD exception
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
final class Area {
    private static final float DEFAULT_AMBIENT_LIGHT = 0.02f;

    private final String mapName;
    private final Viewport viewport;
    private final World world;
    private final GameEventService gameEventService;
    private final GameStage gameStage;
    private final MenuStage menuStage;
    private final NotificationStage notificationStage;
    private final RayHandler rayHandler;
    private final Board board;
    private final GameContext context;
    private final boolean growingGrass;
    private final List<Feature> features = new ArrayList<>();
    private Player player;
    private CameraTrackingFeature camera;
    private CollectFeature collecting;
    private CreatureFeature creatures;
    private DoorFeature doors;

    /** Something that gives the features their HUD (one for the whole game, created with the first map's player). */
    interface HudProvider {
        Hud hud(GameContext context, Player player, IntPredicate throwOut);
    }

    /**
     * Builds the map and brings what is on it to life. Its features come with {@link #startFeatures}.
     *
     * @param elsewhere the player on another map (its backpack and stamina come here), or null for the first map, where
     *                  the map's PLAYER object makes the player
     */
    Area(String mapName, float cameraZoom, ResourcesService resourcesService, UiStage uiStage, Player elsewhere) {
        this.mapName = mapName;
        // the world (board, actors, menus and notifications next to them) moves and zooms with the camera;
        // the UI (buttons, windows) has its own fixed camera, so it stays in place and keeps its size
        viewport = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
        ((OrthographicCamera) viewport.getCamera()).zoom = cameraZoom;
        gameStage = new GameStage(viewport, resourcesService);
        menuStage = new MenuStage(viewport);
        notificationStage = new NotificationStage(viewport);
        world = new World(new Vector2(0, 0), true);
        gameEventService = new GameEventService();

        TileSet tileSet = Maps.loadTileSet(resourcesService, mapName);
        growingGrass = tileSet.getProperties().isGrowingGrass();
        rayHandler = prepareLights(tileSet.getProperties().getAmbientLight());

        List<Tile> tiles = tileSet.getTiles();
        BoardService boardService = new BoardService(gameEventService, tiles);
        // before anything is on the board: what steps onto a tile finds the collectibles there
        boardService.setOccupantListener(new CollectibleFinder(boardService, gameEventService));
        context = new GameContext(resourcesService, gameEventService, new PhraseService(), world, rayHandler,
                (OrthographicCamera) gameStage.getCamera(), gameStage, notificationStage, menuStage, uiStage,
                boardService);
        board = new Board(boardService, gameEventService, tiles);

        for (Tile tile : tiles) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileObject object) {
                    object.prepare(tile, context); // characters, lights... come to life
                }
            }
            tile.prepare(world);
        }

        gameStage.getRoot().addActorAt(0, board); // drawn below everything else

        PlayerCreatedEvent playerCreatedEvent = gameEventService.poll(PlayerCreatedEvent.class);
        if (playerCreatedEvent != null) {
            player = playerCreatedEvent.getPlayer();
        } else if (elsewhere != null) {
            player = new Player(context, elsewhere); // on the map once it comes through a door
        }
    }

    /**
     * box2dlights (without diffuse light) takes the brightness of unlit places from the ambient alpha and adds the
     * ambient colour on top, so a daylight map gets no colour and full alpha (a colour of 1 would paint everything white).
     *
     * @param ambientLight how bright the map is without lights (0 dark .. 1 daylight), {@code null} for the house's setting
     */
    private RayHandler prepareLights(Float ambientLight) {
        RayHandler lights = new RayHandler(world);
        if (ambientLight == null) {
            lights.setAmbientLight(DEFAULT_AMBIENT_LIGHT, DEFAULT_AMBIENT_LIGHT, DEFAULT_AMBIENT_LIGHT, 2 * 0.5f);
        } else {
            lights.setAmbientLight(0, 0, 0, ambientLight);
        }
        return lights;
    }

    /**
     * Features run every frame, in this order. Those of the player only on a map with a player: camera tracking needs
     * it.
     */
    void startFeatures(HudProvider hudProvider, Travel travel) {
        if (player != null) {
            camera = new CameraTrackingFeature(context, player);
            features.add(camera);
            collecting = new CollectFeature(context, player);
            features.add(collecting);
            Hud hud = hudProvider.hud(context, player, collecting::throwOut);
            creatures = new CreatureFeature(context, player, hud);
            features.add(creatures);
            if (FeatureFlag.GROWING_GRASS.isOn() && growingGrass) {
                features.add(new GrassFeature(context, player, hud));
            }
        }
        features.add(new MenuFeature(context, menuStage));
        features.add(new NoPathFeature(context));
        features.add(new StageFeature(context));
        doors = new DoorFeature(context, travel);
        features.add(doors);
    }

    /** The map the player is in: it acts and is drawn, with its lights, its notifications and its menu. */
    void actAndDraw(float delta) {
        world.step(1 / 60f, 6, 2);
        gameStage.act(delta);
        gameStage.draw();

        rayHandler.setCombinedMatrix((OrthographicCamera) gameStage.getCamera());
        rayHandler.updateAndRender();

        notificationStage.act(delta);
        notificationStage.draw();

        menuStage.act(delta);
        menuStage.draw();
    }

    /**
     * A map the player isn't in: everything on it acts (crops grow, animals live) and its features run, but nothing is
     * drawn and its lights aren't worked out (nothing in the game asks them).
     */
    void actBehindTheScenes(float delta) {
        world.step(1 / 60f, 6, 2);
        gameStage.act(delta);
        notificationStage.act(delta);
        menuStage.act(delta);
        runFeatures(delta);
    }

    /** The features, then the events of the frame are done. */
    void runFeatures(float delta) {
        for (Feature feature : features) {
            feature.run(delta);
        }
        gameEventService.updateEventsLivesAndClean();
    }

    // coming and going through doors

    /**
     * Where someone coming out of the door with this name lands: its tile when nothing stands there, else the nearest
     * free tile; null when there is no room at all.
     *
     * @throws IllegalStateException when the map has no such door (the maps don't match)
     */
    Tile arrivalTile(String doorName) {
        Tile door = doors.doorTile(doorName);
        if (door == null) {
            throw new IllegalStateException(mapName + " has no door named " + doorName);
        }
        BoardService boardService = context.getBoardService();
        List<Tile> steps = boardService.findSteps(door,
                tile -> tile.isAccessible() && boardService.getOccupants(tile).isEmpty(), Tile::isAccessible, false);
        if (steps == null) {
            return null;
        }
        return steps.isEmpty() ? door : steps.get(steps.size() - 1);
    }

    /** It comes onto this map on this tile, out of a door (the player: the camera is over it at once). */
    void welcome(GameBoardActor actor, Tile tile) {
        if (actor instanceof SpriteActor animal) {
            animal.comeOnto(tile); // it blocks its tile, as when it walks
        } else {
            actor.setTile(tile);
        }
        gameStage.addActor(actor);
        if (doors.isDoor(tile)) {
            doors.cameOut(actor);
        }
        if (actor == player) {
            camera.jumpTo(player);
        }
    }

    /**
     * It goes through a door to another map: off this one. When the player goes, the action buttons over things here
     * go at once (on the UI stage, they would show over the other map while fading).
     */
    void letGo(GameBoardActor actor, Stage uiStage) {
        actor.leaveBoard();
        if (actor == player) {
            Array<Actor> shown = uiStage.getActors();
            for (int i = shown.size - 1; i >= 0; i--) {
                if (shown.get(i) instanceof ActionButton button && button.isOver(gameStage)) {
                    button.remove();
                }
            }
        }
    }

    /** Is it on this map now? */
    boolean has(Actor actor) {
        return actor.getStage() == gameStage;
    }

    // for the game screen and the HUD

    String getMapName() {
        return mapName;
    }

    GameContext getContext() {
        return context;
    }

    /** This map's rabbit (on the map only while the player is here), or null on a map without a player. */
    Player getPlayer() {
        return player;
    }

    GameStage getGameStage() {
        return gameStage;
    }

    MenuStage getMenuStage() {
        return menuStage;
    }

    GameEventService getGameEventService() {
        return gameEventService;
    }

    /** Throws the item in a backpack slot out next to the player (see the HUD); false without a player. */
    boolean throwOut(int slot) {
        return collecting != null && collecting.throwOut(slot);
    }

    /** Gives the item in a backpack slot to an animal next to the player wishing for it (see the HUD). */
    boolean giveFrom(int slot) {
        return creatures != null && creatures.giveFrom(slot);
    }

    void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    void dispose() {
        gameStage.dispose();
        notificationStage.dispose();
        menuStage.dispose();
        rayHandler.dispose();
        world.dispose();
    }
}
