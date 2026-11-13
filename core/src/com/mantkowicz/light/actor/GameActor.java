package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.resources.SpriteSheet;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.Status.IDLE;
import static com.mantkowicz.light.board.WorldScale.PIXEL_SCALE;
import static com.mantkowicz.light.board.WorldScale.TILE_SIZE;

public abstract class GameActor extends Group {
    private static long ID_COUNTER = 0;

    private final long id;
    private final List<Plugin> pluginsQueue;
    private final GameActorType gameActorType;
    private Status status = IDLE;
    private Image avatar;
    /** The avatar's y when nothing lifts it (hops add to it). */
    private float avatarRestY;

    protected GameActor(GameActorType gameActorType) {
        this.gameActorType = gameActorType;
        this.id = ID_COUNTER++;
        pluginsQueue = new ArrayList<>();
        Debug.outlineIfChosen(this);
    }

    public GameActorType getGameActorType() {
        return gameActorType;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        for (Plugin plugin : pluginsQueue) {
            plugin.run(delta);
        }
    }

    protected void addPlugin(Plugin plugin) {
        pluginsQueue.add(plugin);
    }

    protected void createAvatar(Texture texture) {
        createAvatar(new TextureRegion(texture));
    }

    protected void createAvatar(TextureRegion region) {
        avatar = new Image(region);
        this.addActor(avatar);
        setSize(avatar.getWidth(), avatar.getHeight());
        avatarRestY = 0;
    }

    /**
     * Shows a frame of a pixel-art sprite sheet at the world's pixel scale, on an actor one tile big: what every
     * Sprout Lands character uses. See the full version for other scales or bigger actors.
     */
    protected void createAvatar(TextureRegion frame, SpriteSheet sheet) {
        createAvatar(frame, sheet, PIXEL_SCALE, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Shows a frame of a pixel-art sprite sheet, {@code scale} times bigger. The actor itself is only
     * {@code width x height} (what it occupies on the board, what can be clicked); the picture is centred on it (the
     * sheet's anchor, normally the frame's middle, on the actor's middle) and may stick out, like a character holding a
     * tool.
     */
    protected void createAvatar(TextureRegion frame, SpriteSheet sheet, float scale, float width, float height) {
        avatar = new Image(frame);
        avatar.setSize(frame.getRegionWidth() * scale, frame.getRegionHeight() * scale);
        avatar.setTouchable(Touchable.disabled);
        avatarRestY = height / 2f + (sheet.trimY() - sheet.anchorY()) * scale;
        avatar.setPosition(width / 2f + (sheet.trimX() - sheet.anchorX()) * scale, avatarRestY);
        this.addActor(avatar);
        setSize(width, height);
    }

    /**
     * The actor's picture, or {@code null} if it has none. Moving it only changes how the actor looks, not where it is.
     */
    public Image getAvatar() {
        return avatar;
    }

    /**
     * The avatar's y when nothing lifts it, relative to the actor.
     */
    public float getAvatarRestY() {
        return avatarRestY;
    }

    public Vector2 getCenter() {
        return new Vector2(getX(Align.center), getY(Align.center));
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setPosition(Vector2 position) {
        super.setPosition(position.x, position.y);
    }

    public void moveBy(Vector2 interval) {
        super.moveBy(interval.x, interval.y);
    }
}
