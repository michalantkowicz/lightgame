package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;
import com.mantkowicz.light.resources.Action;

/**
 * Everything a {@link Frog} does, as a gdx-ai state machine (like {@link ChickenState}): each constant is a state, what
 * it does when it gets into it ({@code enter}) and when it goes on to which state ({@code update}, every frame). It
 * can't be stroked or fed: it answers no messages.
 * <pre>
 * SITTING ──(after a moment)──▶ HOPPING ──(arrived)──▶ SITTING
 *         ──(after a moment)──▶ BLINKING, CROAKING ──▶ SITTING
 *         ──(now and then, and at night)──▶ GOING_TO_WATER ──(in the water)──▶ SWIMMING
 *         ──(a crop on the ground nearby)──▶ GOING_FOR_CROP ──(beside it)──▶ EATING ──(3 licks: eaten)──▶ SITTING
 *                                            GOING_FOR_CROP, EATING ──(the crop gone)──▶ SITTING
 * SWIMMING ──(after a while, by day)──▶ COMING_OUT ──(on land)──▶ SITTING; (or it glides on a tile, at night too)
 *   (into the water and out: one jump with a splash; in the water it floats and glides, no hop)
 * any on land ──(someone {@link Frog#KEEP_AWAY} tiles away or nearer)──▶ FLEEING ──(nobody near, or in the water: safe
 *   there)──▶ SITTING, SWIMMING (it floats still a while before it glides on)
 * </pre>
 */
public enum FrogState implements State<Frog> {
    /** On land, looking around. At night (once its moment is over) it goes into the water. */
    SITTING {
        @Override
        public void enter(Frog frog) {
            frog.playAnimation(Action.IDLE);
            frog.stayFor(1.5f, 4);
        }

        @Override
        public void update(Frog frog) {
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (frog.isNight()) {
                if (frog.isTimeUp()) {
                    frog.changeState(GOING_TO_WATER);
                }
            } else if (frog.meal().spot()) {
                frog.changeState(GOING_FOR_CROP);
            } else if (frog.isTimeUp()) {
                frog.changeState(next());
            }
        }

        /** What it does after sitting a while: hops most often, sometimes blinks or croaks, now and then swims. */
        private FrogState next() {
            float dice = MathUtils.random();
            if (dice < 0.4f) {
                return HOPPING;
            }
            if (dice < 0.65f) {
                return BLINKING;
            }
            return dice < 0.9f ? CROAKING : GOING_TO_WATER;
        }
    },

    /** A step (two hops) onto a free tile next to it where nobody is near; with none, it sits on. */
    HOPPING {
        @Override
        public void enter(Frog frog) {
            if (!frog.hopAround()) {
                frog.changeState(SITTING);
            }
        }

        @Override
        public void update(Frog frog) {
            if (!frog.isMoving()) {
                frog.changeState(SITTING);
            }
        }
    },

    BLINKING {
        @Override
        public void enter(Frog frog) {
            frog.playOnce(Action.BLINK);
        }

        @Override
        public void update(Frog frog) {
            sitAfterAnimation(frog);
        }
    },

    /** Its throat puffs up, twice. */
    CROAKING {
        @Override
        public void enter(Frog frog) {
            frog.playOnce(Action.CROAK);
        }

        @Override
        public void update(Frog frog) {
            sitAfterAnimation(frog);
        }
    },

    /** Away from whoever came near, a step at a time (into the water rather than along the land), until nobody is. */
    FLEEING {
        @Override
        public void enter(Frog frog) {
            frog.meal().letGo();
        }

        @Override
        public void update(Frog frog) {
            if (frog.isMoving()) {
                return;
            }
            if (!frog.isThreatened()) {
                frog.changeState(frog.isInWater() ? SWIMMING : SITTING);
            } else {
                frog.stepAway(); // cornered, it stays where it is (and tries again)
            }
        }
    },

    /** Its way to the nearest water, a step at a time. With no water it can get to, it sits on. */
    GOING_TO_WATER {
        @Override
        public void enter(Frog frog) {
            if (!frog.planWayToWater()) {
                frog.changeState(SITTING);
            }
        }

        @Override
        public void update(Frog frog) {
            if (frog.isMoving()) {
                return;
            }
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (frog.isInWater()) {
                frog.changeState(SWIMMING);
            } else if (!frog.followWay() && !frog.planWayToWater()) {
                frog.changeState(SITTING);
            }
        }
    },

    /**
     * Floating in the water, only its head and back showing. After a while it glides a tile on or, by day, comes out
     * onto the nearest land where nobody is near (a splash, a jump). At night it stays in.
     */
    SWIMMING {
        @Override
        public void enter(Frog frog) {
            frog.floatOn();
            frog.stayFor(5, 12);
        }

        @Override
        public void update(Frog frog) {
            if (frog.isMoving()) {
                return;
            }
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (frog.isTimeUp()) {
                if (!frog.isNight() && MathUtils.randomBoolean() && frog.planWayOut()) {
                    frog.changeState(COMING_OUT);
                } else {
                    frog.swimAround();
                    frog.stayFor(3, 8);
                }
            }
        }
    },

    /** Its way out of the water, onto land where nobody is near. */
    COMING_OUT {
        @Override
        public void update(Frog frog) {
            if (frog.isMoving()) {
                return;
            }
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (!frog.followWay()) {
                frog.changeState(frog.isInWater() ? SWIMMING : SITTING);
            }
        }
    },

    /** Its way to stand beside the crop it spotted (left or right of it), a step at a time. */
    GOING_FOR_CROP {
        @Override
        public void update(Frog frog) {
            if (frog.isMoving()) {
                return;
            }
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (frog.meal().isBeside()) {
                frog.changeState(EATING);
            } else if (!frog.meal().isThere() || !frog.followWay()) {
                frog.meal().letGo();
                frog.changeState(SITTING);
            }
        }
    },

    /** Beside the crop, its tongue reaches for it three times (the crop pulses meanwhile); then it is eaten. */
    EATING {
        @Override
        public void enter(Frog frog) {
            frog.meal().start(LICKS);
        }

        @Override
        public void update(Frog frog) {
            if (frog.isThreatened()) {
                frog.changeState(FLEEING);
            } else if (!frog.meal().isThere()) {
                frog.meal().letGo();
                frog.changeState(SITTING);
            } else if (frog.isAnimationFinished()) {
                if (frog.meal().lick()) {
                    frog.meal().eat();
                    frog.changeState(SITTING);
                }
            }
        }
    };

    /** How many times its tongue reaches for a crop before it is eaten. */
    static final int LICKS = 6;

    private static void sitAfterAnimation(Frog frog) {
        if (frog.isThreatened()) {
            frog.changeState(FLEEING);
        } else if (frog.isAnimationFinished()) {
            frog.changeState(SITTING);
        }
    }

    @Override
    public void enter(Frog frog) {
        // most states only look at what happens (update)
    }

    @Override
    public void exit(Frog frog) {
        // nothing to undo
    }

    @Override
    public boolean onMessage(Frog frog, Telegram telegram) {
        return false; // it can't be stroked or fed
    }
}
