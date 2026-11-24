package com.mantkowicz.light.quest;

/** One step of a quest's {@link Quest#run()}: a dialog, waiting for something, giving something. */
public abstract class QuestStep {
    private QuestStep() {
    }

    /** The animal says something. Not shown yet: dialogs come later; for now it is passed by. */
    public static final class Dialog extends QuestStep {
        private final String text;

        Dialog(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /** It waits until the player gives it something of this kind. */
    public static final class WaitFor extends QuestStep {
        private final Thing thing;

        WaitFor(Thing thing) {
            this.thing = thing;
        }

        public Thing getThing() {
            return thing;
        }
    }

    /** It gives the player something of this kind (it lands next to it). */
    public static final class Give extends QuestStep {
        private final Thing thing;

        Give(Thing thing) {
            this.thing = thing;
        }

        public Thing getThing() {
            return thing;
        }
    }
}
