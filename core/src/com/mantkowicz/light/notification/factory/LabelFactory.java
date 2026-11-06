package com.mantkowicz.light.notification.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mantkowicz.light.notification.NotificationStyle;

public class LabelFactory {
    public static Label createLabel(NotificationStyle notificationStyle, String text) {
        return createLabel(notificationStyle, null, text);
    }

    /**
     * @param background drawn behind the text, or {@code null} for none
     */
    public static Label createLabel(NotificationStyle notificationStyle, Drawable background, String text) {
        LabelStyle labelStyle = new LabelStyle();
        labelStyle.background = background;
        labelStyle.font = new BitmapFont(Gdx.files.internal("font.fnt"));
        switch (notificationStyle) {
            case DEFAULT:
                labelStyle.fontColor = Color.WHITE;
                break;
            case WARNING:
                labelStyle.fontColor = Color.FIREBRICK;
                break;
            default:
                throw new IllegalArgumentException("There is no style defined for notification style: " + notificationStyle.name());
        }
        return new Label(text, labelStyle);
    }
}
