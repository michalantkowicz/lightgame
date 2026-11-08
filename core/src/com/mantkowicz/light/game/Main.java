package com.mantkowicz.light.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.ResourcesService;

public class Main extends Game {
    public static final float SCREEN_WIDTH = 800f, SCREEN_HEIGHT = 1280f;
    /** The level the game starts with (made in Tiled, packed by tools/assets). */
    public static final String DEFAULT_MAP = "maps/level1.tmx";
    /**
     * How close the camera is to the world: 1 = as designed, below 1 = closer (0.5 = everything twice as big), above 1 =
     * see more. Only the world zooms; buttons and windows keep their size.
     */
    public static final float CAMERA_ZOOM = 1.5f;

    private final String mapName;
    private final float cameraZoom;

    private ResourcesService resourcesService;
    private World world;

    public Main() {
        this(DEFAULT_MAP);
    }

    public Main(String mapName) {
        this(mapName, CAMERA_ZOOM);
    }

    /**
     * The default level with another camera zoom, e.g. a platform's own (see the HTML and Android launchers).
     */
    public Main(float cameraZoom) {
        this(DEFAULT_MAP, cameraZoom);
    }

    /**
     * @param cameraZoom see {@link #CAMERA_ZOOM}; the tests pass their own, so changing the game's doesn't affect them
     */
    public Main(String mapName, float cameraZoom) {
        this.mapName = mapName;
        this.cameraZoom = cameraZoom;
    }

    @Override
    public void create() {
        resourcesService = new ResourcesService();
        GameEventService gameEventService = new GameEventService();

        Box2D.init();
        world = new World(new Vector2(0, 0), true);

        setScreen(new GameScreen(mapName, cameraZoom, resourcesService, gameEventService, world));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        world.dispose();
        resourcesService.dispose();
    }
}
