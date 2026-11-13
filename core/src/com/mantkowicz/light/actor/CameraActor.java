package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;

import static com.mantkowicz.light.actor.GameActorType.CAMERA_ACTOR;

public class CameraActor extends GameActor {
    private final OrthographicCamera camera;

    public CameraActor(OrthographicCamera camera) {
        super(CAMERA_ACTOR);
        this.camera = camera;

        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        camera.position.set(getX(), getY(), camera.position.z);
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        camera.position.set(getX(), getY(), camera.position.z);
    }

    public void setDebug(float distanceLeftRight, float distanceTopBottom) {
        Actor debugActor = new Actor();
        addActor(debugActor);
        debugActor.setSize(distanceLeftRight, distanceTopBottom);
        debugActor.setPosition(debugActor.getX() - debugActor.getWidth() / 2f,
                debugActor.getY() - debugActor.getHeight() / 2f);
        debugActor.setTouchable(Touchable.disabled);
        debugActor.debug();
    }

    public void centerAt(Vector2 target) {
        if (isNotCentering()) {
            MoveToAction moveToTargetAction = Actions.moveTo(target.x, target.y, 1f, Interpolation.pow2Out);
            addAction(moveToTargetAction);
        }
    }

    public void centerAtWithZoom(Vector2 target, float zoom) {
        centerAt(target);
        camera.zoom = zoom;
    }

    public boolean isNotCentering() {
        return getActions().size == 0;
    }
}
