package com.mantkowicz.light.feature.implementation;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.implementation.CameraActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.feature.Feature;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.camera.LookAtMeEvent;


public class CameraTrackingFeature implements Feature {
    private static final float DISTANCE_LEFT_RIGHT = 420;
    private static final float DISTANCE_TOP_BOTTOM = 420;

    private final CameraActor cameraActor;
    private final GameEventService gameEventService;
    private GameBoardActor actor;
    private Vector2 interval = new Vector2();

    public CameraTrackingFeature(GameContext context, GameBoardActor actor) {
        this.actor = actor;
        this.gameEventService = context.getGameEventService();

        this.cameraActor = new CameraActor(context.getCamera());
        cameraActor.setPosition(actor.getCenter());
//        cameraActor.setDebug(DISTANCE_LEFT_RIGHT, DISTANCE_TOP_BOTTOM);
        context.getGameStage().addActor(cameraActor);
    }

    @Override
    public void run(float delta) {
        if (gameEventService.has(LookAtMeEvent.class)) {
            handleLookAtMeEvent();
        } else if (cameraActor.isNotCentering() && isActorSet()) {
            Vector2 actorCenter = actor.getCenter();
            if (shouldCameraMove(actorCenter)) {
                moveCamera(actorCenter);
            } else {
                gentlyStopCameraIfNecessary();
            }
        }
    }

    private void handleLookAtMeEvent() {
        LookAtMeEvent event = gameEventService.poll(LookAtMeEvent.class);
        GameBoardActor boardGameActor = event.getActor();
        cameraActor.centerAt(boardGameActor.getCenter());

        this.actor = boardGameActor;
    }

    private void gentlyStopCameraIfNecessary() {
        if (interval.len2() > 1f) {
            cameraActor.moveBy(interval);
            interval = interval.scl(0.96f);
        }
    }

    private void moveCamera(Vector2 actorCenter) {
        interval = actorCenter.sub(cameraActor.getCenter()).scl(0.02f);
        cameraActor.moveBy(interval);
    }

    private boolean isActorSet() {
        return actor != null;
    }

    private boolean shouldCameraMove(Vector2 actorCenter) {
        Vector2 cameraCenter = cameraActor.getCenter();
        float horizontalInterval = DISTANCE_LEFT_RIGHT / 2f;
        float verticalInterval = DISTANCE_TOP_BOTTOM / 2f;

        return (actorCenter.x < cameraCenter.x - horizontalInterval ||
                actorCenter.x > cameraCenter.x + horizontalInterval ||
                actorCenter.y < cameraCenter.y - verticalInterval ||
                actorCenter.y > cameraCenter.y + verticalInterval);
    }
}
