package com.mantkowicz.light.notification;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

public class Notification extends Group {
    private final NotificationType notificationType;
    private final Label label;

    public Notification(NotificationType notificationType, Label label) {
        this.notificationType = notificationType;
        this.label = label;

        addActor(label);
        label.setAlignment(Align.center);
        setSize(label.getWidth(), label.getHeight());
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getActions().size <= 0) {
            remove();
        }
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public Label getLabel() {
        return label;
    }

    public void setCenterAt(Vector2 position) {
        position.sub(this.getWidth() / 2f, this.getHeight() / 2f);
        setPosition(position.x, position.y);
    }

    public Vector2 getCenter() {
        return new Vector2(getX(Align.center), getY(Align.center));
    }
}
