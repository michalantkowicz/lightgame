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
 * settings gear). While it is held a circle at its top right fills (the pack's stamina circle: empty, filling, then a
 * green check), a little outside the button so a finger on it doesn't hide it; letting go early empties it.
 */
public class HoldButton extends Group {
    public static final float SECONDS = 3;
    private static final float EMPTYING_SPEED = 3;
    private final TextureRegion icon;
    /** The circle's pack pixels per screen pixel, and how far its middle is out from the button's top right corner. */
    private static final float CIRCLE_SCALE = 3, CIRCLE_OUT = 8;
    private final TextureRegion[] circle;
    private final Runnable action;
    private boolean held;
    private float progress;

    public HoldButton(ResourcesService resources, UiImage icon, float scale, Runnable action) {
        this.icon = resources.getUiImage(icon);
        this.circle = resources.getUiAnimation(UiImage.HOLD_CIRCLE).getKeyFrames();
        this.action = action;
        setSize(this.icon.getRegionWidth() * scale, this.icon.getRegionHeight() * scale);
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

    /** How full the circle is: 0 (not held) .. 1 (done). */
    public float getProgress() {
        return progress;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1, 1, 1, parentAlpha * getColor().a);
        batch.draw(icon, getX(), getY(), getWidth(), getHeight());
        if (progress > 0) {
            TextureRegion frame = circle[Math.round(progress * (circle.length - 1))];
            float w = frame.getRegionWidth() * CIRCLE_SCALE, h = frame.getRegionHeight() * CIRCLE_SCALE;
            float cx = getX() + getWidth() + CIRCLE_OUT, cy = getY() + getHeight() + CIRCLE_OUT;
            batch.draw(frame, Math.round(cx - w / 2f), Math.round(cy - h / 2f), w, h);
        }
        super.draw(batch, parentAlpha);
    }
}
