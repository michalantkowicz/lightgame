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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import static com.mantkowicz.light.actor.GameActorType.ACTION_BUTTON;

/**
 * A {@link RoundButton} with a picture of what it does (e.g. the open chest to collect from), over what it acts on (the
 * chest, the cow) and following it around. Several buttons over one thing (usually one, at most about three: the cow's
 * hand and the carrot it wishes for) stand in a row centred over it, ordered by {@link #setPlace}. It is on the UI
 * stage, so it keeps its size and catches taps before the map under it. It fades in when it appears, stays while its
 * action is available and fades out when it no longer is (and fades back in if it becomes available again before it
 * is gone). Its name (e.g. "COLLECT") is for tests only: players see the picture.
 */
public class ActionButton extends GameActor {
    /** Between the top of what it acts on and the button's bottom, and between buttons in a row over one thing. */
    private static final float GAP = 16, ROW_GAP = 8;
    /** Screen pixels per pack pixel: smaller than the HUD's hint button (6 and 4), so it doesn't hide the map. */
    private static final int SCALE = 5, ICON_SCALE = 3;
    /** Darker than the hint's brown: it stands out from the grass and the water, and fits the wooden HUD. */
    private static final UiImage UP = UiImage.ACTION_BUTTON_DARK_BROWN, DOWN = UiImage.ACTION_BUTTON_DARK_BROWN_DOWN;
    /** How far it moves sideways when shaken, and how long the first move takes (the whole shake is 6 times that). */
    private static final float SHAKE = 8, SHAKE_SECONDS = 0.04f;
    /** How long it takes to fade in when it appears: quicker than fading out, so a tap gets its answer soon. */
    private static final float APPEAR_SECONDS = 0.3f;

    private final Stage gameStage;
    private final Actor over;
    private final RoundButton button;
    /** Its place in the row over its thing: lower is further left. */
    private int place;
    /** How high over its thing's bottom the thing is drawn; null: its height. */
    private DoubleSupplier topY;

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
        getColor().a = 0;
        addAction(Actions.fadeIn(APPEAR_SECONDS, Interpolation.sineIn));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null) {
            followTarget();
        }
    }

    /** Over the top of what it acts on, wherever the camera shows it: in its place in the row of buttons there. */
    private void followTarget() {
        float height = topY == null ? over.getHeight() : (float) topY.getAsDouble();
        Vector2 top = over.localToStageCoordinates(new Vector2(over.getWidth() / 2f, height));
        getStage().screenToStageCoordinates(gameStage.stageToScreenCoordinates(top));
        List<ActionButton> row = rowOverTheSameThing();
        float step = getWidth() + ROW_GAP;
        float left = top.x - (row.size() * step - ROW_GAP) / 2f;
        setPosition(Math.round(left + row.indexOf(this) * step), Math.round(top.y + GAP));
    }

    /** The buttons over the same thing (this one too, also any fading out), in their places from left to right. */
    private List<ActionButton> rowOverTheSameThing() {
        List<ActionButton> row = new ArrayList<>();
        for (Actor actor : getStage().getActors()) {
            if (actor instanceof ActionButton other && other.over == over) {
                row.add(other);
            }
        }
        row.sort((a, b) -> Integer.compare(a.place, b.place));
        return row;
    }

    /**
     * How high over its thing's bottom the thing is drawn, if not its height (a picture sticking out above its tile,
     * e.g. an open chest's lid): the button goes over that.
     */
    public void setTopY(DoubleSupplier topY) {
        this.topY = topY;
    }

    /** Its place in the row of buttons over the same thing: lower is further left (0 by default). */
    public void setPlace(int place) {
        this.place = place;
    }

    /** The picture on it now: what it does. */
    public TextureRegion getPicture() {
        return button.getPicture();
    }

    /** Shows another picture: what the button does now (e.g. close the chest it opened). */
    public void setPicture(TextureRegion picture) {
        button.setPicture(picture);
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
