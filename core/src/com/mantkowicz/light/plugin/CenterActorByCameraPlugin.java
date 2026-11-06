package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.game.GameContext;

/**
 * Keeps the actor centred on the camera, moved by an offset.
 */
public class CenterActorByCameraPlugin implements Plugin {
    private final GameActor actor;
    private final Camera camera;
    private final Vector2 offset;

    public CenterActorByCameraPlugin(GameActor actor, GameContext context, Vector2 offset) {
        this.actor = actor;
        this.camera = context.getCamera();
        this.offset = offset;
    }

    @Override
    public void run(float delta) {
        actor.setPosition(getPosition());
    }

    private Vector2 getPosition() {
        Vector3 cameraPosition = camera.position;
        return new Vector2(cameraPosition.x, cameraPosition.y).sub(actor.getWidth() / 2f, actor.getHeight() / 2f).sub(offset);
    }
}
