package com.mantkowicz.light.game;

import com.badlogic.gdx.Gdx;
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
import com.mantkowicz.light.light.DayClock;
import com.mantkowicz.light.light.DayNightSettings;
import com.mantkowicz.light.map.TileDoor;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.UiStage;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.TimeOfDay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mantkowicz.light.debug.Debug.Tool.FPS_LABEL;
import static com.mantkowicz.light.game.Main.SCREEN_HEIGHT;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

/**
 * The game: the maps ({@link Area}: the farm, the chicken coop...) and the UI over them. It starts on one map; a door
 * (Tiled's DOOR object) leads to another. Every map a chain of doors leads to is built at the start and lives on: every
 * map acts each frame, the one the player is in is also drawn and gets the taps. The player goes through a door behind
 * a short curtain (the screen darkens and lightens again, {@link #CURTAIN_SECONDS} each way); an animal just comes out
 * on the other side.
 * <p>
 * Day and night are the same on every map: one {@link DayClock} (set in {@link DayNightSettings}), which each map's
 * {@code DayNightFeature} follows.
 */
public class GameScreen extends ScreenAdapter {
    /** How long the screen takes to darken, and then to lighten again, when the player goes through a door. */
    public static final float CURTAIN_SECONDS = 0.3f;

    private final String mapName;
    private final float cameraZoom;
    private final ResourcesService resourcesService;
    /** Every map, by its file name; the first is the one the game starts on. */
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
    /** The time of day, for every map. */
    private DayClock clock;

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

        clock = new DayClock(DayNightSettings.current());
        active = build(mapName);
        buildTheMapsBehindDoors();
        listenTo(active);
        if (hud != null) {
            hud.listenTo(active.getGameEventService());
        }

        if (Debug.isOn(FPS_LABEL)) {
            addFeature(new FPSFeature(active.getContext(), new Vector2(300, 520)));
        }
        curtain = Curtain.create();
    }

    /**
     * Every map the doors lead to, and the maps their doors lead to, and so on: all built now, so none is built in the
     * middle of the game (a hitch) and all of them live from the start.
     */
    private void buildTheMapsBehindDoors() {
        List<Area> toLookAt = new ArrayList<>(areas.values());
        for (int i = 0; i < toLookAt.size(); i++) {
            for (String target : toLookAt.get(i).doorTargets()) {
                if (!areas.containsKey(target)) {
                    toLookAt.add(build(target));
                }
            }
        }
    }

    /** The map a door leads to: built at the start, as every map the doors lead to ({@link #buildTheMapsBehindDoors}). */
    private Area behind(TileDoor door) {
        return areas.get(door.getTargetMap());
    }

    /** Screen-wide: runs every frame after the maps, whatever map the player is in. */
    protected void addFeature(Feature feature) {
        features.add(feature);
    }

    /** Builds the map with this file name and brings it to life; only at the start ({@link #show}). */
    private Area build(String name) {
        Player elsewhere = active == null ? null : active.getPlayer();
        Area area = new Area(name, cameraZoom, resourcesService, uiStage, elsewhere);
        areas.put(name, area);
        area.startFeatures(this::hud, this::travel, clock);
        area.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return area;
    }

    /**
     * The HUD, made with the first map's player; every map's features share it. A backpack slot throws out and gives
     * away on the map the player is in.
     */
    private Hud hud(GameContext context, Player player) {
        if (hud == null) {
            hud = new Hud(context, player, slot -> active.throwOut(slot));
            hud.setGiveAway(slot -> active.giveFrom(slot));
            hud.addActor(new TimeOfDay(resourcesService, clock, hud.getGear())); // the day clock at the top left
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
     * map at once. The player goes behind the curtain: the screen darkens, the player comes out on the other map, the
     * screen lightens; taps wait meanwhile.
     */
    private boolean travel(GameBoardActor traveller, TileDoor door) {
        Area from = areaOf(traveller);
        if (from == null) {
            return false;
        }
        if (traveller instanceof Player) {
            return from == active && startJourney(door);
        }
        Area to = behind(door);
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

    /**
     * The curtain comes down; with no room on the other side, it doesn't, and the player stays on the door until it has
     * walked off (and may try again).
     */
    private boolean startJourney(TileDoor door) {
        if (curtain.isDrawn()) {
            return false; // on its way already
        }
        if (behind(door).arrivalTile(door.getName()) == null) {
            active.stayOnDoor(active.getPlayer());
            return false;
        }
        multiplexer.clear(); // no taps until the player is through
        curtain.fall(uiStage, CURTAIN_SECONDS, () -> arrivingThrough = door, () -> listenTo(active));
        return true;
    }

    /**
     * Behind the curtain: the player leaves this map and comes out of the door with the same name on the other one,
     * which is drawn from now on. With no room there (taken while the curtain came down), it stays on the door until it
     * has walked off.
     */
    private void arrive(TileDoor door) {
        Area to = behind(door);
        Tile tile = to.arrivalTile(door.getName());
        if (tile == null || to == active) {
            active.stayOnDoor(active.getPlayer());
            return;
        }
        active.letGo(active.getPlayer(), uiStage);
        to.welcome(to.getPlayer(), tile);
        active = to;
        hud.listenTo(to.getGameEventService());
    }

    @Override
    public void render(float delta) {
        if (arrivingThrough != null) {
            TileDoor door = arrivingThrough;
            arrivingThrough = null;
            arrive(door);
        }
        AnimatedTiledMapTile.updateAnimationBaseTime(); // animated map tiles (water)
        clock.update(delta); // before the maps: their DayNightFeature follows it
        active.actAndDraw(delta);

        uiStage.act(delta);
        uiStage.draw();

        for (Area area : areas.values()) {
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
