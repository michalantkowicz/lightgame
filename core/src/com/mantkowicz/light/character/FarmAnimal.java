package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.DreamBubble;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.PictureBubble;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.actor.WalkNextToOnTapListener;
import com.mantkowicz.light.actor.Zzz;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.plugin.RemoveWhenUnavailablePlugin;
import com.mantkowicz.light.plugin.StateLabelPlugin;
import com.mantkowicz.light.quest.CreatureQuests;
import com.mantkowicz.light.quest.Quest;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;

import java.util.List;

import static com.mantkowicz.light.debug.Debug.Tool.STATE_LABELS;

/**
 * What every farm animal shares (a {@link Cow}, a {@link Chicken}): its favourite thing (one kind of crop), which the
 * player can give it whenever it can be fed (the round button with it over it, {@link Creature}; then it waits
 * {@link #SECONDS_BETWEEN_MEALS} before it can be fed again), being stroked, its quests ({@link CreatureQuests}: the one
 * it offers shows in a bubble over it), and waiting between what it does. What it does and when is its own gdx-ai state
 * machine (an enum next to its class, e.g. {@link CowState}); the states use the helpers here. Tapping it walks the
 * player next to it; holding it shows its description. At night it sleeps ({@link #setNight}): nothing to give, no
 * stroke, "zzz" over it, and now and then it dreams about its favourite thing ({@link DreamBubble}).
 */
public abstract class FarmAnimal extends SpriteActor implements Creature {
    /** What the player tells the animal (gdx-ai messages, answered in its states). */
    static final int MESSAGE_STROKE = 1, MESSAGE_GIFT = 2;
    /** After a stroke it can't be stroked again for this long (the hand doesn't come back meanwhile). */
    static final float SECONDS_BETWEEN_STROKES = 20;
    /** After eating it can be fed again after this long (owner's choice for now: at once, once it has eaten). */
    static final float SECONDS_BETWEEN_MEALS = 0;
    /** A face floating up over it: screen pixels per pack pixel, and how far over its tile (over the heart it shows). */
    private static final float EMOTE_SCALE = 6, EMOTE_GAP = 60;

    private final ResourcesService resources;
    private final GameEventService events;
    /** Its favourite thing, as an item: what its button shows and its dreams are about. */
    private final Crop favourite;
    /** Its face on the quest panel, e.g. a cow's. */
    private final UiImage icon;
    /** How long since its last meal and stroke, its quests...: what goes with it through a door. */
    Shared shared = new Shared(); // package-visible: its trip to bed reads its part (BedtimeTrip.State)
    /** The bubble with the quest it offers. */
    private final OfferBubble offer;
    /** Seconds left of what {@link #stayFor} set. */
    private float timeLeft;
    /** Over it while it sleeps the night: the "zzz", and now and then a dream. */
    private final Zzz zzz;
    private final DreamBubble dream;
    private boolean sleepingTheNight;

    /**
     * What an animal carries through a door to another map ({@link #moveTo}): everything about it that has to stay as
     * it was. What lives in the animal itself (what it is doing, its wait, its bubble) starts again on the other side,
     * so a new field that must not start again belongs here.
     */
    static final class Shared {
        /** Seconds since its last meal (from the start: long enough, it can be fed at once). */
        float sinceFed = SECONDS_BETWEEN_MEALS;
        /** Seconds since it was last stroked (from the start: long enough, it can be stroked at once). */
        float sinceStroked = SECONDS_BETWEEN_STROKES;
        /** Bites still to take when it feeds itself (grazing, pecking at the ground). */
        int bitesLeft;
        /** It is night: it goes to sleep, or sleeps (see {@code BedtimeFeature}). */
        boolean night;
        /** Where it is with its trip to its shelter for the night (in for the night, just out, gave up). */
        final BedtimeTrip.State bedtime = new BedtimeTrip.State();
        /** Its quests, for good. */
        CreatureQuests quests;
    }

    /**
     * @param favourite its favourite crop
     * @param icon      its face on the quest panel
     */
    protected FarmAnimal(GameActorType type, GameContext context, Sprite sprite, String description,
            CropType favourite, UiImage icon) {
        super(type, context, sprite, Action.IDLE, description);
        resources = context.getResourcesService();
        events = context.getGameEventService();
        this.favourite = new Crop(favourite, resources);
        this.icon = icon;
        shared.quests = new CreatureQuests(List.of(), icon);
        zzz = new Zzz(resources.getUiImage(UiImage.ZZZ), context.getGameStage());
        dream = new DreamBubble(resources, context.getGameStage());
        offer = new OfferBubble(this, resources, events);
        setTouchable(Touchable.enabled);
        addListener(new WalkNextToOnTapListener(this, events));
    }

    /** Its quests from Tiled (its {@code quests} property): it keeps them for good. */
    public void assignQuests(List<Quest> quests) {
        shared.quests = new CreatureQuests(quests, icon);
    }

    /**
     * Starts its life: every frame its clocks tick and its state machine takes a step. Called by the subclass once its state machine is in its first state.
     */
    protected void startLiving() {
        addPlugin(delta -> {
            timeLeft -= delta;
            shared.sinceFed += delta;
            shared.sinceStroked += delta;
            offer.update(shared.quests.getOffered(), shared.night);
            if (sleepingTheNight) {
                zzz.keepOver(this);
                dream.dream(this, favourite.getPicture(), delta);
            }
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
     * The same animal on another map, where it came out of a door: it carries its {@link Shared} (its quests, how long
     * since its last meal and stroke). What it was doing starts again from its state machine's first state.
     */
    @Override
    public FarmAnimal moveTo(GameContext context) {
        FarmAnimal there = createAnother(context);
        there.shared = shared;
        return there;
    }

    /** A new animal of the same kind (a chick, a cow with the same favourite) on another map. */
    protected abstract FarmAnimal createAnother(GameContext context);

    /** Leaves the map (through a door): its bubble, its "zzz" and its dream go too. */
    @Override
    public void leaveBoard() {
        offer.hide();
        stopSleepingTheNight();
        super.leaveBoard();
    }

    /** Its state machine takes a step. */
    protected abstract void think();

    /** Its state machine, which the player's messages go to. */
    protected abstract Telegraph getBrain();

    /** The name of the state it is in, e.g. "EATING" (debug labels, tests). */
    public abstract String getStateName();

    /**
     * Is it still busy with what it was given: getting up to eat it, eating it, showing its hearts? What a quest gives
     * back comes once it isn't.
     */
    public abstract boolean isTakingAGift();

    /** Does what it is doing right now allow a stroke (e.g. standing, resting; not walking or eating)? */
    protected abstract boolean isStrokable();

    /** Does what it is doing right now allow being given something (e.g. standing, resting; not walking or eating)? */
    protected abstract boolean isFeedable();

    /**
     * It can be stroked: it is day (at night it goes to sleep or sleeps: nothing wakes it), what it does allows it
     * ({@link #isStrokable()}) and it wasn't stroked in the last {@link #SECONDS_BETWEEN_STROKES} seconds.
     */
    @Override
    public boolean canBeStroked() {
        return !shared.night && isStrokable() && shared.sinceStroked >= SECONDS_BETWEEN_STROKES;
    }

    /**
     * It can be given something: it is day, what it does allows it ({@link #isFeedable()}) and it ate at least
     * {@link #SECONDS_BETWEEN_MEALS} ago.
     */
    @Override
    public boolean canBeFed() {
        return !shared.night && isFeedable() && shared.sinceFed >= SECONDS_BETWEEN_MEALS;
    }

    /**
     * Night or day ({@code BedtimeFeature}): at night it goes to sleep once done with what it is doing, by day it wakes
     * up. Its states look at it ({@link #isNight()}).
     */
    public void setNight(boolean night) {
        shared.night = night;
    }

    boolean isNight() {
        return shared.night;
    }

    // what the player does with it (a tap on it, the round buttons)

    /** Tapping it walks the player next to it, like a chest. */
    @Override
    public boolean isUsedFromNeighbour() {
        return true;
    }

    @Override
    public Item getFavourite() {
        return favourite;
    }

    /** Its favourite crop. */
    public CropType getFavouriteType() {
        return favourite.getType();
    }

    @Override
    public void give(Item item) {
        MessageManager.getInstance().dispatchMessage(null, getBrain(), MESSAGE_GIFT);
    }

    @Override
    public void stroke() {
        MessageManager.getInstance().dispatchMessage(null, getBrain(), MESSAGE_STROKE);
    }

    @Override
    public Actor getActor() {
        return this;
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

    /** It eats what it was given: the next meal only after {@link #SECONDS_BETWEEN_MEALS}. */
    void eat() {
        shared.sinceFed = 0;
    }

    /** It feeds itself now and then (grazing, pecking at the ground): this many bites. */
    void startFeedingItself(int bites) {
        shared.bitesLeft = bites;
    }

    void takeBite() {
        shared.bitesLeft--;
    }

    boolean hasBitesLeft() {
        return shared.bitesLeft > 0;
    }

    // its quests

    /** Its quests (they go with it through doors; {@code QuestOffersFeature} asks them whether one should start). */
    public CreatureQuests getQuests() {
        return shared.quests;
    }

    @Override
    public void setOfferShown(boolean shown) {
        offer.setShown(shown);
    }

    /** The bubble with the quest it offers, or null: for tests and descriptions. */
    public PictureBubble getOfferBubble() {
        return offer.get();
    }

    // the night

    /** Asleep for the night: the "zzz" over it, and now and then a dream about its favourite thing. */
    void startSleepingTheNight() {
        sleepingTheNight = true;
    }

    /** Awake: the "zzz" and the dream are gone. */
    void stopSleepingTheNight() {
        sleepingTheNight = false;
        zzz.remove();
        dream.wake();
    }

    /** The "zzz" over it while it sleeps the night (shown with {@link Zzz#keepOver}, else not on a stage). */
    public Zzz getZzz() {
        return zzz;
    }

    /** What it dreams about now and then while it sleeps the night. */
    public DreamBubble getDream() {
        return dream;
    }

    /** A small face (the pack's tiny emotes) floats up over it and fades. Not used right now: no state calls it. */
    void showEmote(UiImage face) {
        new FloatingPicture(face.name(), resources.getUiImage(face), EMOTE_SCALE).showOver(this, EMOTE_GAP);
    }

    protected ResourcesService getResources() {
        return resources;
    }
}
