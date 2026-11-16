package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.notification.NotificationStyle;

import static com.badlogic.gdx.Gdx.graphics;
import static com.mantkowicz.light.notification.LabelFactory.createLabel;
import static java.lang.String.valueOf;

/** Frames per second in a small black box on the UI stage (Debug.Tool.FPS_LABEL). */
public class FPSFeature implements Feature {
    /** Around the text, inside the black box. */
    private static final float PAD_X = 6, PAD_Y = 2;

    private final Vector2 offset;
    private final Camera camera;
    private final Label fpsLabel;

    public FPSFeature(GameContext context, Vector2 offset) {
        this.offset = offset;
        // on the UI stage: it stays in place and keeps its size whatever the world camera does
        camera = context.getUiStage().getCamera();
        fpsLabel = createLabel(context.getResourcesService().getFont(), NotificationStyle.DEFAULT, blackBox(), "");
        context.getUiStage().addActor(fpsLabel);
    }

    @Override
    public void run(float delta) {
        fpsLabel.toFront();
        fpsLabel.setPosition(camera.position.x + offset.x, camera.position.y + offset.y);
        fpsLabel.setText(valueOf(graphics.getFramesPerSecond()));
        fpsLabel.pack(); // not in a table: it sizes itself to the text, and its box with it
    }

    /** A black box behind the text: a stretched black pixel, with some room around the text. */
    private static TextureRegionDrawable blackBox() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.BLACK);
        pixel.fill();
        TextureRegionDrawable box = new TextureRegionDrawable(new TextureRegion(new Texture(pixel)));
        pixel.dispose();
        box.setLeftWidth(PAD_X);
        box.setRightWidth(PAD_X);
        box.setTopHeight(PAD_Y);
        box.setBottomHeight(PAD_Y);
        return box;
    }
}
