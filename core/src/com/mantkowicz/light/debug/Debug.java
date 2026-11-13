package com.mantkowicz.light.debug;

import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * All debug tools in one place.
 * <ul>
 *     <li>Each {@link Tool} is switched on or off on its own: change {@link #enabledTools} below, or call {@link #switchOn(Tool)}.</li>
 *     <li>Actor outlines (scene2d debug lines) can be turned on for chosen actor classes only, e.g. {@code outline(Dog.class)},
 *     or for one actor with {@link #outline(Actor)}.</li>
 * </ul>
 * The defaults make the game behave as it does without debugging.
 */
public final class Debug {
    public enum Tool {
        /** Draws the box the camera keeps the tracked actor in. */
        CAMERA_TRACKING_BOX,
        /** Frames per second, shown next to the camera. */
        FPS_LABEL,
        /** Debug lines of the collect window's table. */
        COLLECT_WINDOW_LINES,
        /** Over every creature with a state machine (the cow): the state it is in, e.g. "STANDING". */
        STATE_LABELS,
        /** Switched-off feature: the player exclaims after standing in the dark for 2 seconds. */
        DARKNESS_EXCLAMATION
    }

    private static final Set<Tool> enabledTools = EnumSet.noneOf(Tool.class);
    private static final Set<Class<?>> outlinedClasses = new HashSet<>();

    private Debug() {
    }

    public static boolean isOn(Tool tool) {
        return enabledTools.contains(tool);
    }

    public static void switchOn(Tool tool) {
        enabledTools.add(tool);
    }

    public static void switchOff(Tool tool) {
        enabledTools.remove(tool);
    }

    /**
     * Outlines every actor of this exact class that is created from now on.
     */
    public static void outline(Class<? extends Actor> actorClass) {
        outlinedClasses.add(actorClass);
    }

    public static void stopOutlining(Class<? extends Actor> actorClass) {
        outlinedClasses.remove(actorClass);
    }

    /**
     * Outlines this one actor.
     */
    public static void outline(Actor actor) {
        actor.setDebug(true);
    }

    /**
     * Called by actors when they are created: outlines the actor if its class was chosen with {@link #outline(Class)}.
     */
    public static void outlineIfChosen(Actor actor) {
        if (outlinedClasses.contains(actor.getClass())) {
            outline(actor);
        }
    }
}
