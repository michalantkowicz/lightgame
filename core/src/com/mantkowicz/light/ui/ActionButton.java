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

/**
 * A button at the bottom of the screen that stays while its action is available and fades out when it no longer is
 * (and fades back in if it becomes available again before it is gone).
 */
public class ActionButton extends GameActor {
    /**
     * How far below the screen's middle the button's centre is: just above the HUD's face (docs/hud-design.md moves the
     * action onto the face in step 2, which removes this button).
     */
    private static final float BELOW_THE_MIDDLE = 310;
    private final TextButton button;

    public ActionButton(GameContext context, String text, EventListener listener, BooleanSupplier available) {
        super(ACTION_BUTTON);

        button = UiStyle.textButton(context.getResourcesService(), text);
        button.addListener(listener);

        addActor(button);

        setSize(button.getWidth(), button.getHeight());

        Plugin centerActorByCameraPlugin = new CenterActorByCameraPlugin(this, new Vector2(0, BELOW_THE_MIDDLE));
        addPlugin(centerActorByCameraPlugin);

        addPlugin(new RemoveWhenUnavailablePlugin(this, available));
    }

    /** Not removed yet (possibly fading out). */
    public boolean isOnStage() {
        return getStage() != null;
    }
}
