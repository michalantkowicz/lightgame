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
 * STANDING ──(hungry: picks a crop)──▶ WISHING ──(gets it)──▶ EATING ──▶ HAPPY ──▶ STANDING
 *                        WISHING ──(not fed for long)──▶ GRAZING ──▶ CHEWING ──(3 bites)──▶ STANDING
 * STANDING ──(after a while)──▶ WALKING ──(arrived)──▶ STANDING
 * STANDING ──(after a while)──▶ LYING_DOWN ──▶ RESTING ──(after a while)──▶ SLEEPING or GETTING_UP
 *                                              RESTING, SLEEPING ──(after a while, or hungry)──▶ GETTING_UP ──▶ STANDING
 * STANDING, RESTING, SLEEPING, WISHING ──(stroked)──▶ BEING_STROKED ──▶ STROKED ──▶ STANDING (still hungry: WISHING again)
 * at night: WISHING ──▶ STANDING ──(not in bed: BedtimeTrip)──▶ GOING_TO_BED ──(there, or gave up)──▶ LYING_DOWN
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
     * sleep. In the morning, in the barn for the night, it goes out. Hungry: wishes for one of its favourite crops.
     * Otherwise after a while it walks a step or lies down.
     */
    STANDING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.IDLE);
            cow.stayFor(2, 4);
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
            } else if (cow.isHungry()) {
                cow.chooseWish(); // here, not in WISHING: stroked, it comes back wishing for the same
                cow.changeState(WISHING);
            } else if (cow.isTimeUp()) {
                cow.changeState(oneOf(WALKING, WALKING, WALKING, LYING_DOWN)); // walks 3 times as often as it lies down
            }
        }

        @Override
        boolean canBeStroked() {
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

    /** Lying awake. At night it falls asleep at once. After a while it falls asleep or gets up; hunger gets it up at once. */
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
            } else if (cow.isHungry()) {
                cow.changeState(GETTING_UP);
            } else if (cow.isTimeUp()) {
                cow.changeState(oneOf(SLEEPING, GETTING_UP));
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    /** A nap: asleep until it has slept enough or hunger wakes it. Night falling, it sleeps on, the night through. */
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
            } else if (cow.isTimeUp() || cow.isHungry()) {
                cow.changeState(GETTING_UP);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    /**
     * Asleep for the night ("zzz" over it): nothing wakes it, not hunger, not the player (no stroke). Up when the day
     * starts.
     */
    SLEEPING_THE_NIGHT {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.SLEEP);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isNight()) {
                cow.getZzz().keepOver(cow);
            } else {
                cow.getTrip().morning();
                cow.changeState(GETTING_UP);
            }
        }

        @Override
        public void exit(Cow cow) {
            cow.getZzz().remove();
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

    GETTING_UP {
        @Override
        public void enter(Cow cow) {
            cow.playOnce(Action.SETTLE, 3, 6); // the row's second half: up
        }

        @Override
        public void update(Cow cow) {
            if (cow.isAnimationFinished()) {
                cow.changeState(STANDING);
            }
        }
    },

    /**
     * Stands with a bubble showing what it wishes for, until it gets it (or the night comes: it goes to sleep and
     * wishes again in the morning). It can be stroked meanwhile.
     */
    WISHING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.IDLE);
            cow.showWish();
        }

        @Override
        public void exit(Cow cow) {
            cow.hideWish();
        }

        @Override
        public boolean onMessage(Cow cow, Telegram telegram) {
            if (telegram.message == Cow.MESSAGE_GIFT) {
                cow.changeState(EATING);
                return true;
            }
            return super.onMessage(cow, telegram);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isNight()) {
                cow.changeState(STANDING); // and lies down from there
            } else if (cow.hasWishedTooLong()) { // it eats grass instead
                cow.startGrazing();
                cow.changeState(GRAZING);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    EATING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.EAT);
            cow.stayFor(2, 2);
            cow.stopBeingHungry();
        }

        @Override
        public void update(Cow cow) {
            if (cow.isTimeUp()) {
                cow.changeState(HAPPY);
            }
        }
    },

    /** Nobody fed it: it lowers its head and takes a bite of grass. */
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
                cow.stopBeingHungry(); // it has eaten
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

    @Override
    public void update(Cow cow) {
    }

    @Override
    public void exit(Cow cow) {
    }

    /** What every state does about a message unless it says otherwise: a stroke works where the cow can be stroked. */
    @Override
    public boolean onMessage(Cow cow, Telegram telegram) {
        if (telegram.message == Cow.MESSAGE_STROKE && canBeStroked()) {
            cow.changeState(BEING_STROKED);
            return true;
        }
        return false;
    }

    /** One of these, picked at random; name a state twice to make it twice as likely. */
    private static CowState oneOf(CowState... states) {
        return states[MathUtils.random(states.length - 1)];
    }
}
