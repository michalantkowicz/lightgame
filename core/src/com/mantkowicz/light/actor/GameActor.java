package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.mantkowicz.light.actor.implementation.player.Status;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.implementation.player.Status.IDLE;

public abstract class GameActor extends Group {
    private static long ID_COUNTER = 0;

    private final long id;
    private final List<Plugin> pluginsQueue;
    private final GameActorType gameActorType;
    private Status status = IDLE;

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
        Image avatar = new Image(region);
        this.addActor(avatar);
        setSize(avatar.getWidth(), avatar.getHeight());
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
