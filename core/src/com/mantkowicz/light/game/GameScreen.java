package com.mantkowicz.light.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.feature.FPSFeature;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.map.TileDoor;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.UiStage;
import com.mantkowicz.light.ui.hud.Hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import static com.mantkowicz.light.debug.Debug.Tool.FPS_LABEL;
import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

/**
 * The game: the maps ({@link Area}: the farm, the chicken coop...) and the UI over them. It starts on one map; a door
 * (Tiled's DOOR object) leads to another, which is built the first time someone goes through and lives on from then
 * on: every map acts each frame, the one the player is in is also drawn and gets the taps. The player goes through a
 * door behind a short curtain (the screen darkens and lightens again, {@link #CURTAIN_SECONDS} each way); an animal just
 * comes out on the other side.
 */
public class GameScreen extends ScreenAdapter {
    /** How long the screen takes to darken, and then to lighten again, when the player goes through a door. */
    public static final float CURTAIN_SECONDS = 0.3f;

    private final String mapName;
    private final float cameraZoom;
    private final ResourcesService resourcesService;
    /** Every map someone has been to, by its file name; the first is the one the game starts on. */
    private final Map<String, Area> areas = new LinkedHashMap<>();
    /** Screen-wide, whatever the map (the FPS label). */
    private final List<Feature> features = new ArrayList<>();
    private InputMultiplexer multiplexer;
    private UiStage uiStage;
    private Hud hud;
    /** The map the player is in. */
    private Area active;
    /** Over the screen while the player goes through a door. */
    private Curtain curtain;
    /** The door the player goes through, once the screen is dark (the next frame takes it there); else null. */
    private TileDoor arrivingThrough;

    public GameScreen(String mapName, float cameraZoom, ResourcesService resourcesService) {
        this.mapName = mapName;
        this.cameraZoom = cameraZoom;
        this.resourcesService = resourcesService;
    }

    @Override
    public void show() {
        uiStage = new UiStage(new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT));
        multiplexer = new AnyButtonInput(); // the right mouse button does what the left does
        Gdx.input.setInputProcessor(multiplexer);

        active = area(mapName);
        listenTo(active);
        if (hud != null) {
            hud.enter(active.getGameEventService(), active::throwOut, active::giveFrom);
        }

        if (Debug.isOn(FPS_LABEL)) {
            features.add(new FPSFeature(active.getContext(), new Vector2(300, 520)));
        }
        curtain = Curtain.create();
    }

    /** The map with this file name: built (and brought to life) the first time someone goes there. */
    private Area area(String name) {
        Area area = areas.get(name);
        if (area == null) {
            Player elsewhere = active == null ? null : active.getPlayer();
            area = new Area(name, cameraZoom, resourcesService, uiStage, elsewhere);
            areas.put(name, area);
            area.startFeatures(this::hud, this::travel);
            area.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        return area;
    }

    /** The HUD, made with the first map's player; every map's features share it. */
    private Hud hud(GameContext context, Player player, IntPredicate throwOut) {
        if (hud == null) {
            hud = new Hud(context, player, throwOut);
            uiStage.addActor(hud);
        }
        return hud;
    }

    /** Taps go to the UI, then to this map's menu and the map itself. */
    private void listenTo(Area area) {
        multiplexer.clear();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(area.getMenuStage());
        multiplexer.addProcessor(area.getGameStage());
    }

    // going through doors

    /**
     * Someone ended its walk on a door that lets it through (see {@code DoorFeature}). An animal comes out on the other
     * map at once (that map is built if nobody has been there yet). The player goes behind the curtain: the screen
     * darkens, the player comes out on the other map, the screen lightens; taps wait meanwhile.
     */
    private boolean travel(GameBoardActor traveller, TileDoor door) {
        Area from = areaOf(traveller);
        if (from == null) {
            return false;
        }
        if (traveller instanceof Player) {
            return from == active && startJourney(door);
        }
        Area to = area(door.getTargetMap());
        Tile tile = to.arrivalTile(door.getName());
        if (tile == null) {
            return false; // no room on the other side: it stays (and may try again)
        }
        from.letGo(traveller, uiStage);
        to.welcome(traveller.moveTo(to.getContext()), tile);
        return true;
    }

    private Area areaOf(GameBoardActor actor) {
        for (Area area : areas.values()) {
            if (area.has(actor)) {
                return area;
            }
        }
        return null;
    }

    private boolean startJourney(TileDoor door) {
        if (curtain.isDrawn()) {
            return false; // on its way already
        }
        multiplexer.clear(); // no taps until the player is through
        curtain.fall(uiStage, CURTAIN_SECONDS, () -> arrivingThrough = door, () -> listenTo(active));
        return true;
    }

    /**
     * Behind the curtain: the player leaves this map and comes out of the door with the same name on the other one,
     * which is drawn from now on. With no room there, it stays where it is.
     */
    private void arrive(TileDoor door) {
        Area to = area(door.getTargetMap());
        Tile tile = to.arrivalTile(door.getName());
        if (tile == null || to == active) {
            return;
        }
        active.letGo(active.getPlayer(), uiStage);
        to.welcome(to.getPlayer(), tile);
        active = to;
        hud.enter(to.getGameEventService(), to::throwOut, to::giveFrom);
    }

    @Override
    public void render(float delta) {
        if (arrivingThrough != null) {
            TileDoor door = arrivingThrough;
            arrivingThrough = null;
            arrive(door);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            active.getGameStage().fadeOut();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            active.getGameStage().fadeIn();
        }

        AnimatedTiledMapTile.updateAnimationBaseTime(); // animated map tiles (water)
        active.actAndDraw(delta);

        uiStage.act(delta);
        uiStage.draw();

        // the other maps before this one's features: what they tell the shared HUD (e.g. no hint next to an animal,
        // as their rabbit isn't there) is overridden by what this map tells it
        for (Area area : new ArrayList<>(areas.values())) { // a copy: an animal may open a new map meanwhile
            if (area != active) {
                area.actBehindTheScenes(delta);
            }
        }
        active.runFeatures(delta);
        for (Feature feature : features) {
            feature.run(delta);
        }
    }

    @Override
    public void resize(int width, int height) {
        for (Area area : areas.values()) {
            area.resize(width, height);
        }
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        for (Area area : areas.values()) {
            area.dispose();
        }
        uiStage.dispose();
        curtain.dispose();
    }
}
