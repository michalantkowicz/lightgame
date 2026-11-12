package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.TimeUtils;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.event.PlayerMoveEvent;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.AnimationPlugin;
import com.mantkowicz.light.plugin.BoardMovementPlugin;
import com.mantkowicz.light.plugin.CollectPlugin;
import com.mantkowicz.light.plugin.HopPlugin;
import com.mantkowicz.light.plugin.PlayerNotificationPlugin;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Direction;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.ui.collect.PlayerCollectResolver;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.PLAYER;
import static com.mantkowicz.light.actor.Status.IDLE;
import static com.mantkowicz.light.actor.Status.MOVEMENT;
import static com.mantkowicz.light.debug.Debug.Tool.DARKNESS_EXCLAMATION;

public class Player extends GameBoardActor implements Collecting {
    /** The rabbit's sprite sheet; the player is one tile big, the picture sticks out. */
    private static final Sprite SPRITE = Sprite.RABBIT;
    private static final String DESCRIPTION = "player";
    /** Walking pace: seconds to cross one tile (bigger is slower). */
    private static final float SECONDS_PER_TILE = 0.4f;
    /** How high the rabbit hops (px) and how long one hop takes: two hops per tile. */
    private static final float HOP_HEIGHT = 10f;
    private static final float HOP_DURATION = SECONDS_PER_TILE / 2;
    /** As many as the HUD's backpack slots. */
    private static final int INVENTORY_SIZE = 2;

    private final GameEventService gameEventService;
    private final Inventory inventory;
    private final Stamina stamina = new Stamina();
    private final AnimationPlugin animationPlugin;
    private final PlayerCollectResolver collectResolver;
    private Long lastIdleChange;

    public Player(GameContext context) {
        super(PLAYER, context, DESCRIPTION);
        gameEventService = context.getGameEventService();
        inventory = new Inventory(INVENTORY_SIZE, new ArrayList<Item>());
        lastIdleChange = TimeUtils.millis();
        ResourcesService resourcesService = context.getResourcesService();
        createAvatar(resourcesService.getAnimation(SPRITE, Action.IDLE, Direction.DOWN).getKeyFrame(0),
                resourcesService.getSpriteSheet(SPRITE));

        BoardMovementPlugin boardMovementPlugin = new BoardMovementPlugin(this, context, SECONDS_PER_TILE);
        addPlugin(boardMovementPlugin);
        addPlugin(new HopPlugin(this, HOP_HEIGHT, HOP_DURATION));
        animationPlugin = new AnimationPlugin(this, resourcesService, SPRITE);
        addPlugin(animationPlugin);

        PlayerNotificationPlugin playerNotificationPlugin = new PlayerNotificationPlugin(this, context);
        if (Debug.isOn(DARKNESS_EXCLAMATION)) {
            addPlugin(playerNotificationPlugin);
        }

        collectResolver = new PlayerCollectResolver(this, context);
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

    /**
     * The animation being played, e.g. "rabbit/walk_left".
     */
    public String getCurrentAnimation() {
        return animationPlugin.getCurrentAnimation();
    }

    /** How much strength the player has left: lower it as time passes or for hard work, fill it after sleeping. */
    public Stamina getStamina() {
        return stamina;
    }

    /** Throws the item in a backpack slot out onto a free tile next to the player (nothing happens without one). */
    public void throwOut(int slot) {
        collectResolver.throwOut(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
