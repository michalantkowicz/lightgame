package com.mantkowicz.light.plugin;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Plugin;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.function.Supplier;

/**
 * A debug tool ({@link Debug.Tool#STATE_LABELS}): writes the state an actor is in under it, e.g. "STANDING" under
 * the cow, so a state machine can be watched while playing. Add it only when the tool is on:
 * <pre>{@code
 * if (Debug.isOn(STATE_LABELS)) {
 *     addPlugin(new StateLabelPlugin(this, resources, () -> stateMachine.getCurrentState().toString()));
 * }
 * }</pre>
 */
public class StateLabelPlugin implements Plugin {
    private final GameActor actor;
    private final Supplier<String> state;
    private final Label label;

    public StateLabelPlugin(GameActor actor, ResourcesService resources, Supplier<String> state) {
        this.actor = actor;
        this.state = state;
        label = new Label("", new Label.LabelStyle(resources.getFont(), Color.WHITE));
        label.setTouchable(Touchable.disabled);
        label.setAlignment(Align.center);
        actor.addActor(label);
    }

    @Override
    public void run(float delta) {
        label.setText(state.get());
        label.setPosition(actor.getWidth() / 2f, -label.getPrefHeight() / 2f, Align.center);
    }

    /** What the label says right now. */
    public String getText() {
        return label.getText().toString();
    }
}
