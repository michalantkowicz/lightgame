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
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.plugin.RemoveWhenUnavailablePlugin;
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
    /** After a stroke it can't be stroked again for this long (the hand doesn't come back meanwhile). */
    static final float SECONDS_BETWEEN_STROKES = 20;
    /** A face floating up over it: screen pixels per pack pixel, and how far over its tile (over the heart it shows). */
    private static final float EMOTE_SCALE = 6, EMOTE_GAP = 60;

    private final ResourcesService resources;
    private final GameEventService events;
    /** The crops it may wish for when hungry. */
    private final List<CropType> favouriteDishes;
    private final float hungryAfterSeconds, giveUpWishingAfterSeconds;
    /** How hungry it is, what it wishes for...: what goes with it through a door. */
    private Shared shared = new Shared();
    /** What it wishes for this time it is hungry, as an item ({@link Shared#wish}). */
    private Crop favouriteFood;
    private PictureBubble wishBubble;
    /** False while the action button with its wish is over it: the bubble is hidden. */
    private boolean wishShown = true;
    /** Seconds left of what {@link #stayFor} set. */
    private float timeLeft;

    /**
     * What an animal carries through a door to another map ({@link #moveTo}): everything about it that has to stay as
     * it was. What lives in the animal itself (what it is doing, its wait, its bubble) starts again on the other side,
     * so a new field that must not start again belongs here.
     */
    private static final class Shared {
        /** What it wishes for this time it is hungry (picked when it gets hungry). */
        CropType wish;
        /** It has picked what it wishes for this time it is hungry (stroked meanwhile, it wishes for the same again). */
        boolean wishChosen;
        /** Seconds since the start or its last meal. */
        float sinceFed;
        /** Seconds since it was last stroked (from the start: long enough, it can be stroked at once). */
        float sinceStroked = SECONDS_BETWEEN_STROKES;
        /** Bites still to take when it feeds itself (grazing, pecking) after giving up its wish. */
        int bitesLeft;
    }

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
        events = context.getGameEventService();
        this.favouriteDishes = favouriteDishes == null || favouriteDishes.isEmpty()
                ? CropType.all()
                : new ArrayList<>(favouriteDishes);
        this.hungryAfterSeconds = hungryAfterSeconds;
        this.giveUpWishingAfterSeconds = giveUpWishingAfterSeconds;
        shared.wish = this.favouriteDishes.get(0);
        favouriteFood = new Crop(shared.wish, resources);
        setTouchable(Touchable.enabled);
        addListener(new WalkNextToOnTapListener(this, events));
    }

    /**
     * Starts its life: every frame its clocks tick and its state machine takes a step. Called by the subclass once its
     * state machine is in its first state.
     */
    protected void startLiving() {
        addPlugin(delta -> {
            timeLeft -= delta;
            shared.sinceFed += delta;
            shared.sinceStroked += delta;
            think();
        });
        if (Debug.isOn(STATE_LABELS)) {
            addPlugin(new StateLabelPlugin(this, resources, this::getStateName));
        }
    }

    @Override
    public boolean goesThroughDoors() {
        return true;
    }

    /**
     * The same animal on another map, where it came out of a door: it carries its {@link Shared} (as hungry as here,
     * wishing for the same, as long since its last stroke). What it was doing starts again from its state machine's
     * first state (on the other side it looks around first), so a wish comes back in a moment. Take it off this map
     * with {@link #leaveBoard()}.
     */
    @Override
    public FarmAnimal moveTo(GameContext context) {
        FarmAnimal there = createAnother(context);
        there.shared = shared;
        there.favouriteFood = new Crop(shared.wish, there.resources);
        return there;
    }

    /** A new animal of the same kind (a chick, a cow with the same favourite dishes) on another map. */
    protected abstract FarmAnimal createAnother(GameContext context);

    /** Leaves the map (through a door): the bubble with its wish goes too. */
    @Override
    public void leaveBoard() {
        if (wishBubble != null) {
            hideWish();
        }
        super.leaveBoard();
    }

    /** Its state machine takes a step. */
    protected abstract void think();

    /** Its state machine, which the player's messages go to. */
    protected abstract Telegraph getBrain();

    /** The name of the state it is in, e.g. "WISHING" (debug labels, tests). */
    public abstract String getStateName();

    /** Is it wishing for its crop right now (its wish is what the player can give it)? */
    protected abstract boolean isWishing();

    /** Does what it is doing right now allow a stroke (e.g. standing, resting; not walking or eating)? */
    protected abstract boolean isStrokable();

    /**
     * It can be stroked: what it does allows it ({@link #isStrokable()}) and it wasn't stroked in the last
     * {@link #SECONDS_BETWEEN_STROKES} seconds.
     */
    @Override
    public boolean canBeStroked() {
        return isStrokable() && shared.sinceStroked >= SECONDS_BETWEEN_STROKES;
    }

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

    /**
     * Tapped with the hand, it stands still until the hand has faded out ({@link RemoveWhenUnavailablePlugin#DURATION})
     * and only then shows its hearts, so the button never covers them.
     */
    void waitForTheHand() {
        timeLeft = RemoveWhenUnavailablePlugin.DURATION; // no dice: the cow's other random choices stay as they were
        shared.sinceStroked = 0; // the next stroke only after a while
    }

    /** Starts a wait of a random length between the two; {@link #isTimeUp()} says when it is over. */
    void stayFor(float minSeconds, float maxSeconds) {
        timeLeft = MathUtils.random(minSeconds, maxSeconds);
    }

    /** Starts a wait of just this long (no dice: the other random choices stay as they were). */
    void stayExactly(float seconds) {
        timeLeft = seconds;
    }

    boolean isTimeUp() {
        return timeLeft <= 0;
    }

    boolean isWalking() {
        return Status.MOVEMENT.equals(getStatus());
    }

    boolean isHungry() {
        return shared.sinceFed >= hungryAfterSeconds;
    }

    void stopBeingHungry() {
        shared.sinceFed = 0;
        shared.wishChosen = false;
    }

    /**
     * It got hungry: picks, at random, which of its favourite dishes it wishes for this time; once per hunger, so
     * after a stroke it wishes for the same again.
     */
    void chooseWish() {
        if (!shared.wishChosen) {
            shared.wish = CropType.randomOf(favouriteDishes);
            favouriteFood = new Crop(shared.wish, resources);
            shared.wishChosen = true;
        }
    }

    /** The crops it may wish for. */
    public List<CropType> getFavouriteDishes() {
        return favouriteDishes;
    }

    /** It has wished for its crop too long without getting it. */
    boolean hasWishedTooLong() {
        return shared.sinceFed >= hungryAfterSeconds + giveUpWishingAfterSeconds;
    }

    /** It gives up its wish and feeds itself: this many bites (of grass, of what it finds on the ground). */
    void startFeedingItself(int bites) {
        shared.bitesLeft = bites;
    }

    void takeBite() {
        shared.bitesLeft--;
    }

    boolean hasBitesLeft() {
        return shared.bitesLeft > 0;
    }

    /** A bubble over it with what it wishes for; tapping the bubble walks the player next to it, as tapping it does. */
    void showWish() {
        wishBubble = new PictureBubble(resources, favouriteFood.getType().getName(), favouriteFood.getPicture());
        wishBubble.onTap(new WalkNextToOnTapListener(this, events));
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
