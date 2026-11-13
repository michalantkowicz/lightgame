package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Carrot;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.PictureBubble;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.actor.WalkNextToOnTapListener;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.plugin.StateLabelPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;

import static com.mantkowicz.light.actor.GameActorType.COW;
import static com.mantkowicz.light.debug.Debug.Tool.STATE_LABELS;

/**
 * A brown cow. What it does (stands, walks a step, lies down, sleeps, wishes for a carrot, eats, enjoys a stroke) and
 * when is all in {@link CowState}, a gdx-ai state machine; this class only has what the states use. Tapping it walks
 * the player next to it; holding it shows its description.
 */
public class Cow extends SpriteActor implements Creature {
    /** What the player tells the cow (gdx-ai messages, answered in {@link CowState}). */
    static final int MESSAGE_STROKE = 1, MESSAGE_GIFT = 2;
    /** Walking pace: seconds for its step onto the next tile. */
    static final float SECONDS_PER_TILE = 1.5f;
    /** This long after the start or its last carrot it gets hungry again. */
    static final float HUNGRY_AFTER_SECONDS = 20;
    /** A face floating up over it: screen pixels per pack pixel, and how far over its tile (over the heart it shows). */
    private static final float EMOTE_SCALE = 6, EMOTE_GAP = 60;

    private final ResourcesService resources;
    private final DefaultStateMachine<Cow, CowState> stateMachine = new DefaultStateMachine<>(this);
    /** What it wishes for when hungry. */
    private final Item favouriteFood;
    private PictureBubble wishBubble;
    /** False while the action button with its wish is over it: the bubble is hidden. */
    private boolean wishShown = true;
    /** Seconds left of what {@link #stayFor} set, and since the start or its last carrot. */
    private float timeLeft, sinceFed;

    public Cow(GameContext context) {
        super(COW, context, Sprite.COW_BROWN, Action.IDLE, "A brown cow. It looks calm and a bit hungry.");
        resources = context.getResourcesService();
        favouriteFood = new Carrot(resources);
        setTouchable(Touchable.enabled);
        addListener(new WalkNextToOnTapListener(this, context.getGameEventService()));

        stateMachine.changeState(CowState.STANDING);
        addPlugin(delta -> {
            timeLeft -= delta;
            sinceFed += delta;
            stateMachine.update();
        });
        if (Debug.isOn(STATE_LABELS)) {
            addPlugin(new StateLabelPlugin(this, resources, () -> getState().name()));
        }
    }

    public CowState getState() {
        return stateMachine.getCurrentState();
    }

    // what the player does with it (a tap on it, the round button)

    /** Tapping it walks the player next to it, like a chest. */
    @Override
    public boolean isUsedFromNeighbour() {
        return true;
    }

    @Override
    public Item getWish() {
        return getState() == CowState.WISHING ? favouriteFood : null;
    }

    @Override
    public void give(Item item) {
        MessageManager.getInstance().dispatchMessage(null, stateMachine, MESSAGE_GIFT);
    }

    @Override
    public boolean canBeStroked() {
        return getState().canBeStroked();
    }

    @Override
    public void stroke() {
        MessageManager.getInstance().dispatchMessage(null, stateMachine, MESSAGE_STROKE);
    }

    // what the states use (with SpriteActor's playAnimation, playOnce, isAnimationFinished, walkToFreeNeighbour)

    void changeState(CowState state) {
        stateMachine.changeState(state);
    }

    /** Starts a wait of a random length between the two; {@link #isTimeUp()} says when it is over. */
    void stayFor(float minSeconds, float maxSeconds) {
        timeLeft = MathUtils.random(minSeconds, maxSeconds);
    }

    boolean isTimeUp() {
        return timeLeft <= 0;
    }

    boolean isWalking() {
        return Status.MOVEMENT.equals(getStatus());
    }

    boolean isHungry() {
        return sinceFed >= HUNGRY_AFTER_SECONDS;
    }

    void stopBeingHungry() {
        sinceFed = 0;
    }

    /** A bubble over it with what it wishes for. */
    void showWish() {
        wishBubble = new PictureBubble(resources, "carrot", favouriteFood.getPicture());
        wishBubble.showOver(this);
        wishBubble.setVisible(wishShown);
    }

    @Override
    public void setWishShown(boolean shown) {
        wishShown = shown;
        if (wishBubble != null) {
            wishBubble.setVisible(shown);
        }
    }

    @Override
    public Actor getActor() {
        return this;
    }

    void hideWish() {
        wishBubble.remove();
        wishBubble = null;
    }

    /** A small face (the pack's tiny emotes) floats up over it and fades. Not used right now: no state calls it. */
    void showEmote(UiImage face) {
        new FloatingPicture(face.name(), resources.getUiImage(face), EMOTE_SCALE).showOver(this, EMOTE_GAP);
    }

    /** The bubble with its wish, or null: for tests and descriptions. */
    public PictureBubble getWishBubble() {
        return wishBubble;
    }
}
