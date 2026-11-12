package com.mantkowicz.light.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.CenterActorByCameraPlugin;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.plugin.RemoveWhenUnavailablePlugin;

import java.util.function.BooleanSupplier;

import static com.mantkowicz.light.actor.GameActorType.ACTION_BUTTON;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

/**
 * A button at the bottom of the screen that stays while its action is available and fades out when it no longer is
 * (and fades back in if it becomes available again before it is gone).
 */
public class ActionButton extends GameActor {
    private final TextButton button;

    public ActionButton(GameContext context, String text, EventListener listener, BooleanSupplier available) {
        super(ACTION_BUTTON);

        button = UiStyle.textButton(context.getResourcesService(), text);
        button.addListener(listener);

        addActor(button);

        setSize(button.getWidth(), button.getHeight());

        Plugin centerActorByCameraPlugin = new CenterActorByCameraPlugin(this, new Vector2(0, SCREEN_WIDTH / 2f));
        addPlugin(centerActorByCameraPlugin);

        addPlugin(new RemoveWhenUnavailablePlugin(this, available));
    }

    /** Not removed yet (possibly fading out). */
    public boolean isOnStage() {
        return getStage() != null;
    }
}
