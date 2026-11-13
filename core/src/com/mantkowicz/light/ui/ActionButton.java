package com.mantkowicz.light.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.RemoveWhenUnavailablePlugin;
import com.mantkowicz.light.ui.hud.Hud;

import java.util.function.BooleanSupplier;

import static com.mantkowicz.light.actor.GameActorType.ACTION_BUTTON;

/**
 * A {@link RoundButton} with a picture of what it acts on (e.g. the chest to collect from), right of the HUD's face,
 * over the HUD's hint button. It stays while its action is available and fades out when it no longer is (and fades
 * back in if it becomes available again before it is gone). Its name (e.g. "COLLECT") is for tests only: players see
 * the picture.
 */
public class ActionButton extends GameActor {
    public ActionButton(GameContext context, String name, TextureRegion picture, EventListener listener,
            BooleanSupplier available) {
        super(ACTION_BUTTON);
        setName(name);
        RoundButton button = new RoundButton(context.getResourcesService(), picture);
        addActor(button);
        setSize(button.getWidth(), button.getHeight());
        setTouchable(Touchable.enabled);
        addListener(listener);

        addPlugin(new RemoveWhenUnavailablePlugin(this, available));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null) {
            // right of the face, on the screen's bottom (the UI camera stays in the middle of the screen)
            Camera camera = getStage().getCamera();
            setPosition(camera.position.x + Hud.ACTION_RIGHT - getWidth(),
                    camera.position.y - getStage().getViewport().getWorldHeight() / 2f + Hud.ACTION_Y);
        }
    }

    /** Not removed yet (possibly fading out). */
    public boolean isOnStage() {
        return getStage() != null;
    }
}
