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
 * STANDING ──(hungry)──▶ WISHING ──(gets its carrot)──▶ EATING ──▶ HAPPY ──▶ STANDING
 * STANDING ──(after a while)──▶ WALKING ──(arrived)──▶ STANDING
 * STANDING ──(after a while)──▶ LYING_DOWN ──▶ RESTING ──(after a while)──▶ SLEEPING or GETTING_UP
 *                                              RESTING, SLEEPING ──(after a while, or hungry)──▶ GETTING_UP ──▶ STANDING
 * STANDING, RESTING, SLEEPING, WISHING ──(stroked)──▶ STROKED ──▶ STANDING (still hungry: WISHING again)
 * </pre>
 * Another creature gets its own enum like this one (and a class like {@link Cow}).
 */
public enum CowState implements State<Cow> {
    /** On its feet, chewing. Hungry: wishes for a carrot. Otherwise after a while it walks a step or lies down. */
    STANDING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.IDLE);
            cow.stayFor(3, 6);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isHungry()) {
                cow.changeState(WISHING);
            } else if (cow.isTimeUp()) {
                cow.changeState(oneOf(WALKING, WALKING, LYING_DOWN)); // walks twice as often as it lies down
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

    /** Lying awake. After a while it falls asleep or gets up; hunger gets it up at once. */
    RESTING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.REST);
            cow.stayFor(4, 8);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isHungry()) {
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

    /** Asleep until it has slept enough or hunger wakes it. */
    SLEEPING {
        @Override
        public void enter(Cow cow) {
            cow.playAnimation(Action.SLEEP);
            cow.stayFor(6, 12);
        }

        @Override
        public void update(Cow cow) {
            if (cow.isTimeUp() || cow.isHungry()) {
                cow.changeState(GETTING_UP);
            }
        }

        @Override
        boolean canBeStroked() {
            return true;
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

    /** Stands with a bubble showing what it wishes for, until it gets it. It can be stroked meanwhile. */
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
            cow.changeState(STROKED);
            return true;
        }
        return false;
    }

    /** One of these, picked at random; name a state twice to make it twice as likely. */
    private static CowState oneOf(CowState... states) {
        return states[MathUtils.random(states.length - 1)];
    }
}
