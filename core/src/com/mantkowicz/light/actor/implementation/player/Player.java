package com.mantkowicz.light.actor.implementation.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.TimeUtils;
import com.mantkowicz.light.actor.Collecting;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.PlayerCollectResolver;
import com.mantkowicz.light.plugin.implementation.BoardMovementPlugin;
import com.mantkowicz.light.plugin.implementation.CollectPlugin;
import com.mantkowicz.light.plugin.implementation.PlayerNotificationPlugin;
import com.mantkowicz.light.service.event.GameEventService;
import com.mantkowicz.light.service.event.implementation.PlayerMoveEvent;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.PLAYER;
import static com.mantkowicz.light.actor.implementation.player.Status.IDLE;
import static com.mantkowicz.light.actor.implementation.player.Status.MOVEMENT;

public class Player extends GameBoardActor implements Collecting {
    private static final String AVATAR_RESOURCE_NAME = "player.png";
    private static final float SPEED = 0.20f;

    private GameEventService gameEventService;
    private Long lastIdleChange;
    private final Inventory inventory;

    public Player(GameContext context) {
        super(PLAYER, context);

        gameEventService = context.getGameEventService();
        inventory = new Inventory(3, new ArrayList<Item>());

        Texture avatarTexture = context.getResourcesService().getAssetManager().get(AVATAR_RESOURCE_NAME, Texture.class);
        createAvatar(avatarTexture);

        BoardMovementPlugin boardMovementPlugin = new BoardMovementPlugin(this, SPEED, context);
        addPlugin(boardMovementPlugin);

        PlayerNotificationPlugin playerNotificationPlugin = new PlayerNotificationPlugin(this, context);
//        addPlugin(playerNotificationPlugin);

        PlayerCollectResolver collectResolver = new PlayerCollectResolver(this, context);
        CollectPlugin collectPlugin = new CollectPlugin(collectResolver, context);
        addPlugin(collectPlugin);

        setStatus(IDLE);
        addListener(new PlayerClickListener(this, context.getGameEventService()));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        ShaderProgram shader = batch.getShader();
        if (shader != null) {
            batch.setShader(null);
            super.draw(batch, parentAlpha);
            batch.setShader(shader);
        } else {
            super.draw(batch, parentAlpha);
        }
    }

    @Override
    public void setStatus(Status status) {
        if (IDLE.equals(status)) {
            lastIdleChange = TimeUtils.millis();
        } else {
            lastIdleChange = null;
        }
        if (MOVEMENT.equals(status) && IDLE.equals(getStatus())) {
            gameEventService.post(new PlayerMoveEvent(this));
        }
        super.setStatus(status);
    }

    public long getIdleLength() {
        if (lastIdleChange != null) {
            return TimeUtils.millis() - lastIdleChange;
        }
        return 0;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    protected String getDescription() {
        return "player";
    }
}
