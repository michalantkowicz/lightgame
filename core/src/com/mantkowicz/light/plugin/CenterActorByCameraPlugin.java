package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mantkowicz.light.actor.GameActor;

/**
 * Keeps the actor centred on its stage's camera, moved by an offset. On the UI stage that camera stays in the middle of
 * the screen, so the actor stays in place on the screen (also after a resize).
 */
public class CenterActorByCameraPlugin implements Plugin {
    private final GameActor actor;
    private final Vector2 offset;

    public CenterActorByCameraPlugin(GameActor actor, Vector2 offset) {
        this.actor = actor;
        this.offset = offset;
    }

    @Override
    public void run(float delta) {
        if (actor.getStage() != null) {
            actor.setPosition(getPosition(actor.getStage().getCamera()));
        }
    }

    private Vector2 getPosition(Camera camera) {
        Vector3 cameraPosition = camera.position;
        return new Vector2(cameraPosition.x, cameraPosition.y).sub(actor.getWidth() / 2f, actor.getHeight() / 2f)
                .sub(offset);
    }
}
