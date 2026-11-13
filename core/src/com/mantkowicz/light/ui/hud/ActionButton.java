package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.plugin.RemoveWhenUnavailablePlugin;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.RoundButton;

import java.util.function.BooleanSupplier;

import static com.mantkowicz.light.actor.GameActorType.ACTION_BUTTON;

/**
 * A {@link RoundButton} with a picture of what it does (e.g. the open chest to collect from), over what it acts on (the
 * chest, the cow) and following it around. It is on the UI stage, so it keeps its size and catches taps before the map
 * under it. It stays while its action is available and fades out when it no longer is (and fades back in if it becomes
 * available again before it is gone). Its name (e.g. "COLLECT") is for tests only: players see the picture.
 */
public class ActionButton extends GameActor {
    /** Between the top of what it acts on and the button's bottom. */
    private static final float GAP = 16;
    /** Screen pixels per pack pixel: smaller than the HUD's hint button (6 and 4), so it doesn't hide the map. */
    private static final int SCALE = 5, ICON_SCALE = 3;
    /** Darker than the hint's brown: it stands out from the grass and the water, and fits the wooden HUD. */
    private static final UiImage UP = UiImage.ACTION_BUTTON_DARK_BROWN, DOWN = UiImage.ACTION_BUTTON_DARK_BROWN_DOWN;
    /** How far it moves sideways when shaken, and how long the first move takes (the whole shake is 6 times that). */
    private static final float SHAKE = 8, SHAKE_SECONDS = 0.04f;

    private final Stage gameStage;
    private final Actor over;
    private final RoundButton button;

    /**
     * @param over what it acts on, on the game stage: the button is drawn over it
     */
    public ActionButton(GameContext context, String name, TextureRegion picture, EventListener listener,
            BooleanSupplier available, Actor over) {
        super(ACTION_BUTTON);
        this.gameStage = context.getGameStage();
        this.over = over;
        setName(name);
        button = new RoundButton(context.getResourcesService(), picture, UP, DOWN, SCALE, ICON_SCALE);
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
            followTarget();
        }
    }

    /** Centred over the top of what it acts on, wherever the camera shows it. */
    private void followTarget() {
        Vector2 top = over.localToStageCoordinates(new Vector2(over.getWidth() / 2f, over.getHeight()));
        getStage().screenToStageCoordinates(gameStage.stageToScreenCoordinates(top));
        setPosition(Math.round(top.x - getWidth() / 2f), Math.round(top.y + GAP));
    }

    /** A quick shake sideways: tapped, but there is nothing it can do (e.g. nothing to give yet). */
    public void shake() {
        button.clearActions();
        button.setX(0);
        button.addAction(Actions.sequence(
                Actions.moveBy(SHAKE, 0, SHAKE_SECONDS, Interpolation.sine),
                Actions.moveBy(-2 * SHAKE, 0, 2 * SHAKE_SECONDS, Interpolation.sine),
                Actions.moveBy(2 * SHAKE, 0, 2 * SHAKE_SECONDS, Interpolation.sine),
                Actions.moveBy(-SHAKE, 0, SHAKE_SECONDS, Interpolation.sine)));
    }

    /** Shaken and not still yet ({@link #shake()}). */
    public boolean isShaking() {
        return button.hasActions();
    }

    /** Not removed yet (possibly fading out). */
    public boolean isOnStage() {
        return getStage() != null;
    }
}
