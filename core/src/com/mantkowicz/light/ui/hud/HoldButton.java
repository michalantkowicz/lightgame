package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * A button that works only after it is held for {@link #SECONDS}, so small children don't press it by accident (the
 * settings gear). While it is held a ring of dots around it lights up clockwise from the top; letting go early empties
 * it.
 */
public class HoldButton extends Group {
    public static final float SECONDS = 3;
    private static final float EMPTYING_SPEED = 3;
    private final TextureRegion icon;
    private static final int DOTS = 16;
    private final TextureRegion dot, emptyDot;
    private final float dotScale, radius;
    private final Runnable action;
    private boolean held;
    private float progress;

    public HoldButton(ResourcesService resources, UiImage icon, float scale, Runnable action) {
        this.icon = resources.getUiImage(icon);
        this.dot = resources.getUiImage(UiImage.RING_DOT);
        this.emptyDot = resources.getUiImage(UiImage.RING_DOT_EMPTY);
        this.action = action;
        setSize(this.icon.getRegionWidth() * scale, this.icon.getRegionHeight() * scale);
        dotScale = scale - 1;
        radius = Math.max(getWidth(), getHeight()) / 2f + dot.getRegionWidth() * dotScale / 2f + 14;
        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                held = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                held = false;
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (held) {
            progress = Math.min(1, progress + delta / SECONDS);
            if (progress >= 1) {
                held = false;
                progress = 0;
                action.run();
            }
        } else {
            progress = Math.max(0, progress - delta * EMPTYING_SPEED);
        }
    }

    /** How full the ring is: 0 (not held) .. 1 (done). */
    public float getProgress() {
        return progress;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1, 1, 1, parentAlpha * getColor().a);
        if (progress > 0) {
            int lit = Math.round(progress * DOTS);
            for (int i = 0; i < DOTS; i++) {
                TextureRegion region = i < lit ? dot : emptyDot;
                double angle = Math.PI / 2 - 2 * Math.PI * i / DOTS; // clockwise from the top
                float w = region.getRegionWidth() * dotScale, h = region.getRegionHeight() * dotScale;
                float cx = getX() + getWidth() / 2f + (float) Math.cos(angle) * radius;
                float cy = getY() + getHeight() / 2f + (float) Math.sin(angle) * radius;
                batch.draw(region, Math.round(cx - w / 2f), Math.round(cy - h / 2f), w, h);
            }
        }
        batch.draw(icon, getX(), getY(), getWidth(), getHeight());
        super.draw(batch, parentAlpha);
    }
}
