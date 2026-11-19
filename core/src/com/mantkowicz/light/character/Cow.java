package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Sprite;

import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.COW;

/**
 * A brown cow. What it does (stands, walks a step, lies down, sleeps, wishes for a crop, eats, grazes when nobody
 * feeds it, enjoys a stroke) and when is all in {@link CowState}, a gdx-ai state machine; what every farm animal
 * shares (hunger, wishes, being fed and stroked) is in {@link FarmAnimal}. Each time it gets hungry it wishes for one
 * of its favourite dishes, picked at random (set in Tiled with the favouriteDish property, e.g. CARROT,CUCUMBER; any
 * crop without it). At night it goes to the barn to sleep, and out again in the morning ({@link BedtimeTrip}).
 */
public class Cow extends FarmAnimal implements Sheltered {
    /** Walking pace: seconds for its step onto the next tile. */
    static final float SECONDS_PER_TILE = 1.5f;
    /** This long after the start or its last meal it gets hungry again. */
    static final float HUNGRY_AFTER_SECONDS = 60;
    /** Wishing this long without getting its crop, it gives up and grazes, taking this many bites of grass. */
    static final float GIVE_UP_WISHING_AFTER_SECONDS = 90;
    static final int GRAZING_BITES = 3;

    private final DefaultStateMachine<Cow, CowState> stateMachine = new DefaultStateMachine<>(this);
    /** Going to the barn to sleep, and out in the morning. */
    private final BedtimeTrip trip;

    /** A cow that may wish for any crop. */
    public Cow(GameContext context) {
        this(context, CropType.all());
    }

    /**
     * A cow that wishes only for these crops (one at random each time it gets hungry); for any crop when the list is
     * null or empty.
     */
    public Cow(GameContext context, List<CropType> favouriteDishes) {
        super(COW, context, Sprite.COW_BROWN, "A brown cow. It looks calm and a bit hungry.", favouriteDishes,
                HUNGRY_AFTER_SECONDS, GIVE_UP_WISHING_AFTER_SECONDS);
        trip = new BedtimeTrip(this, context.getBoardService(), SECONDS_PER_TILE, false);
        stateMachine.changeState(CowState.STANDING);
        startLiving();
    }

    @Override
    protected FarmAnimal createAnother(GameContext context) {
        return new Cow(context, getFavouriteDishes()); // its trip to the barn goes along in the shared part
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
    protected boolean isWishing() {
        return getState() == CowState.WISHING;
    }

    @Override
    protected boolean isStrokable() {
        return getState().canBeStroked();
    }

    void changeState(CowState state) {
        stateMachine.changeState(state);
    }

    /** Nobody fed it: it grazes, {@link #GRAZING_BITES} bites of grass. */
    void startGrazing() {
        startFeedingItself(GRAZING_BITES);
    }
}
