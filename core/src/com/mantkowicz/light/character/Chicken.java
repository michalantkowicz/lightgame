package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TilePoint;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.SpriteSheet;

import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.CHICKEN;

/**
 * A chicken or a chick ({@link ChickenType}). Like a {@link Cow} it gets hungry, wishes for a crop, eats what it is
 * given, enjoys a stroke and rests, but it is restless: it walks more, pecks at the ground often (looking for
 * something), and it wishes only for corn and gives up soon, bored. A crop lying on the ground nearby it goes for,
 * slowly, and eats ({@link CropHunt}), unless the player picks it up first. What it does and when is all in
 * {@link ChickenState}.
 */
public class Chicken extends FarmAnimal {
    /** This long after the start or its last meal it gets hungry again. */
    static final float HUNGRY_AFTER_SECONDS = 20;
    /** Wishing this long without getting its corn, it is bored: it pecks at the ground instead, this many times. */
    static final float GIVE_UP_WISHING_AFTER_SECONDS = 15;
    static final int PECKS_INSTEAD = 3;
    /** How long leaning towards a crop, and back, takes. */
    private static final float LEAN_SECONDS = 0.2f;

    private final ChickenType type;
    private final DefaultStateMachine<Chicken, ChickenState> stateMachine = new DefaultStateMachine<>(this);
    /** Going for a crop lying on the ground, to eat it. */
    private final CropHunt hunt;
    /** Leaning towards a crop it pecks at, or null. */
    private MoveByAction lean;
    /** Going back to the middle of its tile after leaning, or null. */
    private MoveToAction straighten;

    public Chicken(GameContext context, ChickenType type) {
        super(CHICKEN, context, type.getSprite(), type.getDescription(), List.of(CropType.CORN), HUNGRY_AFTER_SECONDS,
                GIVE_UP_WISHING_AFTER_SECONDS);
        this.type = type;
        this.hunt = new CropHunt(this, context.getBoardService());
        setName(type.getName());
        stateMachine.changeState(ChickenState.STANDING);
        startLiving();
    }

    public ChickenType getType() {
        return type;
    }

    public ChickenState getState() {
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
        return getState() == ChickenState.WISHING;
    }

    @Override
    protected boolean isStrokable() {
        return getState().canBeStroked();
    }

    void changeState(ChickenState state) {
        stateMachine.changeState(state);
    }

    /** Leans sideways (to peck at what is beside it), quickly; {@link #standStraight()} undoes it. */
    void lean(float dx) {
        standStraight();
        lean = Actions.moveBy(dx, 0, LEAN_SECONDS);
        addAction(lean);
    }

    /**
     * Back in the middle of its tile after {@link #lean}, quickly but not in one frame ({@link #LEAN_SECONDS}); a step
     * it takes meanwhile carries it on from where it is.
     */
    void standStraight() {
        if (lean != null) {
            removeAction(lean);
            lean = null;
            Vector2 position = getTile().calculatePositionForActorAt(this, TilePoint.CENTER);
            straighten = Actions.moveTo(position.x, position.y, LEAN_SECONDS);
            addAction(straighten);
        }
    }

    @Override
    protected void walkOnto(Tile target, float seconds, boolean keepFacing) {
        if (straighten != null) {
            removeAction(straighten); // the step goes on from where it is
            straighten = null;
        }
        super.walkOnto(target, seconds, keepFacing);
    }

    /** Going for a crop on the ground: what it goes for, where it started, its steps. */
    CropHunt getHunt() {
        return hunt;
    }

    /** The crop on the ground it goes for or pecks at, or null (for tests and descriptions). */
    public com.mantkowicz.light.actor.ItemOnGround getCropItGoesFor() {
        return hunt.getCrop();
    }

    /** Stands for a while, how long depends on how restless it is. */
    void pause() {
        stayFor(type.getMinPause(), type.getMaxPause());
    }

    /**
     * One step onto a free tile next to it, at its pace, after which it keeps looking the way it went; false when no
     * tile next to it is free.
     */
    boolean walkAStep() {
        return walkToFreeNeighbour(type.getSecondsPerTile(), true);
    }

    /**
     * Is the rabbit right next to it (diagonally too) or on its way to a tile next to it? Then it doesn't walk away:
     * it may be hard to catch, but it never runs from the rabbit coming to it.
     */
    boolean isPlayerNear() {
        Player player = findPlayer();
        Tile tile = getTile();
        return player != null && tile != null && (isNextTo(tile, player.getTile())
                || isNextTo(tile, player.getWalkTarget()));
    }

    private static boolean isNextTo(Tile tile, Tile other) {
        return other != null && (other == tile || tile.getNeighbours().contains(other));
    }

    private Player findPlayer() {
        if (getStage() == null) {
            return null;
        }
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : getStage().getActors()) {
            if (actor instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    /**
     * Its love row (the bird with happy eyes) and over it the hearts: the pack draws them on the row above, so they
     * play as a picture of their own one frame higher.
     */
    void playLove() {
        playOnce(Action.LOVE);
        Image bird = getAvatar();
        SpriteSheet sheet = getResources().getSpriteSheet(getSprite());
        PlayOnceImage hearts = new PlayOnceImage(getResources().getAnimation(getSprite(), Action.HEARTS), null);
        hearts.setBounds(bird.getX(), bird.getY() + sheet.frameHeight() * WorldScale.PIXEL_SCALE, bird.getWidth(),
                bird.getHeight());
        hearts.setOrigin(bird.getOriginX(), bird.getOriginY()); // looking left, the hearts turn with it
        hearts.setScaleX(bird.getScaleX());
        addActor(hearts);
    }

    /** Are hearts over it right now (it is happy or enjoys a stroke)? */
    public boolean showsHearts() {
        for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
            if (child instanceof PlayOnceImage) {
                return true;
            }
        }
        return false;
    }
}
