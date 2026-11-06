package com.mantkowicz.light.actor.implementation.player;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.TimeUtils;
import com.mantkowicz.light.actor.Collecting;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.LookAtMeOnClickListener;
import com.mantkowicz.light.debug.Debug;
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
import static com.mantkowicz.light.debug.Debug.Tool.DARKNESS_EXCLAMATION;

public class Player extends GameBoardActor implements Collecting {
    private static final String AVATAR_NAME = "player.png";
    private static final String DESCRIPTION = "player";
    private static final float SPEED = 0.20f;
    private static final int INVENTORY_SIZE = 3;

    private final GameEventService gameEventService;
    private final Inventory inventory;
    private Long lastIdleChange;

    public Player(GameContext context) {
        super(PLAYER, context, DESCRIPTION);
        gameEventService = context.getGameEventService();
        inventory = new Inventory(INVENTORY_SIZE, new ArrayList<Item>());
        lastIdleChange = TimeUtils.millis();
        createAvatar(context, AVATAR_NAME);

        BoardMovementPlugin boardMovementPlugin = new BoardMovementPlugin(this, context, SPEED);
        addPlugin(boardMovementPlugin);

        PlayerNotificationPlugin playerNotificationPlugin = new PlayerNotificationPlugin(this, context);
        if (Debug.isOn(DARKNESS_EXCLAMATION)) {
            addPlugin(playerNotificationPlugin);
        }

        PlayerCollectResolver collectResolver = new PlayerCollectResolver(this, context);
        CollectPlugin collectPlugin = new CollectPlugin(collectResolver, context);
        addPlugin(collectPlugin);

        addListener(new LookAtMeOnClickListener(this, gameEventService));
        setTouchable(Touchable.enabled);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // The player is drawn with the batch's default shader, not the game stage's custom one.
        ShaderProgram stageShader = batch.getShader();
        batch.setShader(null);
        super.draw(batch, parentAlpha);
        batch.setShader(stageShader);
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
}
