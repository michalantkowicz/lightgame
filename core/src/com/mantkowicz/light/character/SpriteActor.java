package com.mantkowicz.light.character;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TilePoint;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.plugin.SpriteAnimationPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;

import java.util.ArrayList;
import java.util.List;

/**
 * A board actor drawn from one Sprout Lands sheet: a picture one tile big (it may stick out) that plays the sheet's
 * animations. A subclass only picks its sprite, description and behaviours (plugins), e.g.
 * <pre>{@code
 * public Cow(GameContext context) {
 *     super(COW, context, Sprite.COW_BROWN, Action.IDLE, "A brown cow.");
 *     playAnimation(Action.IDLE);
 * }
 * }</pre>
 * and is placed in Tiled with the objectClass of its {@link com.mantkowicz.light.map.TileObjectType} line, e.g.
 * {@code COW(Cow::new)}. It blocks its tile.
 */
public abstract class SpriteActor extends GameBoardActor {
    private final Sprite sprite;
    private final ResourcesService resources;
    private final BoardService boardService;
    private final SpriteAnimationPlugin animationPlugin;

    /**
     * @param firstFrame the row (without a direction) whose first frame is shown until an animation is played
     */
    protected SpriteActor(GameActorType type, GameContext context, Sprite sprite, Action firstFrame,
            String description) {
        super(type, context, description);
        this.sprite = sprite;
        this.resources = context.getResourcesService();
        this.boardService = context.getBoardService();
        createAvatar(resources.getAnimation(sprite, firstFrame).getKeyFrame(0), resources.getSpriteSheet(sprite));

        animationPlugin = new SpriteAnimationPlugin(this);
        addPlugin(animationPlugin);
    }

    /**
     * Plays one of the sprite's rows without a direction (named after the action, e.g. "idle") from its start.
     */
    protected void playAnimation(Action action) {
        animationPlugin.play(ResourcesService.animationName(sprite, action, null),
                resources.getAnimation(sprite, action));
    }

    /**
     * Plays the row backwards, from its last frame to its first, and stops there.
     */
    protected void playBackwards(Action action) {
        animationPlugin.playBackwards(ResourcesService.animationName(sprite, action, null),
                resources.getAnimation(sprite, action));
    }

    /**
     * Plays the row once and stops on its last frame, also when the row loops by itself.
     */
    protected void playOnce(Action action) {
        playOnce(action, 0, resources.getAnimation(sprite, action).getKeyFrames().length - 1);
    }

    /**
     * Plays a part of the row once, from frame {@code first} to frame {@code last} (counted from 0), and stops there:
     * for a row that shows two things, like the cow's row that lies down (frames 0 to 3) and gets up again (3 to 6).
     */
    protected void playOnce(Action action, int first, int last) {
        animationPlugin.playOnce(ResourcesService.animationName(sprite, action, null),
                resources.getAnimation(sprite, action), first, last);
    }

    /**
     * The current animation has shown its last frame (a looping one: for the first time).
     */
    public boolean isAnimationFinished() {
        return animationPlugin.isFinished();
    }

    /**
     * Mirrors the picture around the middle of its tile: a sheet drawn facing right looks left.
     */
    protected void setMirrored(boolean mirrored) {
        getAvatar().setOrigin(getWidth() / 2f - getAvatar().getX(), 0);
        getAvatar().setScaleX(mirrored ? -1 : 1);
    }

    public boolean isMirrored() {
        return getAvatar().getScaleX() < 0;
    }

    /**
     * Walks onto a free tile next to it (up, down, left or right), picked at random: a walkable tile with nobody and
     * nothing on it. The picture, drawn facing right, is mirrored while it walks left. The tile it goes to is blocked
     * at once and the one it leaves is freed on arrival; meanwhile its status is MOVEMENT.
     *
     * @return false (nothing happens) when no tile next to it is free
     */
    protected boolean walkToFreeNeighbour(float seconds) {
        Tile from = getTile();
        List<Tile> free = new ArrayList<>();
        for (Tile tile : from.getSideNeighbours()) {
            if (tile.isAccessible() && boardService.getOccupants(tile).isEmpty()) {
                free.add(tile);
            }
        }
        if (free.isEmpty()) {
            return false;
        }
        Tile target = free.get(MathUtils.random(free.size() - 1));
        boardService.setAccessible(target, false);
        enterTile(target);
        setMirrored(target.getCenter().x < from.getCenter().x - 1);
        setStatus(Status.MOVEMENT);
        Vector2 position = target.calculatePositionForActorAt(this, TilePoint.CENTER);
        addAction(Actions.sequence(Actions.moveTo(position.x, position.y, seconds), Actions.run(() -> {
            boardService.setAccessible(from, true);
            setMirrored(false);
            setStatus(Status.IDLE);
        })));
        return true;
    }

    /**
     * Jumps to the end of the current animation (e.g. a chest that starts closed shows its closed frame at once).
     */
    protected void finishAnimation() {
        animationPlugin.finish();
    }

    public Sprite getSprite() {
        return sprite;
    }

    /**
     * The animation being played, e.g. "cow_brown/idle" or "chest/open_front (backwards)".
     */
    public String getCurrentAnimation() {
        return animationPlugin.getCurrentAnimation();
    }
}
