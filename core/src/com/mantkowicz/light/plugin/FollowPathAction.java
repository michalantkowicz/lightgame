package com.mantkowicz.light.plugin;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import java.util.List;

/**
 * Moves the actor along a polyline at a constant speed: no slowing down at the corners, no stop between segments.
 * The points are the actor's positions (bottom-left corner); the first one is where it starts.
 */
class FollowPathAction extends TemporalAction {
    private final List<Vector2> points;
    private final float[] distances;
    private final float length;

    FollowPathAction(List<Vector2> points, float duration) {
        super(duration);
        this.points = points;
        this.distances = new float[points.size()];
        float total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).dst(points.get(i));
            distances[i] = total;
        }
        this.length = total;
    }

    @Override
    protected void update(float percent) {
        float travelled = percent * length;
        int segment = 1;
        while (segment < points.size() - 1 && distances[segment] < travelled) {
            segment++;
        }
        Vector2 from = points.get(segment - 1);
        Vector2 to = points.get(segment);
        float segmentLength = distances[segment] - distances[segment - 1];
        float progress = segmentLength == 0 ? 1 : (travelled - distances[segment - 1]) / segmentLength;
        actor.setPosition(from.x + (to.x - from.x) * progress, from.y + (to.y - from.y) * progress);
    }
}
