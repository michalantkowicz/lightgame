package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.resources.Action;

/**
 * Everything a chicken or a chick does, as a gdx-ai state machine (like {@link CowState}): each constant is a state,
 * saying what it does when it gets into it ({@code enter}), when it goes on to which state ({@code update}, every
 * frame) and how it answers the player ({@code onMessage}). How long it stands and how often it walks rather than
 * pecks come from its {@link ChickenType}.
 * <pre>
 * STANDING ──(hungry: wants corn)──▶ WISHING ──(gets it)──▶ EATING ──▶ HAPPY ──▶ STANDING
 *                        WISHING ──(bored soon)──▶ FORAGING ──(3 pecks)──▶ STANDING
 * STANDING ──(after a moment)──▶ WALKING ──(arrived)──▶ STANDING
 * STANDING ──(after a moment)──▶ PECKING ──▶ STANDING
 * STANDING, WISHING ──(a crop on the ground nearby)──▶ GOING_FOR_CROP ──(5 s)──▶ STEPPING_TO_CROP ──▶ GOING_FOR_CROP ...
 *     GOING_FOR_CROP, STEPPING_TO_CROP ──(next to it)──▶ PECKING_CROP ──(10 s: eaten)──▶ GOING_BACK ──(there)──▶ STANDING
 *     GOING_FOR_CROP, PECKING_CROP ──(the crop gone, or it (or its next step) would block the player)──▶ GOING_BACK
 * STANDING ──(after a moment)──▶ SITTING_DOWN ──▶ RESTING ──(after a while)──▶ SLEEPING or GETTING_UP
 *                                                 RESTING, SLEEPING ──(after a while, or hungry)──▶ GETTING_UP ──▶ STANDING
 * all but WALKING, EATING, FORAGING, STEPPING_TO_CROP, GOING_BACK and the hearts ──(stroked)──▶ BEING_STROKED ──▶ STROKED
 *     ──▶ STANDING (still hungry: WISHING again, the same corn); stroked going for a crop or pecking it: it gives the
 *     crop up for good, and after STROKED goes GOING_BACK
 * at night: WISHING ──▶ STANDING ──(not in bed: CoopTrip)──▶ GOING_TO_BED ──(there, or gave up)──▶ SITTING_DOWN
 *           STANDING ──(in bed)──▶ SITTING_DOWN ──▶ RESTING ──▶ SLEEPING_THE_NIGHT ──(day)──▶ GETTING_UP
 *           RESTING, SLEEPING ──(in bed)──▶ SLEEPING_THE_NIGHT, (not in bed)──▶ GETTING_UP ──▶ STANDING
 *           GOING_FOR_CROP, PECKING_CROP ──▶ GOING_BACK; the rest ends in STANDING as by day; no stroke all night
 * by day, in the coop for the night: STANDING ──▶ LEAVING_THE_COOP ──(out of the door, or gave up)──▶ STANDING
 *     out of the door: STANDING ──▶ LEAVING_THE_COOP ──(away from the door, or gave up)──▶ STANDING
 * </pre>
 */
public enum ChickenState implements State<Chicken> {
    /**
     * On its feet, looking around. At night it goes to bed (to the coop, {@link CoopTrip}) or, in bed, sits down to
     * sleep. In the morning, in the coop for the night, it goes out. Hungry: wants corn. Otherwise after a moment it
     * walks, pecks or sits down.
     */
    STANDING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.IDLE);
            chicken.pause();
        }

        @Override
        public void update(Chicken chicken) {
            CoopTrip trip = chicken.getTrip();
            if (chicken.isNight()) {
                if (trip.knowsTheWay()) { // its first frame on a map it waits to be told the way
                    chicken.changeState(trip.isInBed() ? SITTING_DOWN : GOING_TO_BED);
                }
            } else if (trip.isAway() && trip.knowsTheWay()) {
                chicken.changeState(LEAVING_THE_COOP);
            } else if (chicken.getHunt().spotCrop()) {
                chicken.changeState(GOING_FOR_CROP);
            } else if (chicken.isHungry()) {
                chicken.chooseWish();
                chicken.changeState(WISHING);
            } else if (chicken.isTimeUp()) {
                chicken.changeState(next(chicken.getType()));
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    /**
     * One step onto a free tile next to it. With no free tile, or with the rabbit next to it or coming to it, it
     * pecks where it stands instead.
     */
    WALKING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.WALK);
            if (chicken.isPlayerNear() || !chicken.walkAStep()) {
                chicken.changeState(PECKING);
            }
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.isWalking()) {
                chicken.changeState(STANDING);
            }
        }
    },

    /** It bends down and pecks at the ground, looking for something. */
    PECKING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playOnce(Action.PECK);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isAnimationFinished()) {
                chicken.changeState(STANDING);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    SITTING_DOWN {
        @Override
        public void enter(Chicken chicken) {
            chicken.playOnce(Action.SETTLE);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isAnimationFinished()) {
                chicken.changeState(RESTING);
            }
        }

        @Override
        boolean canBeStroked() {
            return true; // next to the rabbit it never walks: these are the moments a tap would otherwise miss
        }
    },

    /** Sitting awake. At night it falls asleep at once. After a while it falls asleep or gets up; hunger gets it up at once. */
    RESTING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.REST);
            chicken.stayFor(3, 6);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isNight()) {
                chicken.changeState(chicken.getTrip().isInBed() ? SLEEPING_THE_NIGHT : GETTING_UP);
            } else if (chicken.isHungry()) {
                chicken.changeState(GETTING_UP);
            } else if (chicken.isTimeUp()) {
                chicken.changeState(MathUtils.randomBoolean() ? SLEEPING : GETTING_UP);
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
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.SLEEP);
            chicken.stayFor(4, 8);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isNight()) {
                chicken.changeState(chicken.getTrip().isInBed() ? SLEEPING_THE_NIGHT : GETTING_UP);
            } else if (chicken.isTimeUp() || chicken.isHungry()) {
                chicken.changeState(GETTING_UP);
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
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.SLEEP);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isNight()) {
                chicken.getZzz().keepOver(chicken);
            } else {
                chicken.getTrip().morning();
                chicken.changeState(GETTING_UP);
            }
        }

        @Override
        public void exit(Chicken chicken) {
            chicken.getZzz().remove();
        }
    },

    GETTING_UP {
        @Override
        public void enter(Chicken chicken) {
            chicken.playOnce(Action.RISE);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isAnimationFinished()) {
                chicken.changeState(STANDING);
            }
        }

        @Override
        boolean canBeStroked() {
            return true; // next to the rabbit it never walks: these are the moments a tap would otherwise miss
        }
    },

    /**
     * At night, on its way to bed: to the door of the coop (it goes through), or in the coop to a free place away from
     * the entrance ({@link CoopTrip}). There, or with no way, it sits down to sleep.
     */
    GOING_TO_BED {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.WALK);
            if (!chicken.getTrip().startToBed()) {
                chicken.changeState(SITTING_DOWN); // nowhere to go: it sleeps here
            }
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.isNight()) {
                chicken.changeState(STANDING); // morning came first
            } else if (chicken.getTrip().walk() != CoopTrip.Way.ON_THE_WAY) {
                chicken.changeState(SITTING_DOWN);
            }
        }
    },

    /**
     * In the morning, in the coop for the night: back to the door, and out; out of it, to a free place at least two
     * tiles from the door ({@link CoopTrip}).
     */
    LEAVING_THE_COOP {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.WALK);
            if (!chicken.getTrip().startOut()) {
                chicken.changeState(STANDING);
            }
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isNight() || chicken.getTrip().walk() != CoopTrip.Way.ON_THE_WAY) {
                chicken.changeState(STANDING);
            }
        }
    },

    /**
     * Stands with a bubble showing corn, until it gets it or gets bored (or the night comes: it goes to sleep and
     * wishes again in the morning). It can be stroked meanwhile.
     */
    WISHING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.IDLE);
            chicken.showWish();
        }

        @Override
        public void exit(Chicken chicken) {
            chicken.hideWish();
        }

        @Override
        public boolean onMessage(Chicken chicken, Telegram telegram) {
            if (telegram.message == FarmAnimal.MESSAGE_GIFT) {
                chicken.changeState(EATING);
                return true;
            }
            return super.onMessage(chicken, telegram);
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isNight()) {
                chicken.changeState(STANDING); // and sits down from there
            } else if (chicken.getHunt().spotCrop()) { // a crop on the ground will do too
                chicken.changeState(GOING_FOR_CROP);
            } else if (chicken.hasWishedTooLong()) { // bored: it looks for something on the ground instead
                chicken.startFeedingItself(Chicken.PECKS_INSTEAD);
                chicken.changeState(FORAGING);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
        }
    },

    /** It got its corn: pecks at it for a moment. */
    EATING {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.EAT);
            chicken.stayFor(2, 2);
            chicken.stopBeingHungry();
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isTimeUp()) {
                chicken.changeState(HAPPY);
            }
        }
    },

    /** Nobody fed it: it pecks at the ground, a few times, and finds enough. */
    FORAGING {
        @Override
        public void enter(Chicken chicken) {
            chicken.takeBite();
            chicken.playOnce(Action.PECK);
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.isAnimationFinished()) {
                return;
            }
            if (chicken.hasBitesLeft()) {
                chicken.changeState(FORAGING); // enter again: the next peck
            } else {
                chicken.stopBeingHungry();
                chicken.changeState(STANDING);
            }
        }
    },

    /** A crop lies on the ground nearby: it stands looking at it a while ({@link CropHunt#SECONDS_PER_STEP}), then steps closer. */
    GOING_FOR_CROP {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.IDLE);
            chicken.stayExactly(CropHunt.SECONDS_PER_STEP);
        }

        @Override
        public void update(Chicken chicken) {
            CropHunt hunt = chicken.getHunt();
            if (!hunt.isCropThere() || hunt.blocksThePlayerHere() || chicken.isNight()) {
                chicken.changeState(GOING_BACK); // picked up meanwhile, in the player's way, or bedtime
            } else if (!chicken.isTimeUp()) {
                return; // it looks a while first, also when it is beside the crop already
            } else if (hunt.isNextToCrop()) {
                chicken.changeState(PECKING_CROP);
            } else {
                chicken.changeState(hunt.stepTowardsCrop() ? STEPPING_TO_CROP : GOING_BACK);
            }
        }

        @Override
        boolean canBeStroked() {
            return true; // and then it gives the crop up (onMessage)
        }
    },

    /** One step closer to the crop; next to it, it starts pecking at once. */
    STEPPING_TO_CROP {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.WALK);
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.isWalking()) {
                chicken.changeState(chicken.getHunt().isNextToCrop() ? PECKING_CROP : GOING_FOR_CROP);
            }
        }
    },

    /** Next to the crop, turned to it, it pecks at it (the crop pulses) until it has eaten it, or the crop is gone. */
    PECKING_CROP {
        @Override
        public void enter(Chicken chicken) {
            chicken.getHunt().faceCrop();
            chicken.playAnimation(Action.PECK);
            chicken.getHunt().setPecking(true);
            chicken.stayExactly(CropHunt.SECONDS_TO_EAT);
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.getHunt().isCropThere() || chicken.getHunt().blocksThePlayerHere() || chicken.isNight()) {
                chicken.changeState(GOING_BACK); // picked up from under its beak, in the player's way, or bedtime
            } else if (chicken.isTimeUp()) {
                chicken.getHunt().eatCrop();
                chicken.stopBeingHungry();
                chicken.changeState(GOING_BACK);
            }
        }

        @Override
        public void exit(Chicken chicken) {
            chicken.getHunt().setPecking(false);
            chicken.standStraight();
        }

        @Override
        boolean canBeStroked() {
            return true; // and then it gives the crop up (onMessage)
        }
    },

    /** Back where it started, or as near as it can get, at its usual pace; then it goes on as before. */
    GOING_BACK {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.WALK);
            chicken.getHunt().startGoingBack();
            if (!chicken.getHunt().stepBack()) {
                chicken.changeState(STANDING);
            }
        }

        @Override
        public void update(Chicken chicken) {
            if (!chicken.isWalking() && !chicken.getHunt().stepBack()) {
                chicken.changeState(STANDING);
            }
        }
    },

    /** After eating: hearts over it. */
    HAPPY {
        @Override
        public void enter(Chicken chicken) {
            chicken.playLove();
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isAnimationFinished()) {
                chicken.changeState(STANDING);
            }
        }
    },

    /** Tapped with the hand: it stands still while the hand fades out, so nothing covers its hearts. */
    BEING_STROKED {
        @Override
        public void enter(Chicken chicken) {
            chicken.playAnimation(Action.IDLE);
            chicken.waitForTheHand();
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isTimeUp()) {
                chicken.changeState(STROKED);
            }
        }
    },

    /** The player stroked it: hearts over it. */
    STROKED {
        @Override
        public void enter(Chicken chicken) {
            chicken.playLove();
        }

        @Override
        public void update(Chicken chicken) {
            if (chicken.isAnimationFinished()) {
                // stroked while it went for a crop: it gave the crop up and goes back where it started
                chicken.changeState(chicken.getHunt().isGoingBackAfterTheStroke() ? GOING_BACK : STANDING);
            }
        }
    };

    /** In this state the player can stroke it (an action button over it shows a hand). */
    boolean canBeStroked() {
        return false;
    }

    @Override
    public void update(Chicken chicken) {
    }

    @Override
    public void exit(Chicken chicken) {
    }

    /** What every state does about a message unless it says otherwise: a stroke works where it can be stroked. */
    @Override
    public boolean onMessage(Chicken chicken, Telegram telegram) {
        if (telegram.message == FarmAnimal.MESSAGE_STROKE && canBeStroked()) {
            chicken.getHunt().giveUpForAStroke(); // going for a crop, it gives it up
            chicken.changeState(BEING_STROKED);
            return true;
        }
        return false;
    }

    /** What it does after standing: walks, pecks or sits down, as often as its type says (sitting down: weight 1). */
    private static ChickenState next(ChickenType type) {
        int roll = MathUtils.random(type.getWalkWeight() + type.getPeckWeight());
        if (roll < type.getWalkWeight()) {
            return WALKING;
        }
        return roll < type.getWalkWeight() + type.getPeckWeight() ? PECKING : SITTING_DOWN;
    }
}
