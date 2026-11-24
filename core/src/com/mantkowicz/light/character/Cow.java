package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;

import static com.mantkowicz.light.actor.GameActorType.COW;

/**
 * A brown cow. What it does (stands, walks a step, grazes now and then, lies down, sleeps, eats what it is given,
 * enjoys a stroke) and when is all in {@link CowState}, a gdx-ai state machine; what every farm animal shares (its
 * favourite thing, being fed and stroked, its quests) is in {@link FarmAnimal}. Its favourite crop is set in Tiled with
 * the favouriteDish property, e.g. CARROT (a carrot without it). At night it goes to the barn to sleep, and out again
 * in the morning ({@link BedtimeTrip}).
 */
public class Cow extends FarmAnimal implements Sheltered {
    /** Walking pace: seconds for its step onto the next tile. */
    static final float SECONDS_PER_TILE = 1.5f;
    /** Grazing now and then, it takes this many bites of grass. */
    static final int GRAZING_BITES = 3;
    /** Its favourite crop when Tiled doesn't say. */
    public static final CropType DEFAULT_FAVOURITE = CropType.CARROT;

    private final DefaultStateMachine<Cow, CowState> stateMachine = new DefaultStateMachine<>(this);
    /** Going to the barn to sleep, and out in the morning. */
    private final BedtimeTrip trip;
    /** Given something while lying: it gets up and then eats. */
    private boolean mealWaiting;

    /** A cow whose favourite is a carrot. */
    public Cow(GameContext context) {
        this(context, DEFAULT_FAVOURITE);
    }

    public Cow(GameContext context, CropType favourite) {
        super(COW, context, Sprite.COW_BROWN, "A brown cow. It looks calm and likes a treat.", favourite,
                UiImage.COW_FACE);
        trip = new BedtimeTrip(this, context.getBoardService(), SECONDS_PER_TILE, false);
        stateMachine.changeState(CowState.STANDING);
        startLiving();
    }

    @Override
    protected FarmAnimal createAnother(GameContext context) {
        return new Cow(context, getFavouriteType()); // its trip to the barn and its quests go along in the shared part
    }

    @Override
    public void setRoost(Roost roost) {
        trip.setRoost(roost);
    }

    @Override
    public boolean sleepsAway() {
        return trip.sleepsAway();
    }

    /** Going to the barn to sleep, and out in the morning. */
    BedtimeTrip getTrip() {
        return trip;
    }

    public CowState getState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public boolean isTakingAGift() {
        return mealWaiting || getState() == CowState.EATING || getState() == CowState.HAPPY;
    }

    @Override
    public String getStateName() {
        return getState().name();
    }

    @Override
    protected void think() {
        stateMachine.update();
    }

    @Override
    protected Telegraph getBrain() {
        return stateMachine;
    }

    @Override
    protected boolean isStrokable() {
        return getState().canBeStroked();
    }

    @Override
    protected boolean isFeedable() {
        return getState().canBeFed();
    }

    void changeState(CowState state) {
        stateMachine.changeState(state);
    }

    /** Now and then it grazes: {@link #GRAZING_BITES} bites of grass. */
    void startGrazing() {
        startFeedingItself(GRAZING_BITES);
    }

    /** Given something while lying: it eats once it is up ({@link #takeWaitingMeal()}). */
    void eatOnceUp() {
        mealWaiting = true;
    }

    /** Was it given something while lying? Then it eats now (and the meal no longer waits). */
    boolean takeWaitingMeal() {
        boolean waiting = mealWaiting;
        mealWaiting = false;
        return waiting;
    }
}
