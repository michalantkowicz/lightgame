package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.resources.Action;

/**
 * Everything the cow does, as a gdx-ai state machine: each constant is a state, saying what the cow does when it gets
 * into it ({@code enter}), when it goes on to which state ({@code update}, called every frame) and how it answers the
 * player ({@code onMessage}). To change the cow's behaviour, change it here; {@link Cow} only has the helpers.
 * <pre>
 * STANDING ──(given its favourite, or what its quest waits for)──▶ EATING ──▶ HAPPY ──▶ STANDING
 * RESTING, SLEEPING ──(given something)──▶ GETTING_UP ──▶ EATING ──▶ HAPPY ──▶ STANDING
 * STANDING ──(after a while)──▶ WALKING ──(arrived)──▶ STANDING
 * STANDING ──(now and then)──▶ GRAZING ──▶ CHEWING ──(3 bites)──▶ STANDING
 * STANDING ──(after a while)──▶ LYING_DOWN ──▶ RESTING ──(after a while)──▶ SLEEPING or GETTING_UP
 *                                              RESTING, SLEEPING ──(after a while)──▶ GETTING_UP ──▶ STANDING
 * STANDING, RESTING, SLEEPING ──(stroked)──▶ BEING_STROKED ──▶ STROKED ──▶ STANDING
 * at night: STANDING ──(not in bed: BedtimeTrip)──▶ GOING_TO_BED ──(there, or gave up)──▶ LYING_DOWN
 *           STANDING ──(in bed)──▶ LYING_DOWN ──▶ RESTING ──▶ SLEEPING_THE_NIGHT ──(day)──▶ GETTING_UP
 *           RESTING, SLEEPING ──(in bed)──▶ SLEEPING_THE_NIGHT, (not in bed)──▶ GETTING_UP ──▶ STANDING
 *           the rest ends in STANDING as by day; no stroke all night
 * by day, in the barn for the night: STANDING ──▶ LEAVING_THE_BARN ──(out of the door, or gave up)──▶ STANDING
 *     out of the door: STANDING ──▶ LEAVING_THE_BARN ──(2..4 tiles from the door, or gave up)──▶ STANDING
 * </pre>
 * Another creature gets its own enum like this one (and a class like {@link Cow}).
 */
public enum CowState implements State<Cow> {
    /**
     * On its feet, chewing. At night it goes to bed (into the barn, {@link BedtimeTrip}) or, in bed, lies down to
     * sleep. In the morning, in the barn for the night, it goes out. Otherwise after a while it walks a step, grazes or
     * lies down (as often each).
     */
    STANDING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.IDLE);
            cow.stayFor(3, 6); // a calm animal: it stands a good while
        }

        @Override
        public void update(Cow cow) {
            BedtimeTrip trip = cow.getTrip();
            if (cow.isNight()) {
                if (trip.knowsTheWay()) { // its first frame on a map it waits to be told the way
                    cow.changeState(trip.isInBed() ? LYING_DOWN : GOING_TO_BED);
                }
            } else if (trip.isAway() && trip.knowsTheWay()) {
                cow.changeState(LEAVING_THE_BARN);
            } else if (cow.isTimeUp()) {
                CowState next = oneOf(WALKING, LYING_DOWN, GRAZING);
                if (next == GRAZING) {
                    cow.startGrazing();
                }
                cow.changeState(next);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }

        @Override
        boolean canBeFed() {
            return true;
        }
    },

    /** One step onto a free tile next to it. With no free tile it stays where it is. */
    WALKING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.WALK);
            cow.walkToFreeNeighbour(Cow.SECONDS_PER_TILE);
        }

        @Override
        public void update(Cow cow) {
            if (!cow.isWalking()) {
                cow.changeState(STANDING);
            }
        }
    },

    LYING_DOWN {
        @Override
        public void enter(Cow cow) {
            cow.playOnce(Action.SETTLE, 0, 3); // the row's first half: down
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(RESTING);
            }
        }
    },

    /** Lying awake. At night it falls asleep at once. After a while it falls asleep or gets up. */
    RESTING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.REST);
            cow.stayFor(4, 8);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isNight()) {
                cow.changeState(cow.getTrip().isInBed() ? SLEEPING_THE_NIGHT : GETTING_UP);
            } else if (cow.isTimeUp()) {
                cow.changeState(oneOf(SLEEPING, GETTING_UP));
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }

        @Override
        boolean canBeFed() {
            return true;
        }
    },

    /** A nap: asleep until it has slept enough. Night falling, it sleeps on, the night through. */
    SLEEPING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.SLEEP);
            cow.stayFor(6, 12);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isNight()) {
                cow.changeState(cow.getTrip().isInBed() ? SLEEPING_THE_NIGHT : GETTING_UP);
            } else if (cow.isTimeUp()) {
                cow.changeState(GETTING_UP);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }

        @Override
        boolean canBeFed() {
            return true;
        }
    },

    /**
     * Asleep for the night ("zzz" over it, and now and then a dream about its favourite thing): nothing wakes it, not
     * the player (nothing to give, no stroke). Up when the day starts.
     */
    SLEEPING_THE_NIGHT {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.SLEEP);
            cow.startSleepingTheNight();
        }

        @Override
        public void update(Cow cow) {
            if (!cow.isNight()) {
                cow.getTrip().morning();
                cow.changeState(GETTING_UP);
            }
        }

        @Override
        public void exit(Cow cow) {
            cow.stopSleepingTheNight();
        }
    },

    /** At night, not in bed: it walks to the barn's door, through, and to a place 2..4 tiles from the door inside. */
    GOING_TO_BED {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.WALK);
            if (!cow.getTrip().startToBed()) {
                cow.changeState(LYING_DOWN); // nowhere to go: it sleeps here
            }
        }

        @Override
        public void update(Cow cow) {
            if (!cow.isNight()) {
                cow.changeState(STANDING); // morning came first
            } else if (cow.getTrip().walk() != BedtimeTrip.Way.ON_THE_WAY) {
                cow.changeState(LYING_DOWN);
            }
        }
    },

    /** In the morning: out of the barn through its door, then to a place 2..4 tiles from the door outside. */
    LEAVING_THE_BARN {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.WALK);
            if (!cow.getTrip().startOut()) {
                cow.changeState(STANDING);
            }
        }

        @Override
        public void update(Cow cow) {
            if (cow.isNight() || cow.getTrip().walk() != BedtimeTrip.Way.ON_THE_WAY) {
                cow.changeState(STANDING);
            }
        }
    },

    /** Up on its feet; given something while lying, it eats it then. */
    GETTING_UP {
        @Override
        public void enter(Cow cow) {
            cow.playOnce(Action.SETTLE, 3, 6); // the row's second half: up
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(cow.takeWaitingMeal() ? EATING : STANDING);
            }
        }
    },

    /** It eats what it was given. */
    EATING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.EAT);
            cow.stayFor(2, 2);
            cow.eat();
        }

        @Override
        public void update(Cow cow) {
            if (cow.isTimeUp()) {
                cow.changeState(HAPPY);
            }
        }
    },

    /** Now and then: it lowers its head and takes a bite of grass. */
    GRAZING {
        @Override
        public void enter(Cow cow) {
            cow.takeBite();
            cow.playOnce(Action.GRAZE);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(CHEWING);
            }
        }
    },

    /** Chews the grass it bit off; then the next bite, or back on its feet when the bites are taken. */
    CHEWING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.EAT);
            cow.stayFor(1, 1);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isTimeUp() && cow.hasBitesLeft()) {
                cow.changeState(GRAZING);
            } else if (cow.isTimeUp()) {
                cow.changeState(STANDING);
            }
        }
    },

    /** After eating: a heart over its head. */
    HAPPY {
        @Override
        public void enter(Cow cow) {
            cow.playOnce(Action.LOVE);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(STANDING);
            }
        }
    },

    /** Tapped with the hand: it stands still while the hand fades out, so nothing covers its hearts. */
    BEING_STROKED {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.IDLE);
            cow.waitForTheHand();
        }

        @Override
        public void update(Cow cow) {
            if (cow.isTimeUp()) {
                cow.changeState(STROKED);
            }
        }
    },

    /** The player stroked it: a heart over its head. */
    STROKED {
        @Override
        public void enter(Cow cow) {
            cow.playOnce(Action.LOVE);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(STANDING);
            }
        }
    };

    /** In this state the player can stroke the cow (an action button over it shows a hand). */
    boolean canBeStroked() {
        return false;
    }

    /** In this state the player can give the cow something (its favourite, what its quest waits for). */
    boolean canBeFed() {
        return false;
    }

    @Override
    public void update(Cow cow) {
    }

    @Override
    public void exit(Cow cow) {
    }

    /**
     * What every state does about a message unless it says otherwise: a stroke works where the cow can be stroked, a
     * gift where it can be fed (lying, it gets up first).
     */
    @Override
    public boolean onMessage(Cow cow, Telegram telegram) {
        if (telegram.message == Cow.MESSAGE_STROKE && canBeStroked()) {
            cow.changeState(BEING_STROKED);
            return true;
        }
        if (telegram.message == Cow.MESSAGE_GIFT && canBeFed()) {
            if (this == STANDING) {
                cow.changeState(EATING);
            } else {
                cow.eatOnceUp();
                cow.changeState(GETTING_UP);
            }
            return true;
        }
        return false;
    }

    /** One of these, picked at random; name a state twice to make it twice as likely. */
    private static CowState oneOf(CowState... states) {
        return states[MathUtils.random(states.length - 1)];
    }
}
