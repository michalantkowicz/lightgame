package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TilePoint;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.plugin.StateLabelPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

import static com.mantkowicz.light.debug.Debug.Tool.STATE_LABELS;

/**
 * A frog (green or pale green), about as big as a chick. It is shy: it can't be stroked or fed, and it keeps more than
 * {@link #KEEP_AWAY} tiles away from the player and the other animals, hopping away from whoever comes nearer (into
 * the water rather than along the land; in the water it is safe). It hops,
 * half a tile at a time; it doesn't block anyone's way (it is never in it for long). It lives by the water and in it:
 * now and then, and always for the night, it jumps into the water with a splash and floats there (only its head and
 * back show), gliding from tile to tile without a hop; by day it jumps out again with a splash after a while. A crop lying on the ground nearby it goes for and eats with its tongue
 * ({@link FrogMeal}), when nobody is near. Who is near and where it can go it sees in {@link FrogSurroundings}; what it
 * does and when is all in {@link FrogState}.
 */
public class Frog extends SpriteActor {
    /** Nobody (the player, an animal) may come this near (in tiles, diagonally too): it hops away. */
    public static final int KEEP_AWAY = 2;
    /** The name of its splashes on the stage (for tests). */
    public static final String SPLASH = "frog splash";
    /** One hop, half a tile: the hop row's frames from the crouch to the landing. */
    static final int FIRST_HOP_FRAME = 1, LAST_HOP_FRAME = 7;
    static final float HOP_SECONDS = 0.49f;
    /** In the water it glides from tile to tile this slowly. */
    static final float SWIM_SECONDS = 1.2f;
    /** Into the water and out of it: one jump over the whole tile, this long. */
    static final float JUMP_SECONDS = 0.6f;
    /** Its splash (the fishing bobber's, 32 pack pixels) is drawn this much of its size. */
    private static final float SPLASH_SIZE = 0.6f;

    private final DefaultStateMachine<Frog, FrogState> stateMachine = new DefaultStateMachine<>(this);
    private final FrogSurroundings around;
    private final FrogMeal meal;
    private final ResourcesService resources;
    /** Seconds left of what {@link #stayFor} set. */
    private float timeLeft;
    private boolean night;
    /** Where it is going, tile by tile (to the water, out of it, to a crop); empty when it isn't going anywhere. */
    private Deque<Tile> way = new ArrayDeque<>();

    public Frog(GameContext context, Sprite sprite) {
        super(GameActorType.FROG, context, sprite, Action.IDLE,
                "A frog. It hops away from anyone who comes near and lives by the water.");
        resources = context.getResourcesService();
        around = new FrogSurroundings(context.getGameStage());
        meal = new FrogMeal(this, context.getGameStage(), around);
        setName("frog");
        setTouchable(Touchable.disabled); // a tap where it sits goes to the map: it is in nobody's way
        stateMachine.changeState(FrogState.SITTING);
        addPlugin(delta -> {
            timeLeft -= delta;
            stateMachine.update();
        });
        if (Debug.isOn(STATE_LABELS)) {
            addPlugin(new StateLabelPlugin(this, context.getResourcesService(), this::getStateName));
        }
    }

    /** The frog's sheet for Tiled's colour property: "pale" the pale green one, "green" the green one, else either. */
    public static Sprite colour(String colour) {
        if ("pale".equalsIgnoreCase(colour)) {
            return Sprite.FROG_PALE;
        }
        if ("green".equalsIgnoreCase(colour)) {
            return Sprite.FROG;
        }
        return MathUtils.randomBoolean() ? Sprite.FROG : Sprite.FROG_PALE;
    }

    /** It sits where it was put: it doesn't block its tile (see {@link #stepTo}). */
    @Override
    public void comeOnto(Tile tile) {
        setTile(tile);
    }

    public FrogState getState() {
        return stateMachine.getCurrentState();
    }

    /** The name of the state it is in, e.g. "SWIMMING" (debug labels, tests). */
    public String getStateName() {
        return getState().name();
    }

    void changeState(FrogState state) {
        stateMachine.changeState(state);
    }

    /** Night or day ({@code BedtimeFeature}): at night it goes into the water and stays there until the morning. */
    public void setNight(boolean night) {
        this.night = night;
    }

    boolean isNight() {
        return night;
    }

    /** Going for a crop on the ground and eating it. */
    FrogMeal meal() {
        return meal;
    }

    /** The crop it goes for or eats, or null: for tests. */
    public ItemOnGround getCrop() {
        return meal.getCrop();
    }

    // waiting

    void stayFor(float minSeconds, float maxSeconds) {
        timeLeft = MathUtils.random(minSeconds, maxSeconds);
    }

    boolean isTimeUp() {
        return timeLeft <= 0;
    }

    boolean isMoving() {
        return Status.MOVEMENT.equals(getStatus());
    }

    boolean isInWater() {
        return getTile().isWater();
    }

    // going places

    /**
     * Someone is {@link #KEEP_AWAY} tiles away or nearer, and it is on land: in the water it is safe (nobody follows it
     * there), so once in, it floats still a while however near the bank they stand.
     */
    boolean isThreatened() {
        return !isInWater() && !around.isSafe(getTile());
    }

    /** A step away from whoever is near (see {@link FrogSurroundings#awayFrom}); false when it stays. */
    boolean stepAway() {
        return stepToIfAny(around.awayFrom(getTile()));
    }

    /** A step onto a tile next to it (up, down, left or right) on land where nobody is near; false when it stays. */
    boolean hopAround() {
        return stepToIfAny(around.hopFrom(getTile()));
    }

    /** Swims a tile on, onto water next to it where nobody is near; or stays. */
    void swimAround() {
        stepToIfAny(around.swimFrom(getTile()));
    }

    private boolean stepToIfAny(Tile target) {
        if (target == null) {
            return false;
        }
        stepTo(target);
        return true;
    }

    /** Plans its way to the nearest water (over land and water). False when there is no water it can get to. */
    boolean planWayToWater() {
        return planWay(Tile::isWater);
    }

    /** Plans its way out of the water, onto the nearest land where nobody is near. False when there is none. */
    boolean planWayOut() {
        return planWay(around::isSafeLand);
    }

    /** Plans its way to the nearest tile that is the goal; false (no way) when there is none. */
    boolean planWay(Predicate<Tile> goal) {
        Deque<Tile> found = FrogSurroundings.wayTo(getTile(), goal);
        way = found == null ? new ArrayDeque<>() : found;
        return found != null;
    }

    /** Takes the next step of its way; false when it has arrived (or the next tile can't be gone onto any more). */
    boolean followWay() {
        Tile next = way.poll();
        if (next == null || !FrogSurroundings.canBeOn(next)) {
            way.clear();
            return false;
        }
        stepTo(next);
        return true;
    }

    /**
     * One step onto a tile next to it: on land two hops of half a tile each; into the water one jump over the whole tile
     * and a splash where it lands; out of it a splash where it leaves and one jump; from water to water it glides,
     * floating on (no hop). It doesn't block the tile (it is on it for the others to see, e.g. not to throw anything
     * onto it). Facing right as drawn, it is mirrored going left; going up or down it keeps facing the way it did.
     */
    void stepTo(Tile target) {
        Tile from = getTile();
        enterTile(target);
        float dx = target.getCenter().x - from.getCenter().x;
        if (Math.abs(dx) > 1) {
            setMirrored(dx < 0);
        }
        setStatus(Status.MOVEMENT);
        Vector2 end = target.calculatePositionForActorAt(this, TilePoint.CENTER);
        if (from.isWater() && target.isWater()) {
            floatOn();
            addAction(Actions.sequence(Actions.moveTo(end.x, end.y, SWIM_SECONDS), Actions.run(this::arrive)));
            return;
        }
        if (from.isWater() || target.isWater()) { // in or out: one jump, a splash on the water's side
            if (from.isWater()) {
                splash(from);
            }
            addAction(Actions.sequence(Actions.run(this::hop), Actions.moveTo(end.x, end.y, JUMP_SECONDS),
                    Actions.run(() -> {
                        if (target.isWater()) {
                            splash(target);
                        }
                        arrive();
                    })));
            return;
        }
        float halfX = (end.x - getX()) / 2, halfY = (end.y - getY()) / 2;
        addAction(Actions.sequence(
                Actions.run(this::hop), Actions.moveBy(halfX, halfY, HOP_SECONDS),
                Actions.run(this::hop), Actions.moveTo(end.x, end.y, HOP_SECONDS),
                Actions.run(this::arrive)));
    }

    private void hop() {
        playOnce(Action.HOP, FIRST_HOP_FRAME, LAST_HOP_FRAME);
    }

    private void arrive() {
        setStatus(Status.IDLE);
        if (isInWater()) {
            floatOn();
        } else {
            playAnimation(Action.IDLE);
        }
    }

    /** Floats in the water: the swim row, going on as it was if it already floats (moving or not). */
    void floatOn() {
        if (!getCurrentAnimation().endsWith("/" + Action.SWIM.getRow())) {
            playAnimation(Action.SWIM);
        }
    }

    /** Water splashes up over this tile (the fishing bobber's splash), and is gone. */
    private void splash(Tile water) {
        PlayOnceImage splash = new PlayOnceImage(resources.getAnimation(Sprite.FISHING_BOBBER, Action.SPLASH), null);
        float size = 32 * WorldScale.PIXEL_SCALE * SPLASH_SIZE;
        splash.setSize(size, size);
        splash.setPosition(water.getCenter().x - size / 2, water.getCenter().y - size / 2);
        splash.setName(SPLASH);
        getStage().addActor(splash);
    }
}
