package com.mantkowicz.light.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.mantkowicz.light.screen.GameScreen;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.resources.ResourcesService;

public class Main extends Game {
    public static final float SCREEN_WIDTH = 800f, SCREEN_HEIGHT = 1280f;

    private ResourcesService resourcesService;
    private World world;

    @Override
    public void create() {
        resourcesService = new ResourcesService();
        GameEventService gameEventService = new GameEventService();

        Box2D.init();
        world = new World(new Vector2(0, 0), true);

        setScreen(new GameScreen(resourcesService, gameEventService, world));
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
