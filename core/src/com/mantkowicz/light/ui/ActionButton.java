package com.mantkowicz.light.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.CenterActorByCameraPlugin;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.plugin.RemoveOnPlayerMovePlugin;

import static com.mantkowicz.light.actor.GameActorType.ACTION_BUTTON;
import static com.mantkowicz.light.game.Main.SCREEN_WIDTH;

public class ActionButton extends GameActor {
    private final TextButton button;

    public ActionButton(GameContext context, String text, EventListener listener) {
        super(ACTION_BUTTON);

        button = new TextButton(text, context.getResourcesService().getSkin(), "action");
        button.addListener(listener);

        addActor(button);

        setSize(button.getWidth(), button.getHeight());

        Plugin centerActorByCameraPlugin = new CenterActorByCameraPlugin(this, new Vector2(0, SCREEN_WIDTH / 2f));
        addPlugin(centerActorByCameraPlugin);

        RemoveOnPlayerMovePlugin removeOnPlayerMovePlugin = new RemoveOnPlayerMovePlugin(this, context);
        addPlugin(removeOnPlayerMovePlugin);
    }
}
