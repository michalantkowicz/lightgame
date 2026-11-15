package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.GameActorType;
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

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.debug.Debug.Tool.STATE_LABELS;

/**
 * What every farm animal shares (a {@link Cow}, a {@link Chicken}): it gets hungry, wishes for one of its favourite
 * crops (a bubble over it), gives up after a while, can be fed and stroked by the player (the round buttons over it,
 * {@link Creature}), and waits between what it does. What it does and when is its own gdx-ai state machine (an enum
 * next to its class, e.g. {@link CowState}); the states use the helpers here. Tapping it walks the player next to it;
 * holding it shows its description.
 */
public abstract class FarmAnimal extends SpriteActor implements Creature {
    /** What the player tells the animal (gdx-ai messages, answered in its states). */
    static final int MESSAGE_STROKE = 1, MESSAGE_GIFT = 2;
    /** A face floating up over it: screen pixels per pack pixel, and how far over its tile (over the heart it shows). */
    private static final float EMOTE_SCALE = 6, EMOTE_GAP = 60;

    private final ResourcesService resources;
    /** The crops it may wish for when hungry. */
    private final List<CropType> favouriteDishes;
    private final float hungryAfterSeconds, giveUpWishingAfterSeconds;
    /** What it wishes for this time it is hungry (picked when it gets hungry). */
    private Crop favouriteFood;
    private PictureBubble wishBubble;
    /** False while the action button with its wish is over it: the bubble is hidden. */
    private boolean wishShown = true;
    /** Seconds left of what {@link #stayFor} set, and since the start or its last meal. */
    private float timeLeft, sinceFed;
    /** Bites still to take when it feeds itself (grazing, pecking) after giving up its wish. */
    private int bitesLeft;

    /**
     * @param favouriteDishes           the crops it wishes for (one at random each time it gets hungry); any crop when
     *                                  null or empty
     * @param hungryAfterSeconds        this long after the start or its last meal it gets hungry again
     * @param giveUpWishingAfterSeconds wishing this long without getting its crop, it gives up and feeds itself
     */
    protected FarmAnimal(GameActorType type, GameContext context, Sprite sprite, String description,
            List<CropType> favouriteDishes, float hungryAfterSeconds, float giveUpWishingAfterSeconds) {
        super(type, context, sprite, Action.IDLE, description);
        resources = context.getResourcesService();
        this.favouriteDishes = favouriteDishes == null || favouriteDishes.isEmpty()
                ? CropType.all()
                : new ArrayList<>(favouriteDishes);
        this.hungryAfterSeconds = hungryAfterSeconds;
        this.giveUpWishingAfterSeconds = giveUpWishingAfterSeconds;
        favouriteFood = new Crop(this.favouriteDishes.get(0), resources);
        setTouchable(Touchable.enabled);
        addListener(new WalkNextToOnTapListener(this, context.getGameEventService()));
    }

    /**
     * Starts its life: every frame its clocks tick and its state machine takes a step. Called by the subclass once its
     * state machine is in its first state.
     */
    protected void startLiving() {
        addPlugin(delta -> {
            timeLeft -= delta;
            sinceFed += delta;
            think();
        });
        if (Debug.isOn(STATE_LABELS)) {
            addPlugin(new StateLabelPlugin(this, resources, this::getStateName));
        }
    }

    /** Its state machine takes a step. */
    protected abstract void think();

    /** Its state machine, which the player's messages go to. */
    protected abstract Telegraph getBrain();

    /** The name of the state it is in, e.g. "WISHING" (debug labels, tests). */
    public abstract String getStateName();

    /** Is it wishing for its crop right now (its wish is what the player can give it)? */
    protected abstract boolean isWishing();

    // what the player does with it (a tap on it, the round buttons)

    /** Tapping it walks the player next to it, like a chest. */
    @Override
    public boolean isUsedFromNeighbour() {
        return true;
    }

    @Override
    public Item getWish() {
        return isWishing() ? favouriteFood : null;
    }

    @Override
    public void give(Item item) {
        MessageManager.getInstance().dispatchMessage(null, getBrain(), MESSAGE_GIFT);
    }

    @Override
    public void stroke() {
        MessageManager.getInstance().dispatchMessage(null, getBrain(), MESSAGE_STROKE);
    }

    // what the states use (with SpriteActor's playAnimation, playOnce, isAnimationFinished, walkToFreeNeighbour)

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
        return sinceFed >= hungryAfterSeconds;
    }

    void stopBeingHungry() {
        sinceFed = 0;
    }

    /** It got hungry: picks, at random, which of its favourite dishes it wishes for this time. */
    void chooseWish() {
        favouriteFood = new Crop(CropType.randomOf(favouriteDishes), resources);
    }

    /** The crops it may wish for. */
    public List<CropType> getFavouriteDishes() {
        return favouriteDishes;
    }

    /** It has wished for its crop too long without getting it. */
    boolean hasWishedTooLong() {
        return sinceFed >= hungryAfterSeconds + giveUpWishingAfterSeconds;
    }

    /** It gives up its wish and feeds itself: this many bites (of grass, of what it finds on the ground). */
    void startFeedingItself(int bites) {
        bitesLeft = bites;
    }

    void takeBite() {
        bitesLeft--;
    }

    boolean hasBitesLeft() {
        return bitesLeft > 0;
    }

    /** A bubble over it with what it wishes for. */
    void showWish() {
        wishBubble = new PictureBubble(resources, favouriteFood.getType().getName(), favouriteFood.getPicture());
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

    protected ResourcesService getResources() {
        return resources;
    }
}
