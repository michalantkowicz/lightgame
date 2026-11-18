package com.mantkowicz.light.actor;

import com.badlogic.gdx.math.Vector2;
import com.mantkowicz.light.event.GameEvent;

/** The camera centres on an actor (or on a point near it) and follows that actor from then on. */
public class LookAtMeEvent extends GameEvent {
    private final GameBoardActor actor;
    private final Vector2 point;

    public LookAtMeEvent(GameBoardActor actor) {
        this(actor, null);
    }

    /** @param point where the camera centres (e.g. kept off the map's edge), or null: on the actor */
    public LookAtMeEvent(GameBoardActor actor, Vector2 point) {
        this.actor = actor;
        this.point = point;
    }

    public GameBoardActor getActor() {
        return actor;
    }

    /** Where the camera centres: the given point, else the actor's middle. */
    public Vector2 getPoint() {
        return point != null ? point.cpy() : actor.getCenter();
    }
}
