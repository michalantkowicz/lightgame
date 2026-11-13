package com.mantkowicz.light.notification;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mantkowicz.light.resources.Palette;

public class LabelFactory {
    public static Label createLabel(BitmapFont font, NotificationStyle notificationStyle, String text) {
        return createLabel(font, notificationStyle, null, text);
    }

    /**
     * @param font       usually {@code resourcesService.getFont()}
     * @param background drawn behind the text, or {@code null} for none
     */
    public static Label createLabel(BitmapFont font, NotificationStyle notificationStyle, Drawable background,
            String text) {
        LabelStyle labelStyle = new LabelStyle();
        labelStyle.background = background;
        labelStyle.font = font;
        switch (notificationStyle) {
            case DEFAULT :
                labelStyle.fontColor = Color.WHITE;
                break;
            case WARNING :
                labelStyle.fontColor = Color.FIREBRICK;
                break;
            case ON_PANEL :
                labelStyle.fontColor = Palette.TEXT;
                break;
            default :
                throw new IllegalArgumentException(
                        "There is no style defined for notification style: " + notificationStyle.name());
        }
        return new Label(text, labelStyle);
    }
}
