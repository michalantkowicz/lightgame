package com.mantkowicz.light.character;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.SpriteSheet;

import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.CHICKEN;

/**
 * A chicken or a chick ({@link ChickenType}). Like a {@link Cow} it gets hungry, wishes for a crop, eats what it is
 * given, enjoys a stroke and rests, but it is restless: it walks more, pecks at the ground often (looking for
 * something), and it wishes only for corn and gives up soon, bored. What it does and when is all in
 * {@link ChickenState}.
 */
public class Chicken extends FarmAnimal {
    /** This long after the start or its last meal it gets hungry again. */
    static final float HUNGRY_AFTER_SECONDS = 20;
    /** Wishing this long without getting its corn, it is bored: it pecks at the ground instead, this many times. */
    static final float GIVE_UP_WISHING_AFTER_SECONDS = 15;
    static final int PECKS_INSTEAD = 3;

    private final ChickenType type;
    private final DefaultStateMachine<Chicken, ChickenState> stateMachine = new DefaultStateMachine<>(this);

    public Chicken(GameContext context, ChickenType type) {
        super(CHICKEN, context, type.getSprite(), type.getDescription(), List.of(CropType.CORN), HUNGRY_AFTER_SECONDS,
                GIVE_UP_WISHING_AFTER_SECONDS);
        this.type = type;
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
